package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.dto.CambioTopico;
import cl.duoc.pedidos360.kafkaadmin.dto.GrupoConsumidor;
import cl.duoc.pedidos360.kafkaadmin.dto.GrupoConsumidor.LagParticion;
import cl.duoc.pedidos360.kafkaadmin.dto.NuevoTopico;
import cl.duoc.pedidos360.kafkaadmin.dto.Topico;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Administra Kafka con el AdminClient: topicos, su configuracion y los grupos de consumidores. */
@Service
public class KafkaAdminService {

    private static final String RETENCION = "retention.ms";
    private static final String LIMPIEZA = "cleanup.policy";

    private final AdminClient admin;

    public KafkaAdminService(AdminClient admin) {
        this.admin = admin;
    }

    public List<Topico> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics().names());
        if (nombres.isEmpty()) {
            return List.of();
        }

        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        List<ConfigResource> recursos = nombres.stream().map(this::recurso).toList();
        Map<ConfigResource, Config> configuraciones = esperar(admin.describeConfigs(recursos).all());

        List<Topico> topicos = new ArrayList<>();
        for (String nombre : new TreeSet<>(nombres)) {
            TopicDescription descripcion = descripciones.get(nombre);
            Config config = configuraciones.get(recurso(nombre));
            int replicas = descripcion.partitions().isEmpty() ? 0 : descripcion.partitions().get(0).replicas().size();
            topicos.add(new Topico(nombre, descripcion.partitions().size(), replicas,
                    valorDe(config, RETENCION), valorDe(config, LIMPIEZA)));
        }
        return topicos;
    }

    public boolean existe(String nombre) {
        return esperar(admin.listTopics().names()).contains(nombre);
    }

    public void crear(NuevoTopico datos) {
        crear(datos.nombre(),
                datos.particiones() == null ? 3 : datos.particiones(),
                datos.replicas() == null ? 3 : datos.replicas(),
                datos.retencionMs() == null ? 7L * 24 * 60 * 60 * 1000 : datos.retencionMs(),
                datos.cleanupPolicy() == null ? "delete" : datos.cleanupPolicy());
    }

    public void crear(String nombre, int particiones, int replicas, long retencionMs, String cleanupPolicy) {
        NewTopic nuevo = new NewTopic(nombre, particiones, (short) replicas)
                .configs(Map.of(RETENCION, String.valueOf(retencionMs), LIMPIEZA, cleanupPolicy));
        esperar(admin.createTopics(List.of(nuevo)).all());
    }

    /** Cambia solo lo que venga informado. */
    public void modificar(String nombre, CambioTopico cambio) {
        List<AlterConfigOp> operaciones = new ArrayList<>();
        if (cambio.retencionMs() != null) {
            operaciones.add(fijar(RETENCION, String.valueOf(cambio.retencionMs())));
        }
        if (cambio.cleanupPolicy() != null) {
            operaciones.add(fijar(LIMPIEZA, cambio.cleanupPolicy()));
        }
        if (!operaciones.isEmpty()) {
            esperar(admin.incrementalAlterConfigs(Map.of(recurso(nombre), operaciones)).all());
        }
        if (cambio.particiones() != null) {
            esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(cambio.particiones()))).all());
        }
    }

    public void borrar(String nombre) {
        esperar(admin.deleteTopics(List.of(nombre)).all());
    }

    /** Cada grupo con su lag: offset final de la particion menos el offset que el grupo ya confirmo. */
    public List<GrupoConsumidor> listarGrupos() {
        List<String> ids = esperar(admin.listConsumerGroups().all()).stream()
                .map(ConsumerGroupListing::groupId)
                .sorted()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<String, ConsumerGroupDescription> descripciones = esperar(admin.describeConsumerGroups(ids).all());

        List<GrupoConsumidor> grupos = new ArrayList<>();
        for (String id : ids) {
            Map<TopicPartition, OffsetAndMetadata> actuales =
                    esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());

            Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
            for (TopicPartition particion : actuales.keySet()) {
                pedido.put(particion, OffsetSpec.latest());
            }
            Map<TopicPartition, ListOffsetsResultInfo> finales =
                    pedido.isEmpty() ? Map.of() : esperar(admin.listOffsets(pedido).all());

            List<LagParticion> detalle = new ArrayList<>();
            long lagTotal = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entrada : actuales.entrySet()) {
                long actual = entrada.getValue().offset();
                long ultimo = finales.get(entrada.getKey()).offset();
                long lag = Math.max(0, ultimo - actual);
                lagTotal += lag;
                detalle.add(new LagParticion(entrada.getKey().topic(), entrada.getKey().partition(), actual, ultimo, lag));
            }
            detalle.sort(Comparator.comparing(LagParticion::topico).thenComparingInt(LagParticion::particion));

            grupos.add(new GrupoConsumidor(id, descripciones.get(id).state().toString(), lagTotal, detalle));
        }
        return grupos;
    }

    private ConfigResource recurso(String topico) {
        return new ConfigResource(ConfigResource.Type.TOPIC, topico);
    }

    private AlterConfigOp fijar(String clave, String valor) {
        return new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET);
    }

    private String valorDe(Config config, String clave) {
        ConfigEntry entrada = config == null ? null : config.get(clave);
        return entrada == null ? null : entrada.value();
    }

    /**
     * El AdminClient responde con futuros. Aca se espera el resultado; los errores de Kafka
     * (topico existente, topico inexistente) suben tal cual para que el controller les ponga su codigo.
     */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(15, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("Kafka no respondio a tiempo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpio la consulta a Kafka", e);
        }
    }
}
