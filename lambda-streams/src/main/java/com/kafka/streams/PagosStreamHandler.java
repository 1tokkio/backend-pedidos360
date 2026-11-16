package com.kafka.streams;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.streams.DTO.PagoMetricaDTO;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Se dispara con el Stream de la tabla MetricasPagos. Cada pago nuevo se envia por WebSocket
 * a todos los paneles conectados, asi el Admin lo ve aparecer sin recargar la pagina.
 */
public class PagosStreamHandler implements RequestHandler<DynamodbEvent, Void> {

    private static final String TABLA_CONEXIONES = "ConexionesWebSocket";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    // Viene de la configuracion de la Lambda, no del codigo: cambia si se recrea el API WebSocket.
    private final String webSocketApiEndpoint = System.getenv("WEBSOCKET_API_ENDPOINT");

    @Override
    public Void handleRequest(DynamodbEvent event, Context context) {
        if (event == null || event.getRecords() == null) {
            context.getLogger().log("El evento recibido de DynamoDB Stream es nulo.");
            return null;
        }
        context.getLogger().log("Procesando " + event.getRecords().size() + " eventos desde DynamoDB Stream...");

        List<PagoMetricaDTO> lote = new ArrayList<>();
        for (DynamodbEvent.DynamodbStreamRecord registro : event.getRecords()) {
            if ("INSERT".equals(registro.getEventName()) || "MODIFY".equals(registro.getEventName())) {
                Map<String, AttributeValue> imagen = registro.getDynamodb().getNewImage();
                if (imagen != null && !imagen.isEmpty()) {
                    lote.add(aDto(imagen));
                }
            }
        }

        if (!lote.isEmpty()) {
            enviarATodos(lote, context);
        }
        return null;
    }

    private PagoMetricaDTO aDto(Map<String, AttributeValue> imagen) {
        PagoMetricaDTO dto = new PagoMetricaDTO();
        if (imagen.get("id_pago") != null)
            dto.setIdPago(imagen.get("id_pago").getS());
        if (imagen.get("orden_id") != null)
            dto.setOrdenId(Long.valueOf(imagen.get("orden_id").getN()));
        if (imagen.get("monto") != null)
            dto.setMonto(Long.valueOf(imagen.get("monto").getN()));
        if (imagen.get("estado") != null)
            dto.setEstado(imagen.get("estado").getS());
        if (imagen.get("motivo") != null)
            dto.setMotivo(imagen.get("motivo").getS());
        if (imagen.get("medio") != null)
            dto.setMedio(imagen.get("medio").getS());
        if (imagen.get("timestamp") != null)
            dto.setTimestamp(Long.valueOf(imagen.get("timestamp").getN()));
        return dto;
    }

    private void enviarATodos(List<PagoMetricaDTO> lote, Context context) {
        try {
            if (webSocketApiEndpoint == null || webSocketApiEndpoint.isEmpty()) {
                context.getLogger().log("Error: la variable de entorno WEBSOCKET_API_ENDPOINT no esta configurada.");
                return;
            }
            String json = objectMapper.writeValueAsString(lote);
            context.getLogger().log("Transmitiendo " + lote.size() + " pagos a los paneles conectados...");

            // El SDK agrega /@connections/{id} por su cuenta. Si se pego la "Connection URL" de la
            // consola, que ya termina en /@connections, se quita para no duplicarlo.
            String base = webSocketApiEndpoint.endsWith("/@connections")
                    ? webSocketApiEndpoint.substring(0, webSocketApiEndpoint.length() - "/@connections".length())
                    : webSocketApiEndpoint;

            ApiGatewayManagementApiClient cliente = ApiGatewayManagementApiClient.builder()
                    .endpointOverride(URI.create(base))
                    .region(Region.US_EAST_1)
                    .build();

            for (String connectionId : conexionesActivas(context)) {
                try {
                    cliente.postToConnection(PostToConnectionRequest.builder()
                            .connectionId(connectionId)
                            .data(SdkBytes.fromUtf8String(json))
                            .build());
                } catch (Exception e) {
                    context.getLogger().log("Error enviando a la conexion " + connectionId
                            + " (posiblemente cerrada): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            context.getLogger().log("Error general enviando los pagos: " + e.getMessage());
        }
    }

    private List<String> conexionesActivas(Context context) {
        List<String> conexiones = new ArrayList<>();
        try {
            var respuesta = dynamoDb.scan(ScanRequest.builder().tableName(TABLA_CONEXIONES).build());
            for (Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item : respuesta.items()) {
                if (item.containsKey("connectionId")) {
                    conexiones.add(item.get("connectionId").s());
                }
            }
        } catch (Exception e) {
            context.getLogger().log("Error leyendo la tabla " + TABLA_CONEXIONES + ": " + e.getMessage());
        }
        return conexiones;
    }
}
