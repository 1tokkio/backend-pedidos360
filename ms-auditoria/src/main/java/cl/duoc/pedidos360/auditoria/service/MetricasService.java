package cl.duoc.pedidos360.auditoria.service;

import cl.duoc.pedidos360.auditoria.mensajeria.PagoResultado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MetricasService {

    private final DynamoDbClient dynamo;
    private final String tabla;

    public MetricasService(DynamoDbClient dynamo, @Value("${aws.dynamodb.tabla-pagos}") String tabla) {
        this.dynamo = dynamo;
        this.tabla = tabla;
    }

    /**
     * Los nombres de los atributos son los que lee la Lambda de streams. Si falla el SDK la
     * excepcion sube y el manejador de errores del consumidor reintenta.
     */
    public void guardarPago(PagoResultado pago) {
        LocalDateTime fecha = pago.fecha() == null ? LocalDateTime.now() : pago.fecha();

        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("id_pago", texto(String.valueOf(pago.pagoId())));
        item.put("orden_id", numero(pago.ordenId()));
        item.put("monto", numero(pago.monto()));
        item.put("estado", texto(pago.estado()));
        item.put("motivo", texto(pago.motivo() == null ? "" : pago.motivo()));
        item.put("medio", texto(pago.medio() == null ? "" : pago.medio()));
        item.put("timestamp", numero(fecha.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));

        dynamo.putItem(PutItemRequest.builder().tableName(tabla).item(item).build());
    }

    private AttributeValue texto(String valor) {
        return AttributeValue.builder().s(valor).build();
    }

    private AttributeValue numero(Number valor) {
        return AttributeValue.builder().n(String.valueOf(valor)).build();
    }
}
