# pedidos360-backend

Los nueve microservicios del sistema Pedidos360, construidos con Spring Boot 3.2.5
sobre Java 17. Cada uno es un Resource Server de OAuth2: recibe el access token que
el frontend obtuvo desde Microsoft Entra ID (administradores) o AWS Cognito
(clientes) y lo valida antes de responder.

En AWS, antes de llegar aqui, el API Gateway ya filtro la peticion con un Lambda
authorizer (`Backend-Ecommerce/lambda-validador`) que comprueba que el token sea
valido para alguno de los dos proveedores. Ese filtro no reemplaza la validacion
de cada microservicio: la Lambda solo decide si la peticion entra a la API, cada
servicio vuelve a validar el token por su cuenta y decide el rol con
`@PreAuthorize`. Son dos capas independientes a proposito.

| Servicio           | Puerto | Schema           | Ruta base                 |
|---------------------|--------|------------------|----------------------------|
| ms-usuarios         | 8081   | `usuarios`       | `/api/v1/usuarios`         |
| ms-productos        | 8082   | `productos`      | `/api/v1/productos`        |
| ms-carrito          | 8083   | `carrito`        | `/api/v1/carrito`          |
| ms-ordenes          | 8084   | `ordenes`        | `/api/v1/ordenes`          |
| ms-notificaciones   | 8085   | `notificaciones` | `/api/v1/notificaciones`   |
| ms-auditoria        | 8086   | `auditoria`      | `/api/v1/auditoria`        |
| ms-pagos            | 8087   | `pagos`          | `/api/v1/pagos`            |
| ms-recomendaciones  | 8088   | `recomendaciones`| `/api/v1/recomendaciones`  |
| ms-kafka-admin      | 8089   | (sin base)       | `/api/v1/kafka`            |

## Validacion del token

`SecurityConfig` comprueba cuatro cosas antes de dejar pasar una peticion:

1. **Firma** - contra las claves publicas (JWKS) que publica el tenant.
2. **Emisor** - el claim `iss` tiene que ser el del tenant configurado.
3. **Audiencia** - el claim `aud` tiene que apuntar a esta API.
4. **Vigencia** - el token no puede estar expirado ni ser todavia futuro.

Ademas, los roles se traducen a autoridades de Spring con el prefijo `ROLE_`, lo que
permite proteger endpoints con `@PreAuthorize("hasRole('Admin')")`: el claim `roles`
en Azure, `cognito:groups` en Cognito, y si el token no trae ninguno de los dos se
aplica el `default-role` del proveedor. Un token valido pero sin ese rol recibe
**403**, mientras que una peticion sin token recibe **401**.

## Mensajeria

Se usan dos brokers, cada uno para lo que mejor resuelve:

- **RabbitMQ para tareas ("haz esto")**. Un exchange `topic` llamado `pedidos360`.
  `ms-ordenes` publica `pedido.creado` cuando el pago se aprueba, y lo consumen
  `ms-productos` (`productos.stock`), `ms-notificaciones` (`notificaciones.correo`) y
  `ms-recomendaciones` (`recomendaciones.historial`). Es una tarea que se hace una vez y se
  confirma, asi que no hace falta conservarla. Cada cola tiene su dead-letter queue (`<cola>.dlq`,
  via el exchange `pedidos360.dlx`): si el consumidor falla registra el error y el mensaje queda
  en la DLQ, visible en la consola de RabbitMQ.
- **Kafka para hechos ("esto ocurrio")**. Cuatro topicos con 3 particiones y 3 replicas:
  `pagos.resultado` (publica `ms-pagos`; lo leen `ms-ordenes` y `ms-auditoria` en grupos distintos),
  `auditoria.eventos` (publican `ms-ordenes` y `ms-pagos`; lo lee `ms-auditoria`),
  `pagos.intentos` (publica y lee `ms-pagos` para detectar la rafaga de intentos) e
  `interacciones.usuario` (publica y lee `ms-recomendaciones`). La auditoria
  paso de RabbitMQ a Kafka porque es un registro que se debe poder releer, no una tarea que se
  consume y desaparece.

Los mensajes viajan como JSON. Su estructura esta en los `record` de cada servicio:
`PagoResultado`, `EventoAuditoria` (o `MensajeAuditoria`) y `PedidoCreado`.

## Endpoints

