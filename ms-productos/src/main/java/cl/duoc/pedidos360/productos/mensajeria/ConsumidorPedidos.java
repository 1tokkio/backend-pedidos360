package cl.duoc.pedidos360.productos.mensajeria;

import cl.duoc.pedidos360.productos.config.RabbitConfig;
import cl.duoc.pedidos360.productos.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorPedidos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorPedidos.class);

    private final ProductoService servicio;

    public ConsumidorPedidos(ProductoService servicio) {
        this.servicio = servicio;
    }

    @RabbitListener(queues = RabbitConfig.COLA)
    public void alCrearseUnPedido(PedidoCreado pedido) {
        try {
            servicio.descontarStock(pedido);
            log.info("Stock descontado para la orden {}", pedido.ordenId());
        } catch (Exception e) {
            // Se registra y se rechaza sin reencolar: el mensaje va a productos.stock.dlq.
            log.error("No se pudo descontar el stock de la orden {}: {}", pedido.ordenId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Fallo el descuento de stock", e);
        }
    }
}
