package cl.duoc.pedidos360.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pedidos360";
    public static final String COLA = "notificaciones.correo";
    public static final String CLAVE = "pedido.creado";

    // Los mensajes que fallan van al exchange de mensajes muertos y de ahi a la cola .dlq.
    public static final String EXCHANGE_DLX = "pedidos360.dlx";
    public static final String COLA_DLQ = "notificaciones.correo.dlq";

    @Bean
    public TopicExchange intercambio() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange intercambioDlx() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue colaCorreo() {
        return QueueBuilder.durable(COLA)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(COLA_DLQ)
                .build();
    }

    @Bean
    public Queue colaCorreoDlq() {
        return new Queue(COLA_DLQ, true);
    }

    @Bean
    public Binding enlaceCorreo(Queue colaCorreo, TopicExchange intercambio) {
        return BindingBuilder.bind(colaCorreo).to(intercambio).with(CLAVE);
    }

    @Bean
    public Binding enlaceCorreoDlq(Queue colaCorreoDlq, DirectExchange intercambioDlx) {
        return BindingBuilder.bind(colaCorreoDlq).to(intercambioDlx).with(COLA_DLQ);
    }

    @Bean
    public MessageConverter convertidor() {
        return new Jackson2JsonMessageConverter();
    }
}
