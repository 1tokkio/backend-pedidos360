package cl.duoc.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaConfig {

    // El AdminClient no se conecta al crearse: si Kafka esta caido, el servicio igual arranca.
    @Bean(destroyMethod = "close")
    public AdminClient adminClient(@Value("${spring.kafka.bootstrap-servers}") String servidores) {
        return AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servidores,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000));
    }
}
