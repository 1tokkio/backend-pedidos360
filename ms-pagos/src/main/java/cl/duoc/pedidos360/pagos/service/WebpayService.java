package cl.duoc.pedidos360.pagos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Llama al ambiente de integracion de WebPay Plus con RestClient. No usa el SDK de
 * Transbank: son dos llamadas y tres cabeceras, y asi se ve exactamente lo que se envia.
 */
@Service
public class WebpayService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient cliente;
    private final String urlRetorno;

    public WebpayService(@Value("${app.webpay.url-base}") String urlBase,
                         @Value("${app.webpay.codigo-comercio}") String codigoComercio,
                         @Value("${app.webpay.llave}") String llave,
                         @Value("${app.webpay.return-url}") String urlRetorno) {
        this.urlRetorno = urlRetorno;
        this.cliente = RestClient.builder()
                .baseUrl(urlBase + "/rswebpaytransaction/api/webpay/v1.2/transactions")
                .defaultHeader("Tbk-Api-Key-Id", codigoComercio)
                .defaultHeader("Tbk-Api-Key-Secret", llave)
                .build();
    }

    /** Crea la transaccion. La respuesta trae "token" y "url" para redirigir al usuario. */
    public Map<String, Object> crear(String ordenCompra, String sesion, int monto) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("buy_order", ordenCompra);
        cuerpo.put("session_id", sesion.length() > 61 ? sesion.substring(0, 61) : sesion);
        cuerpo.put("amount", monto);
        // La url de retorno sale de la configuracion, no del cliente, para que nadie la cambie.
        cuerpo.put("return_url", urlRetorno);

        return cliente.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(MAPA);
    }

    /** Confirma la transaccion despues de que el usuario pago. Aprobada si status=AUTHORIZED y response_code=0. */
    public Map<String, Object> confirmar(String token) {
        return cliente.put()
                .uri("/{token}", token)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MAPA);
    }
}