### ms-usuarios
- `GET /api/v1/usuarios/estado` - abierto, sin token.
- `GET /api/v1/usuarios/perfil` - registra al usuario a partir de los claims y devuelve su perfil.
- `GET /api/v1/usuarios` - listado completo, **solo rol Admin**.

### ms-productos
- `GET /api/v1/productos/estado` - abierto, sin token.
- `GET /api/v1/productos` - catalogo con stock.
- `GET /api/v1/productos?id=1` - detalle de un producto (id por query string, no por path).
- `POST /api/v1/productos` - cuerpo `{ "nombre": "...", "descripcion": "...", "precio": 1000, "categoria": "...", "stock": 10 }`, **solo rol Admin**.
- Consume `pedido.creado` y descuenta el stock de cada item.

### ms-carrito
- `GET /api/v1/carrito/estado` - abierto, sin token.
- `GET /api/v1/carrito` - carrito del usuario del token, con total calculado.
- `POST /api/v1/carrito/items` - cuerpo `{ "productoId": 1, "nombreProducto": "...", "precioUnitario": 1000, "cantidad": 2 }`.
- `DELETE /api/v1/carrito/items?id=1` - quita un item propio (id por query string, no por path).
- `DELETE /api/v1/carrito` - vacia el carrito.

### ms-ordenes
- `GET /api/v1/ordenes/estado` - abierto, sin token.
- `POST /api/v1/ordenes` - cuerpo `{ "items": [{ "productoId": 1, "nombreProducto": "...", "precioUnitario": 1000, "cantidad": 1 }] }`.
  La orden queda en estado `PENDIENTE_PAGO` y se deja constancia en el topico `auditoria.eventos`.
- `GET /api/v1/ordenes/mis-ordenes` - ordenes del usuario del token.
- `GET /api/v1/ordenes` - todas las ordenes, **solo rol Admin**.
- Consume `pagos.resultado` (Kafka, grupo `ordenes`). Si el pago se aprobo y el monto coincide
  con el total, la orden pasa a `PAGADA` y recien ahi publica `pedido.creado` en RabbitMQ. Si no,
  pasa a `RECHAZADA`.

### ms-notificaciones
- `GET /api/v1/notificaciones/estado` - abierto, sin token.
- `POST /api/v1/notificaciones/enviar` - cuerpo `{ "destinatario": "..." }`, envia un correo de prueba sin pasar por RabbitMQ.
- `GET /api/v1/notificaciones` - historial de notificaciones enviadas, **solo rol Admin**.
- Consume `pedido.creado`, envia el correo de confirmacion y deja el intento registrado.
  El correo es HTML con tabla de productos, total y boton "Ver mis ordenes", mas una version en texto
  plano. El boton usa `TIENDA_URL`; si esa variable va vacia, el correo sale sin el boton.

### ms-auditoria
- `GET /api/v1/auditoria/estado` - abierto, sin token.
- `GET /api/v1/auditoria` - listado de eventos registrados, **solo rol Admin**.
- Consume `auditoria.eventos` (Kafka, grupo `auditoria`) y lo guarda en PostgreSQL.
- Consume `pagos.resultado` (Kafka, grupo `metricas`) y guarda cada pago en la tabla DynamoDB
  `MetricasPagos`, que dispara la Lambda de streams.

### ms-pagos
- `GET /api/v1/pagos/estado` - abierto, sin token.
- `POST /api/v1/pagos/webpay/iniciar` - cuerpo `{ "ordenId": 1, "monto": 91980 }`. Revisa fraude y, si
  pasa, crea la transaccion en WebPay y devuelve `{ pagoId, estado, url, token }`. Si el detector la
  bloquea responde **403** con el motivo.
- `POST /api/v1/pagos/webpay/confirmar` - cuerpo `{ "token": "..." }`. Confirma con WebPay, guarda el
  pago y publica `pagos.resultado`.
- `POST /api/v1/pagos` - pago directo sin WebPay, para probar desde Postman. Mismas reglas de fraude.
- `GET /api/v1/pagos/mis-pagos` - pagos del usuario del token.
- `GET /api/v1/pagos` - todos los pagos, **solo rol Admin**.
- Publica `pagos.resultado`, `auditoria.eventos` y `pagos.intentos` en Kafka.
- Fraude, dos reglas:
  - Monto mayor a `FRAUDE_MONTO_MAXIMO` (500000): responde **403**.
  - Rafaga: `FRAUDE_INTENTOS_MAXIMOS` (3) intentos del mismo usuario en menos de `FRAUDE_VENTANA_SEGUNDOS`
    (10) lo bloquean `FRAUDE_BLOQUEO_MINUTOS` (30). Cada intento se publica en `pagos.intentos`; el
    consumidor (grupo `fraude`) los cuenta en la tabla `estado_fraude`. Mientras el usuario esta
    bloqueado, `iniciar` responde **429**.

