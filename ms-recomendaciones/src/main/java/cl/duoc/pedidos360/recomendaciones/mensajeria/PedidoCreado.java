package cl.duoc.pedidos360.recomendaciones.mensajeria;

import java.util.List;

/** Mensaje que publica ms-ordenes. Aca importan el usuario y los productos que viajaron juntos. */
public record PedidoCreado(Long ordenId, String usuarioOid, List<Item> items) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad) {
    }
}
