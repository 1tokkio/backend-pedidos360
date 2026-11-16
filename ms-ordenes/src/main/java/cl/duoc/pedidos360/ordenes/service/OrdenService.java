package cl.duoc.pedidos360.ordenes.service;

import cl.duoc.pedidos360.ordenes.dto.LineaOrden;
import cl.duoc.pedidos360.ordenes.mensajeria.PagoResultado;
import cl.duoc.pedidos360.ordenes.mensajeria.PublicadorAuditoria;
import cl.duoc.pedidos360.ordenes.mensajeria.PublicadorOrdenes;
import cl.duoc.pedidos360.ordenes.model.DetalleOrden;
import cl.duoc.pedidos360.ordenes.model.Orden;
import cl.duoc.pedidos360.ordenes.repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    private final OrdenRepository repositorio;
    private final PublicadorOrdenes publicador;
    private final PublicadorAuditoria auditoria;

    public OrdenService(OrdenRepository repositorio, PublicadorOrdenes publicador, PublicadorAuditoria auditoria) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.auditoria = auditoria;
    }

    /** La orden queda pendiente de pago: stock y correo esperan a que el pago se apruebe. */
    public Orden crear(String usuarioOid, String correo, List<LineaOrden> lineas) {
        Orden orden = new Orden(usuarioOid, correo);
        for (LineaOrden linea : lineas) {
            orden.agregarDetalle(new DetalleOrden(
                    linea.productoId(), linea.nombreProducto(), linea.precioUnitario(), linea.cantidad()));
        }
        orden = repositorio.save(orden);
        auditoria.publicar("ORDEN_CREADA", "orden " + orden.getId() + " por " + orden.getTotal() + ", pendiente de pago");
        return orden;
    }

    /**
     * Aplica a la orden el resultado de un pago que llego por Kafka. Solo actua sobre ordenes
     * pendientes, asi un mensaje repetido no vuelve a descontar stock ni a mandar otro correo.
     * Se publica pedido.creado recien despues de guardar, para no avisar de una orden que no quedo.
     */
    public void aplicarPago(PagoResultado pago) {
        Optional<Orden> encontrada = repositorio.findById(pago.ordenId());
        if (encontrada.isEmpty()) {
            log.warn("Llego el pago {} de una orden que no existe: {}", pago.pagoId(), pago.ordenId());
            return;
        }

        Orden orden = encontrada.get();
        if (!"PENDIENTE_PAGO".equals(orden.getEstado())) {
            log.info("La orden {} ya estaba {}, se ignora el pago {}", orden.getId(), orden.getEstado(), pago.pagoId());
            return;
        }

        String motivoRechazo = motivoDeRechazo(orden, pago);
        if (motivoRechazo == null) {
            orden.setEstado("PAGADA");
            repositorio.save(orden);
            publicador.publicarOrdenCreada(orden, orden.getCorreoUsuario());
            auditoria.publicar("ORDEN_PAGADA", "orden " + orden.getId() + " pagada con " + pago.medio());
            log.info("Orden {} pagada, se publico pedido.creado", orden.getId());
        } else {
            orden.setEstado("RECHAZADA");
            repositorio.save(orden);
            auditoria.publicar("ORDEN_RECHAZADA", "orden " + orden.getId() + ": " + motivoRechazo);
            log.info("Orden {} rechazada: {}", orden.getId(), motivoRechazo);
        }
    }

    /** Null si el pago sirve para esta orden; si no, la razon. */
    private String motivoDeRechazo(Orden orden, PagoResultado pago) {
        if (!"APROBADO".equals(pago.estado())) {
            return pago.motivo() == null ? "pago " + pago.estado() : pago.motivo();
        }
        if (!orden.getUsuarioOid().equals(pago.usuarioOid())) {
            return "el pago es de otro usuario";
        }
        // ms-pagos recibe el monto desde el cliente; aca se comprueba contra el total real de la orden.
        if (!orden.getTotal().equals(pago.monto())) {
            return "el monto pagado (" + pago.monto() + ") no coincide con el total (" + orden.getTotal() + ")";
        }
        return null;
    }

    public List<Orden> misOrdenes(String usuarioOid) {
        return repositorio.findByUsuarioOidOrderByFechaCreacionDesc(usuarioOid);
    }

    public List<Orden> todos() {
        return repositorio.findAll();
    }
}
