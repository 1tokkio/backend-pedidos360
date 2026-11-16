package cl.duoc.pedidos360.kafkaadmin.dto;

// Solo se cambia lo que venga informado. Las particiones solo se pueden aumentar, nunca bajar.
public record CambioTopico(Long retencionMs, String cleanupPolicy, Integer particiones) {
}
