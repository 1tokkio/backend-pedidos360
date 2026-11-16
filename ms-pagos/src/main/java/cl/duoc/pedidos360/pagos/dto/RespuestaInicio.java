package cl.duoc.pedidos360.pagos.dto;

/** estado es PENDIENTE si hay que redirigir a WebPay (url y token), FRAUDE o RECHAZADO si no. */
public record RespuestaInicio(Long pagoId, String estado, String motivo, String url, String token) {
}
