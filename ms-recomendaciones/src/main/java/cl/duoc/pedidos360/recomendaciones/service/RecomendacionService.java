package cl.duoc.pedidos360.recomendaciones.service;

import cl.duoc.pedidos360.recomendaciones.dto.Recomendacion;
import cl.duoc.pedidos360.recomendaciones.mensajeria.PedidoCreado;
import cl.duoc.pedidos360.recomendaciones.model.CompraConjunta;
import cl.duoc.pedidos360.recomendaciones.model.ProductoVendido;
import cl.duoc.pedidos360.recomendaciones.repository.CompraConjuntaRepository;
import cl.duoc.pedidos360.recomendaciones.repository.ProductoVendidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Recomienda con conteos, sin modelos: los productos mas vendidos y los que se
 * compran en la misma orden que otro.
 */
@Service
public class RecomendacionService {

    private final ProductoVendidoRepository vendidos;
    private final CompraConjuntaRepository conjuntas;

    public RecomendacionService(ProductoVendidoRepository vendidos, CompraConjuntaRepository conjuntas) {
        this.vendidos = vendidos;
        this.conjuntas = conjuntas;
    }

    /** Suma las unidades vendidas y cuenta cada par de productos que viajo en la misma orden. */
    @Transactional
    public void registrar(PedidoCreado pedido) {
        for (PedidoCreado.Item item : pedido.items()) {
            ProductoVendido vendido = vendidos.findByProductoId(item.productoId())
                    .orElseGet(() -> new ProductoVendido(item.productoId(), item.nombreProducto()));
            vendido.sumar(item.cantidad());
            vendidos.save(vendido);
        }

        for (PedidoCreado.Item uno : pedido.items()) {
            for (PedidoCreado.Item otro : pedido.items()) {
                if (uno.productoId().equals(otro.productoId())) {
                    continue;
                }
                CompraConjunta par = conjuntas.findByProductoIdAndRelacionadoId(uno.productoId(), otro.productoId())
                        .orElseGet(() -> new CompraConjunta(uno.productoId(), otro.productoId(), otro.nombreProducto()));
                par.sumar();
                conjuntas.save(par);
            }
        }
    }

    /** Con productoId: los que se compran junto a ese producto. Sin productoId: los mas vendidos. */
    public List<Recomendacion> recomendar(Long productoId) {
        if (productoId == null) {
            return vendidos.findTop4ByOrderByCantidadDesc().stream()
                    .map(v -> new Recomendacion(v.getProductoId(), v.getNombreProducto(), v.getCantidad()))
                    .toList();
        }
        return conjuntas.findTop3ByProductoIdOrderByVecesDesc(productoId).stream()
                .map(c -> new Recomendacion(c.getRelacionadoId(), c.getNombreRelacionado(), c.getVeces()))
                .toList();
    }
}
