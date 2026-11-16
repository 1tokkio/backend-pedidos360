package cl.duoc.pedidos360.kafkaadmin.dto;

/** Lo que se muestra de un topico: su forma (particiones y replicas) y su configuracion. */
public record Topico(String nombre, int particiones, int replicas, String retencionMs, String cleanupPolicy) {
}
