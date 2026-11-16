package cl.duoc.pedidos360.recomendaciones.controller;

import cl.duoc.pedidos360.recomendaciones.dto.Recomendacion;
import cl.duoc.pedidos360.recomendaciones.dto.SolicitudInteraccion;
import cl.duoc.pedidos360.recomendaciones.service.RecomendacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recomendaciones")
public class RecomendacionController {

    private final RecomendacionService servicio;

    public RecomendacionController(RecomendacionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "ms-recomendaciones", "estado", "operativo");
    }

    /** Sin query: los mas vendidos. Con ?productoId=: los que se compran junto a ese producto. */
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','Cliente')")
    public ResponseEntity<List<Recomendacion>> recomendar(@RequestParam(required = false) Long productoId) {
        return ResponseEntity.ok(servicio.recomendar(productoId));
    }

    /** Los cinco productos con mas puntaje para el usuario del token. */
    @GetMapping("/mis-recomendaciones")
    @PreAuthorize("hasAnyRole('Admin','Cliente')")
    public ResponseEntity<List<Recomendacion>> misRecomendaciones(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok(servicio.delUsuario(identificadorDe(token)));
    }

    /** Registra que el usuario vio un producto o lo agrego al carrito. Responde 202: se procesa por Kafka. */
    @PostMapping("/interacciones")
    @PreAuthorize("hasAnyRole('Admin','Cliente')")
    public ResponseEntity<Map<String, Object>> interaccion(@AuthenticationPrincipal Jwt token,
                                                           @RequestBody SolicitudInteraccion peticion) {
        boolean accionValida = "VISTA".equals(peticion.accion()) || "CARRITO".equals(peticion.accion());
        if (peticion.productoId() == null || peticion.nombreProducto() == null || !accionValida) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("mensaje", "Se necesita productoId, nombreProducto y una accion VISTA o CARRITO");
            return ResponseEntity.badRequest().body(cuerpo);
        }

        servicio.publicarInteraccion(identificadorDe(token), peticion.productoId(),
                peticion.nombreProducto(), peticion.accion());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Interaccion enviada");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cuerpo);
    }

    /** El identificador estable es "oid" en Azure y "sub" en Cognito. */
    private String identificadorDe(Jwt token) {
        String issuer = token.getIssuer() == null ? "" : token.getIssuer().toString();
        return issuer.contains("cognito-idp") ? token.getSubject() : token.getClaimAsString("oid");
    }
}
