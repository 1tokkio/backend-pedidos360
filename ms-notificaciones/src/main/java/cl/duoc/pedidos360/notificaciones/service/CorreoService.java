package cl.duoc.pedidos360.notificaciones.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final Locale CHILE = new Locale("es", "CL");

    // Colores de la tienda. En un correo el CSS va en linea porque muchos clientes ignoran las hojas de estilo.
    private static final String MARINO = "#0f172a";
    private static final String NARANJA = "#f97316";
    private static final String BOTON = "#c2410c";
    private static final String VERDE = "#15803d";
    private static final String GRIS = "#64748b";
    private static final String LINEA = "#e2e8f0";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlTienda;
    private final boolean configurado;

    public CorreoService(JavaMailSender mailSender,
                          @Value("${correo.remitente}") String remitente,
                          @Value("${correo.url-tienda:}") String urlTienda,
                          @Value("${spring.mail.username:}") String usuarioSmtp) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlTienda = urlTienda == null ? "" : urlTienda.trim();
        this.configurado = usuarioSmtp != null && !usuarioSmtp.isBlank();
    }

    /** Mismo asunto que arma el correo, expuesto para que quede registrado en la tabla. */
    public String asuntoPara(Long ordenId) {
        return ordenId == null
                ? "Pedidos360 - Correo de prueba"
                : "Confirmamos tu orden #" + ordenId + " | Pedidos360";
    }

    // Un fallo de envio no relanza excepcion: la orden ya existe, no hay nada que
    // reintentar. Sin SMTP_USER configurado, el correo queda en el log en vez de enviarse.
    public boolean enviarConfirmacion(String destinatario, Long ordenId, Integer total, LocalDate fecha,
                                      List<LineaCorreo> lineas) {
        String asunto = asuntoPara(ordenId);
        String html = cuerpoHtml(ordenId, total, fecha, lineas);
        String texto = cuerpoTexto(ordenId, total, fecha, lineas);

        if (!configurado) {
            log.info("SMTP no configurado, correo no enviado de verdad. Para: {} | Asunto: {} | Cuerpo: {}",
                    destinatario, asunto, texto);
            return true;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            // multipart: el correo lleva el HTML y una version en texto plano para los clientes que no lo muestran.
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(texto, html);

            mailSender.send(mensaje);
            log.info("Correo enviado a {} por la orden {}", destinatario, ordenId);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de la orden {}: {}", ordenId, e.getMessage());
            return false;
        }
    }

    /** HTML armado a mano con tablas: es lo unico que se ve igual en Gmail, Outlook y el celular. */
    private String cuerpoHtml(Long ordenId, Integer total, LocalDate fecha, List<LineaCorreo> lineas) {
        boolean prueba = ordenId == null;
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>")
            .append("<body style=\"margin:0;padding:0;background:#f1f5f9;\">");

        // Texto que aparece como vista previa en la bandeja de entrada.
        html.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">")
            .append(prueba ? "Correo de prueba de Pedidos360." : "Recibimos tu pago. Este es el resumen de tu orden #" + ordenId + ".")
            .append("</div>");

        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#f1f5f9\" style=\"background:#f1f5f9;\">")
            .append("<tr><td align=\"center\" style=\"padding:28px 12px;\">")
            // Ancho flexible con tope de 600 px: en el celular se encoge en vez de cortarse.
            .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#ffffff\" ")
            .append("style=\"width:100%;max-width:600px;background:#ffffff;border-radius:14px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;color:").append(MARINO).append(";\">");

        // Encabezado con la marca.
        html.append("<tr><td bgcolor=\"").append(MARINO).append("\" style=\"background:").append(MARINO).append(";padding:22px 32px;\">")
            .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>")
            .append("<td width=\"36\" height=\"36\" align=\"center\" bgcolor=\"").append(NARANJA).append("\" ")
            .append("style=\"background:").append(NARANJA).append(";border-radius:9px;color:#ffffff;font-size:12px;font-weight:bold;\">360</td>")
            .append("<td style=\"padding-left:12px;color:#ffffff;font-size:21px;\">Pedidos<span style=\"color:").append(NARANJA).append(";font-weight:bold;\">360</span></td>")
            .append("</tr></table></td></tr>");

        if (prueba) {
            html.append("<tr><td style=\"padding:32px;\">")
                .append("<h1 style=\"margin:0 0 10px;font-size:22px;\">Correo de prueba</h1>")
                .append("<p style=\"margin:0;color:").append(GRIS).append(";line-height:1.6;\">Este mensaje confirma que ms-notificaciones puede enviar correos con la configuración SMTP actual.</p>")
                .append("</td></tr>");
        } else {
            // Saludo y estado del pago.
            html.append("<tr><td style=\"padding:32px 32px 8px;\">")
                .append("<span style=\"display:inline-block;background:#dcfce7;color:").append(VERDE).append(";font-size:12px;font-weight:bold;padding:4px 12px;border-radius:99px;\">Pago confirmado</span>")
                .append("<h1 style=\"margin:14px 0 8px;font-size:26px;line-height:1.25;\">Gracias por tu compra</h1>")
                .append("<p style=\"margin:0;color:").append(GRIS).append(";font-size:15px;line-height:1.6;\">Recibimos tu pago y tu orden ya está en marcha. Este es el resumen.</p>")
                .append("</td></tr>");

            // Numero de orden y fecha.
            html.append("<tr><td style=\"padding:20px 32px 4px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" bgcolor=\"#f8fafc\" style=\"background:#f8fafc;border-radius:10px;\"><tr>")
                .append(dato("N° de orden", "#" + ordenId))
                .append(dato("Fecha", fecha == null ? "-" : fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", CHILE))))
                .append("</tr></table></td></tr>");

            html.append("<tr><td style=\"padding:20px 32px 0;\">");
            html.append(tablaProductos(lineas, total));
            html.append("</td></tr>");

            if (!urlTienda.isEmpty()) {
                html.append("<tr><td align=\"center\" style=\"padding:28px 32px 8px;\">")
                    .append("<a href=\"").append(HtmlUtils.htmlEscape(urlTienda)).append("\" ")
                    .append("style=\"display:inline-block;background:").append(BOTON).append(";color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;padding:13px 30px;border-radius:99px;\">Ver mis órdenes</a>")
                    .append("</td></tr>");
            }
        }

        // Pie.
        html.append("<tr><td style=\"padding:28px 32px 30px;\">")
            .append("<hr style=\"border:0;border-top:1px solid ").append(LINEA).append(";margin:0 0 18px;\">")
            .append("<p style=\"margin:0 0 6px;color:").append(GRIS).append(";font-size:12.5px;line-height:1.6;\">Recibes este correo porque compraste en Pedidos360. Si no reconoces esta compra, comunícate con el administrador de la tienda.</p>")
            .append("<p style=\"margin:0;color:#94a3b8;font-size:12px;\">Mensaje automático, por favor no respondas a este correo.</p>")
            .append("</td></tr>");

        html.append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    /** Una celda de la franja con el numero de orden y la fecha. */
    private String dato(String titulo, String valor) {
        return "<td style=\"padding:14px 18px;\">"
                + "<div style=\"color:" + GRIS + ";font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;\">" + titulo + "</div>"
                + "<div style=\"font-size:16px;font-weight:bold;margin-top:3px;\">" + HtmlUtils.htmlEscape(valor) + "</div></td>";
    }

    /** Tabla con producto, cantidad, precio y subtotal, y el total destacado al final. */
    private String tablaProductos(List<LineaCorreo> lineas, Integer total) {
        StringBuilder tabla = new StringBuilder();
        tabla.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px;\">")
             .append("<tr>")
             .append(encabezado("Producto", "left"))
             .append(encabezado("Cant.", "center"))
             .append(encabezado("Precio", "right"))
             .append(encabezado("Subtotal", "right"))
             .append("</tr>");

        if (lineas != null) {
            for (LineaCorreo linea : lineas) {
                tabla.append("<tr>")
                     .append(celda(HtmlUtils.htmlEscape(linea.nombre()), "left", true))
                     .append(celda(String.valueOf(linea.cantidad()), "center", false))
                     .append(celda(pesos(linea.precioUnitario()), "right", false))
                     .append(celda(pesos(linea.subtotal()), "right", false))
                     .append("</tr>");
            }
        }

        tabla.append("<tr><td colspan=\"4\" align=\"right\" style=\"padding:18px 0 4px;\">")
             .append("<span style=\"color:").append(GRIS).append(";font-size:13px;\">Total pagado</span><br>")
             .append("<span style=\"font-size:28px;font-weight:bold;color:").append(MARINO).append(";\">").append(pesos(total == null ? 0 : total)).append("</span>")
             .append("</td></tr></table>");
        return tabla.toString();
    }

    private String encabezado(String texto, String alineacion) {
        return "<td align=\"" + alineacion + "\" style=\"padding:10px 6px;border-bottom:2px solid " + LINEA
                + ";color:" + GRIS + ";font-size:11.5px;text-transform:uppercase;letter-spacing:.06em;\">" + texto + "</td>";
    }

    private String celda(String contenido, String alineacion, boolean destacada) {
        return "<td align=\"" + alineacion + "\" style=\"padding:13px 6px;border-bottom:1px solid " + LINEA + ";"
                + (destacada ? "font-weight:bold;" : "") + "\">" + contenido + "</td>";
    }

    /** Version en texto plano del mismo correo. */
    private String cuerpoTexto(Long ordenId, Integer total, LocalDate fecha, List<LineaCorreo> lineas) {
        if (ordenId == null) {
            return "Pedidos360 - Este es un correo de prueba enviado desde ms-notificaciones.";
        }
        StringBuilder texto = new StringBuilder();
        texto.append("Pedidos360\n\nGracias por tu compra. Recibimos tu pago y tu orden #").append(ordenId).append(" ya está en marcha.\n");
        if (fecha != null) {
            texto.append("Fecha: ").append(fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", CHILE))).append("\n");
        }
        texto.append("\nDetalle:\n");
        if (lineas != null) {
            for (LineaCorreo linea : lineas) {
                texto.append("  ").append(linea.cantidad()).append(" x ").append(linea.nombre())
                     .append(" - ").append(pesos(linea.subtotal())).append("\n");
            }
        }
        texto.append("\nTotal pagado: ").append(pesos(total == null ? 0 : total)).append("\n");
        if (!urlTienda.isEmpty()) {
            texto.append("\nVer mis órdenes: ").append(urlTienda).append("\n");
        }
        texto.append("\nMensaje automático, por favor no respondas a este correo.\n");
        return texto.toString();
    }

    /** Pesos chilenos sin decimales, por ejemplo $45.990. El formateador se crea cada vez porque no es seguro entre hilos. */
    private String pesos(int monto) {
        return NumberFormat.getCurrencyInstance(CHILE).format(monto);
    }
}
