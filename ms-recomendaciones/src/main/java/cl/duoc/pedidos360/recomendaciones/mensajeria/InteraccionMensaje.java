package cl.duoc.pedidos360.recomendaciones.mensajeria;

/** Mensaje del topico interacciones.usuario. La accion es VISTA, CARRITO o COMPRA. */
public record InteraccionMensaje(String usuarioOid, Long productoId, String nombreProducto, String accion) {
}
