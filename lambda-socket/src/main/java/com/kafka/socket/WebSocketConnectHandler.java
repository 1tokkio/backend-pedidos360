package com.kafka.socket;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2WebSocketResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;

/**
 * Ruta $connect del API Gateway WebSocket: cada navegador que abre el panel en vivo
 * queda registrado en DynamoDB, y la Lambda de streams usa esa tabla para saber a quien avisar.
 */
public class WebSocketConnectHandler
        implements RequestHandler<APIGatewayV2WebSocketEvent, APIGatewayV2WebSocketResponse> {

    private static final String TABLA_CONEXIONES = "ConexionesWebSocket";

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();

    @Override
    public APIGatewayV2WebSocketResponse handleRequest(APIGatewayV2WebSocketEvent event, Context context) {
        APIGatewayV2WebSocketResponse response = new APIGatewayV2WebSocketResponse();
        try {
            Map<String, String> queryParams = event.getQueryStringParameters();
            String clientId = (queryParams != null && queryParams.get("clientId") != null)
                    ? queryParams.get("clientId")
                    : "default-client";
            String connectionId = event.getRequestContext().getConnectionId();
            context.getLogger().log("Cliente conectado: " + connectionId);

            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(TABLA_CONEXIONES)
                    .item(Map.of(
                            "connectionId", AttributeValue.builder().s(connectionId).build(),
                            "clientId", AttributeValue.builder().s(clientId).build()))
                    .build());

            response.setStatusCode(200);
            response.setBody("Conectado exitosamente");
        } catch (Exception e) {
            context.getLogger().log("Error guardando conexion: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody("Error interno del servidor");
        }
        return response;
    }
}
