package cl.duoc.pedidos360.pagos.service;

import cl.duoc.pedidos360.pagos.dto.RespuestaInicio;
import cl.duoc.pedidos360.pagos.mensajeria.PublicadorPagos;
import cl.duoc.pedidos360.pagos.model.Pago;
import cl.duoc.pedidos360.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository repositorio;
    private final DetectorFraude detector;
    private final WebpayService webpay;
    private final PublicadorPagos publicador;

    public PagoService(PagoRepository repositorio, DetectorFraude detector,
                       WebpayService webpay, PublicadorPagos publicador) {
        this.repositorio = repositorio;
        this.detector = detector;
        this.webpay = webpay;
        this.publicador = publicador;
    }

    /** Pago sin WebPay: pasa por el mismo detector y publica el mismo evento. Sirve para probar desde Postman. */
    public Pago pagoDirecto(String usuarioOid, Long ordenId, int monto) {
        Pago pago = new Pago(ordenId, usuarioOid, monto, "DIRECTO");
        String sospecha = detector.evaluar(usuarioOid, monto);
        if (sospecha != null) {
            return cerrar(pago, "FRAUDE", sospecha);
        }
        return cerrar(pago, "APROBADO", "Pago directo aprobado");
    }

    /** Primer paso de WebPay: revisa fraude y, si pasa, crea la transaccion y entrega la url de pago. */
    public RespuestaInicio iniciarWebpay(String usuarioOid, Long ordenId, int monto) {
        Pago pago = new Pago(ordenId, usuarioOid, monto, "WEBPAY");

        String sospecha = detector.evaluar(usuarioOid, monto);
        if (sospecha != null) {
            pago = cerrar(pago, "FRAUDE", sospecha);
            return new RespuestaInicio(pago.getId(), "FRAUDE", sospecha, null, null);
        }

        // Se guarda antes de llamar a WebPay para tener el id y armar el buy_order.
        pago = repositorio.save(pago);
        try {
            Map<String, Object> respuesta = webpay.crear("P360-" + pago.getId(), usuarioOid, monto);
            String token = (String) respuesta.get("token");
            pago.setToken(token);
            repositorio.save(pago);
            return new RespuestaInicio(pago.getId(), "PENDIENTE", null, (String) respuesta.get("url"), token);
        } catch (RestClientException e) {
            String motivo = "WebPay no pudo crear la transaccion: " + e.getMessage();
            cerrar(pago, "RECHAZADO", motivo);
            return new RespuestaInicio(pago.getId(), "RECHAZADO", motivo, null, null);
        }
    }

    /**
     * Segundo paso: el usuario volvio de WebPay. Vacio si el token no existe o no es de este usuario.
     * Si el pago ya estaba resuelto (el usuario recargo la pagina) devuelve el resultado guardado.
     */
    public Optional<Pago> confirmarWebpay(String usuarioOid, String token) {
        Optional<Pago> encontrado = repositorio.findByToken(token)
                .filter(pago -> pago.getUsuarioOid().equals(usuarioOid));
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        Pago pago = encontrado.get();
        if (!"PENDIENTE".equals(pago.getEstado())) {
            return Optional.of(pago);
        }

        try {
            Map<String, Object> respuesta = webpay.confirmar(token);
            boolean autorizado = "AUTHORIZED".equals(respuesta.get("status"))
                    && respuesta.get("response_code") instanceof Number codigo
                    && codigo.intValue() == 0;
            if (autorizado) {
                return Optional.of(cerrar(pago, "APROBADO", "Pago autorizado por WebPay"));
            }
            return Optional.of(cerrar(pago, "RECHAZADO",
                    "WebPay rechazo el pago (codigo " + respuesta.get("response_code") + ")"));
        } catch (RestClientException e) {
            // Tambien cae aca cuando el usuario anulo el pago: WebPay no deja confirmar ese token.
            return Optional.of(cerrar(pago, "RECHAZADO", "El pago no se completo en WebPay"));
        }
    }

    public List<Pago> misPagos(String usuarioOid) {
        return repositorio.findByUsuarioOidOrderByFechaDesc(usuarioOid);
    }

    public List<Pago> todos() {
        return repositorio.findAllByOrderByFechaDesc();
    }

    /** Deja el pago en su estado final, lo guarda y avisa por Kafka. */
    private Pago cerrar(Pago pago, String estado, String motivo) {
        pago.resolver(estado, motivo);
        pago = repositorio.save(pago);
        log.info("Pago {} de la orden {} quedo {}: {}", pago.getId(), pago.getOrdenId(), estado, motivo);
        publicador.publicarResultado(pago);
        return pago;
    }
}
