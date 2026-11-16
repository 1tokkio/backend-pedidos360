package cl.duoc.pedidos360.recomendaciones.dto;

/** Cuerpo del POST de interacciones. La accion puede ser VISTA o CARRITO: la compra llega sola desde RabbitMQ. */
public record SolicitudInteraccion(Long productoId, String nombreProducto, String accion) {
}
