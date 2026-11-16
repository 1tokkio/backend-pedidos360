package cl.duoc.pedidos360.recomendaciones.controller;

import cl.duoc.pedidos360.recomendaciones.dto.Recomendacion;
import cl.duoc.pedidos360.recomendaciones.service.RecomendacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
