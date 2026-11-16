package cl.duoc.pedidos360.kafkaadmin.dto;

// Lo que no se informe toma el valor por defecto del proyecto: 3 particiones, 3 replicas, 7 dias y delete.
public record NuevoTopico(String nombre, Integer particiones, Integer replicas, Long retencionMs, String cleanupPolicy) {
}
