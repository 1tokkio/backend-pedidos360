package cl.duoc.pedidos360.notificaciones.mensajeria;

import cl.duoc.pedidos360.notificaciones.config.RabbitConfig;
import cl.duoc.pedidos360.notificaciones.service.LineaCorreo;
import cl.duoc.pedidos360.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ConsumidorPedidos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorPedidos.class);

    private final NotificacionService notificaciones;

    public ConsumidorPedidos(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @RabbitListener(queues = RabbitConfig.COLA)
    public void alCrearseUnPedido(PedidoCreado pedido) {
        try {
            List<LineaCorreo> lineas = pedido.items().stream()
                    .map(item -> new LineaCorreo(item.nombreProducto(), item.cantidad(), item.precioUnitario()))
                    .toList();
            LocalDate fecha = pedido.fecha() == null ? null : pedido.fecha().toLocalDate();

            notificaciones.enviar(pedido.correoUsuario(), pedido.ordenId(), pedido.total(), fecha, lineas);
        } catch (Exception e) {
            // Se registra y se rechaza sin reencolar: el mensaje va a notificaciones.correo.dlq.
            log.error("No se pudo notificar la orden {}: {}", pedido.ordenId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Fallo la notificacion", e);
        }
    }
}
