package cl.duoc.pedidos360.recomendaciones.mensajeria;

import cl.duoc.pedidos360.recomendaciones.config.RabbitConfig;
import cl.duoc.pedidos360.recomendaciones.service.RecomendacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorPedidos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorPedidos.class);

    private final RecomendacionService servicio;

    public ConsumidorPedidos(RecomendacionService servicio) {
        this.servicio = servicio;
    }

    @RabbitListener(queues = RabbitConfig.COLA)
    public void alCrearseUnPedido(PedidoCreado pedido) {
        try {
            servicio.registrar(pedido);
            log.info("Historial actualizado con la orden {}", pedido.ordenId());
        } catch (Exception e) {
            // Se registra y se rechaza sin reencolar: el mensaje va a recomendaciones.historial.dlq.
            log.error("No se pudo registrar la orden {} en el historial: {}", pedido.ordenId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Fallo el registro del historial", e);
        }
    }
}
