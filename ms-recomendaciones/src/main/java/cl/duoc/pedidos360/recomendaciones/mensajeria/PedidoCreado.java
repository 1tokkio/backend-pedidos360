package cl.duoc.pedidos360.recomendaciones.mensajeria;

import java.util.List;

/** Mensaje que publica ms-ordenes. Aca solo importan los productos que viajaron juntos. */
public record PedidoCreado(Long ordenId, List<Item> items) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad) {
    }
}
