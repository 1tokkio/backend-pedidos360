package cl.duoc.pedidos360.kafkaadmin.controller;

import cl.duoc.pedidos360.kafkaadmin.dto.CambioTopico;
import cl.duoc.pedidos360.kafkaadmin.dto.GrupoConsumidor;
import cl.duoc.pedidos360.kafkaadmin.dto.NuevoTopico;
import cl.duoc.pedidos360.kafkaadmin.dto.Topico;
import cl.duoc.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Administrar Kafka es solo del Admin. El nombre del topico va por query string, no por path. */
@RestController
@RequestMapping("/api/v1/kafka")
public class KafkaAdminController {

    private final KafkaAdminService servicio;

    public KafkaAdminController(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("servicio", "ms-kafka-admin", "estado", "operativo");
    }

    @GetMapping("/topicos")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Topico>> listarTopicos() {
        return ResponseEntity.ok(servicio.listarTopicos());
    }

    @PostMapping("/topicos")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> crearTopico(@RequestBody NuevoTopico datos) {
        if (datos.nombre() == null || !datos.nombre().matches("[a-zA-Z0-9._-]+")) {
            return mensaje(HttpStatus.BAD_REQUEST, "El nombre del topico es obligatorio y solo admite letras, numeros, punto, guion y guion bajo");
        }
        servicio.crear(datos);
        return mensaje(HttpStatus.CREATED, "Topico " + datos.nombre() + " creado");
    }

    @PutMapping("/topicos")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> modificarTopico(@RequestParam String nombre, @RequestBody CambioTopico cambio) {
        servicio.modificar(nombre, cambio);
        return mensaje(HttpStatus.OK, "Topico " + nombre + " modificado");
    }

    @DeleteMapping("/topicos")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> borrarTopico(@RequestParam String nombre) {
        servicio.borrar(nombre);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<GrupoConsumidor>> listarGrupos() {
        return ResponseEntity.ok(servicio.listarGrupos());
    }

    @ExceptionHandler(TopicExistsException.class)
    public ResponseEntity<?> topicoExistente(TopicExistsException e) {
        return mensaje(HttpStatus.CONFLICT, "El topico ya existe");
    }

    @ExceptionHandler(UnknownTopicOrPartitionException.class)
    public ResponseEntity<?> topicoInexistente(UnknownTopicOrPartitionException e) {
        return mensaje(HttpStatus.NOT_FOUND, "El topico no existe");
    }

    // Cualquier otro rechazo de Kafka (replicas de mas, configuracion invalida) es un dato mal enviado.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> rechazoDeKafka(ApiException e) {
        return mensaje(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> kafkaNoDisponible(IllegalStateException e) {
        return mensaje(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    private ResponseEntity<Object> mensaje(HttpStatus estado, String texto) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", texto);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
