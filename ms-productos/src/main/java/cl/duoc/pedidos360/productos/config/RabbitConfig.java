package cl.duoc.pedidos360.productos.config;

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

/**
 * Declara el exchange, la cola, su dead-letter queue y los enlaces. Si ya existen en RabbitMQ
 * no pasa nada, asi el servicio arranca en cualquier orden respecto de los demas.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "pedidos360";
    public static final String COLA = "productos.stock";
    public static final String CLAVE = "pedido.creado";

    // Los mensajes que fallan van al exchange de mensajes muertos y de ahi a la cola .dlq.
    public static final String EXCHANGE_DLX = "pedidos360.dlx";
    public static final String COLA_DLQ = "productos.stock.dlq";

    @Bean
    public TopicExchange intercambio() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange intercambioDlx() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue colaStock() {
        return QueueBuilder.durable(COLA)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(COLA_DLQ)
                .build();
    }

    @Bean
    public Queue colaStockDlq() {
        return new Queue(COLA_DLQ, true);
    }

    @Bean
    public Binding enlaceStock(Queue colaStock, TopicExchange intercambio) {
        return BindingBuilder.bind(colaStock).to(intercambio).with(CLAVE);
    }

    @Bean
    public Binding enlaceStockDlq(Queue colaStockDlq, DirectExchange intercambioDlx) {
        return BindingBuilder.bind(colaStockDlq).to(intercambioDlx).with(COLA_DLQ);
    }

    /** Con este convertidor el mensaje viaja como JSON y se puede leer en la consola de RabbitMQ. */
    @Bean
    public MessageConverter convertidor() {
        return new Jackson2JsonMessageConverter();
    }
}