### ms-recomendaciones
- `GET /api/v1/recomendaciones/estado` - abierto, sin token.
- `GET /api/v1/recomendaciones` - los productos mas vendidos.
- `GET /api/v1/recomendaciones?productoId=1` - los que mas se compran en la misma orden que ese producto.
- `GET /api/v1/recomendaciones/mis-recomendaciones` - los cinco productos con mas puntaje para el usuario del token.
- `POST /api/v1/recomendaciones/interacciones` - cuerpo `{ "productoId": 1, "nombreProducto": "...", "accion": "VISTA" }`.
  La accion es `VISTA` (peso 1) o `CARRITO` (peso 2). Responde **202**: la interaccion se publica en
  `interacciones.usuario` y la procesa el consumidor (grupo `recomendaciones`), que la guarda y recalcula el top 5.
- Consume `pedido.creado` (RabbitMQ, cola `recomendaciones.historial`): actualiza los conteos de mas vendidos y de
  compras juntas, y suma cada producto comprado con peso 3 al top 5 del usuario.

### ms-kafka-admin
Todo **solo rol Admin**, salvo `/estado`. El nombre del topico va por query string.
- `GET /api/v1/kafka/estado` - abierto, sin token.
- `GET /api/v1/kafka/topicos` - topicos con particiones, replicas, retencion y politica de limpieza.
- `POST /api/v1/kafka/topicos` - cuerpo `{ "nombre": "...", "particiones": 3, "replicas": 3, "retencionMs": 604800000, "cleanupPolicy": "delete" }`.
- `PUT /api/v1/kafka/topicos?nombre=...` - cuerpo `{ "retencionMs": ..., "cleanupPolicy": "...", "particiones": ... }`.
- `DELETE /api/v1/kafka/topicos?nombre=...` - borra el topico.
- `GET /api/v1/kafka/grupos` - grupos de consumidores con su offset y lag por particion.
- Al arrancar deja creados `pagos.resultado`, `auditoria.eventos`, `pagos.intentos` e `interacciones.usuario` si
  no existen. Reintenta unas veces si el cluster todavia se esta levantando.

### lambda-socket y lambda-streams
No son microservicios: se empaquetan con `mvn package` en `target/function.jar` y se suben a mano a Lambda
(runtime `java21`).
- `lambda-socket`, Handler `com.kafka.socket.WebSocketConnectHandler::handleRequest`: ruta `$connect`
  del API Gateway WebSocket, guarda el `connectionId` en `ConexionesWebSocket`.
- `lambda-streams`, Handler `com.kafka.streams.PagosStreamHandler::handleRequest`: trigger del Stream de
  `MetricasPagos`, envia cada pago a todas las conexiones. Necesita la variable `WEBSOCKET_API_ENDPOINT`.

## Como levantarlo

Primero hay que tener corriendo el stack de `Data-Ecommerce`, que crea la base y
RabbitMQ, y el de Kafka (`docker-compose.kafka.yml`).

    cp .env.example .env
    # completar los valores
    docker compose up -d --build

En local, si ambos repositorios corren en la misma maquina, `DATASOURCE_URL` y
`RABBITMQ_HOST` usan el nombre de los contenedores porque comparten la red
`pedidos360-network`. En AWS, cada repositorio vive en su propia instancia y esos
valores pasan a ser la IP privada de la instancia de datos; `KAFKA_BOOTSTRAP_SERVERS` pasa a ser la IP
privada de la instancia de Kafka, con los tres puertos (9092, 9093 y 9094).

Antes de levantar la version con dead-letter queues hay que borrar de la consola de RabbitMQ las colas
`productos.stock`, `notificaciones.correo` y `auditoria.eventos`: las dos primeras cambian de argumentos
y la tercera pasa a Kafka. Los schemas `pagos` y `recomendaciones` se crean con
`Data-Ecommerce/init/02-schemas-evaluacion-final.sql`.

Para desarrollar un servicio suelto sin Docker, exportar las mismas variables y:

    cd ms-usuarios
    mvn spring-boot:run
