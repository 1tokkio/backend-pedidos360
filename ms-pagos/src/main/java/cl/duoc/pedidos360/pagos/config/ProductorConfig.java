package cl.duoc.pedidos360.pagos.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProductorConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String servidores) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servidores);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Con tres replicas, acks=all espera a que el mensaje este en las replicas sincronizadas.
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Ajuste de rendimiento: agrupa mensajes en paquetes de 32 KB y espera hasta 20 ms para llenarlos.
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // Si Kafka no responde, falla en 10 segundos en vez de dejar colgada la peticion.
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(@Value("${spring.kafka.bootstrap-servers}") String servidores) {
        return new KafkaTemplate<>(producerFactory(servidores));
    }
}
