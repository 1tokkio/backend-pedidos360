package cl.duoc.pedidos360.recomendaciones.dto;

/** puntaje son las unidades vendidas o las veces que se compro junto al producto consultado. */
public record Recomendacion(Long productoId, String nombreProducto, Long puntaje) {
}
