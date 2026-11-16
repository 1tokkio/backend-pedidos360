package cl.duoc.pedidos360.pagos.controller;

import cl.duoc.pedidos360.pagos.dto.RespuestaInicio;
import cl.duoc.pedidos360.pagos.dto.SolicitudConfirmacion;
import cl.duoc.pedidos360.pagos.dto.SolicitudPago;
import cl.duoc.pedidos360.pagos.model.Pago;
import cl.duoc.pedidos360.pagos.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "ms-pagos", "estado", "operativo");
    }

    @PostMapping
    public ResponseEntity<?> pagoDirecto(@AuthenticationPrincipal Jwt token, @RequestBody SolicitudPago peticion) {
        if (!solicitudValida(peticion)) {
            return error(HttpStatus.BAD_REQUEST, "Se necesita ordenId y un monto mayor a cero");
        }

        Pago pago = servicio.pagoDirecto(identificadorDe(token), peticion.ordenId(), peticion.monto());
        if ("FRAUDE".equals(pago.getEstado())) {
            return error(HttpStatus.FORBIDDEN, pago.getMotivo());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @PostMapping("/webpay/iniciar")
    public ResponseEntity<?> iniciarWebpay(@AuthenticationPrincipal Jwt token, @RequestBody SolicitudPago peticion) {
        if (!solicitudValida(peticion)) {
            return error(HttpStatus.BAD_REQUEST, "Se necesita ordenId y un monto mayor a cero");
        }

        RespuestaInicio respuesta = servicio.iniciarWebpay(identificadorDe(token), peticion.ordenId(), peticion.monto());
        return switch (respuesta.estado()) {
            case "FRAUDE" -> error(HttpStatus.FORBIDDEN, respuesta.motivo());
            case "RECHAZADO" -> error(HttpStatus.BAD_GATEWAY, respuesta.motivo());
            default -> ResponseEntity.ok(respuesta);
        };
    }

    @PostMapping("/webpay/confirmar")
    public ResponseEntity<?> confirmarWebpay(@AuthenticationPrincipal Jwt token,
                                             @RequestBody SolicitudConfirmacion peticion) {
        if (peticion.token() == null || peticion.token().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Falta el token de la transaccion");
        }

        return servicio.confirmarWebpay(identificadorDe(token), peticion.token())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "No existe un pago pendiente con ese token"));
    }

    @GetMapping("/mis-pagos")
    public ResponseEntity<List<Pago>> misPagos(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok(servicio.misPagos(identificadorDe(token)));
    }

    /** Todos los pagos, incluidos los marcados como fraude. Solo Admin. */
    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Pago>> todos() {
        return ResponseEntity.ok(servicio.todos());
    }

    private boolean solicitudValida(SolicitudPago peticion) {
        return peticion.ordenId() != null && peticion.monto() != null && peticion.monto() > 0;
    }

    private ResponseEntity<Object> error(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        return ResponseEntity.status(estado).body(cuerpo);
    }

    /** El identificador estable es "oid" en Azure y "sub" en Cognito. */
    private String identificadorDe(Jwt token) {
        String issuer = token.getIssuer() == null ? "" : token.getIssuer().toString();
        return issuer.contains("cognito-idp") ? token.getSubject() : token.getClaimAsString("oid");
    }
}
