package cl.duoc.pedidos360.recomendaciones.service;

import cl.duoc.pedidos360.recomendaciones.dto.Recomendacion;
import cl.duoc.pedidos360.recomendaciones.mensajeria.InteraccionMensaje;
import cl.duoc.pedidos360.recomendaciones.mensajeria.PedidoCreado;
import cl.duoc.pedidos360.recomendaciones.mensajeria.PublicadorInteracciones;
import cl.duoc.pedidos360.recomendaciones.model.CompraConjunta;
import cl.duoc.pedidos360.recomendaciones.model.Interaccion;
import cl.duoc.pedidos360.recomendaciones.model.ProductoVendido;
import cl.duoc.pedidos360.recomendaciones.model.RecomendacionUsuario;
import cl.duoc.pedidos360.recomendaciones.repository.CompraConjuntaRepository;
import cl.duoc.pedidos360.recomendaciones.repository.InteraccionRepository;
import cl.duoc.pedidos360.recomendaciones.repository.ProductoVendidoRepository;
import cl.duoc.pedidos360.recomendaciones.repository.RecomendacionUsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomienda con conteos, sin modelos: los mas vendidos, los que se compran en la misma
 * orden que otro, y los cinco que mas puntaje tienen para cada usuario.
 */
@Service
public class RecomendacionService {

    private final ProductoVendidoRepository vendidos;
    private final CompraConjuntaRepository conjuntas;
    private final InteraccionRepository interacciones;
    private final RecomendacionUsuarioRepository recomendaciones;
    private final PublicadorInteracciones publicador;

    public RecomendacionService(ProductoVendidoRepository vendidos, CompraConjuntaRepository conjuntas,
                                InteraccionRepository interacciones, RecomendacionUsuarioRepository recomendaciones,
                                PublicadorInteracciones publicador) {
        this.vendidos = vendidos;
        this.conjuntas = conjuntas;
        this.interacciones = interacciones;
        this.recomendaciones = recomendaciones;
        this.publicador = publicador;
    }

    /** Suma las unidades vendidas, cuenta cada par de productos que viajo en la misma orden y puntua la compra. */
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

        // Comprar es la interaccion que mas pesa. Va directo a la base: este mensaje ya viene de RabbitMQ.
        if (pedido.usuarioOid() != null) {
            for (PedidoCreado.Item item : pedido.items()) {
                registrarInteraccion(new InteraccionMensaje(
                        pedido.usuarioOid(), item.productoId(), item.nombreProducto(), "COMPRA"));
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

    /** El POST no escribe en la base: manda la interaccion a Kafka y el consumidor la procesa. */
    public void publicarInteraccion(String usuarioOid, Long productoId, String nombreProducto, String accion) {
        publicador.publicar(new InteraccionMensaje(usuarioOid, productoId, nombreProducto, accion));
    }

    /** Guarda la interaccion y recalcula el top 5 de ese usuario. */
    @Transactional
    public void registrarInteraccion(InteraccionMensaje mensaje) {
        interacciones.save(new Interaccion(mensaje.usuarioOid(), mensaje.productoId(),
                mensaje.nombreProducto(), mensaje.accion(), pesoDe(mensaje.accion())));
        calcularTop5(mensaje.usuarioOid());
    }

    public List<Recomendacion> delUsuario(String usuarioOid) {
        return recomendaciones.findByUsuarioOidOrderByPosicionAsc(usuarioOid).stream()
                .map(r -> new Recomendacion(r.getProductoId(), r.getNombreProducto(), r.getPuntaje()))
                .toList();
    }

    private int pesoDe(String accion) {
        return switch (accion) {
            case "COMPRA" -> 3;
            case "CARRITO" -> 2;
            default -> 1;
        };
    }

    /** Suma los pesos de cada producto, se queda con los cinco mejores y reemplaza el top anterior. */
    private void calcularTop5(String usuarioOid) {
        Map<Long, Long> puntajes = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Interaccion interaccion : interacciones.findByUsuarioOid(usuarioOid)) {
            puntajes.merge(interaccion.getProductoId(), (long) interaccion.getPeso(), Long::sum);
            nombres.put(interaccion.getProductoId(), interaccion.getNombreProducto());
        }

        List<Map.Entry<Long, Long>> mejores = puntajes.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .toList();

        recomendaciones.deleteByUsuarioOid(usuarioOid);
        int posicion = 1;
        for (Map.Entry<Long, Long> mejor : mejores) {
            recomendaciones.save(new RecomendacionUsuario(
                    usuarioOid, mejor.getKey(), nombres.get(mejor.getKey()), mejor.getValue(), posicion++));
        }
    }
}
