package cl.duoc.pedidos360.notificaciones.service;

/** Una fila de la tabla de productos del correo. */
public record LineaCorreo(String nombre, int cantidad, int precioUnitario) {

    public int subtotal() {
        return cantidad * precioUnitario;
    }
}
