package cl.duoc.pedidos360.auditoria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Antes de Kafka las filas traian la routing key de Rabbit. La columna se conserva
    // para no tocar las filas existentes: ahora guarda el tipo del evento.
    @Column(name = "routing_key", nullable = false, length = 100)
    private String tipo;

    // Filas viejas, de antes de Kafka, no tienen servicio.
    @Column(length = 60)
    private String servicio;

    @Lob
    @Column(nullable = false)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    public EventoAuditoria() {
    }

    public EventoAuditoria(String servicio, String tipo, String contenido) {
        this.servicio = servicio;
        this.tipo = tipo;
        this.contenido = contenido;
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getServicio() {
        return servicio;
    }

    public String getContenido() {
        return contenido;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
}
