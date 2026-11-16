package cl.duoc.pedidos360.pagos.config;

/**
 * ms-pagos solo publica. Los topicos los crea ms-kafka-admin, aca solo se
 * guardan los nombres para no repetirlos como texto suelto.
 */
public final class KafkaConfig {

    public static final String TOPICO_PAGOS = "pagos.resultado";
    public static final String TOPICO_AUDITORIA = "auditoria.eventos";

    private KafkaConfig() {
    }
}
