package cl.duoc.pedidos360.notificaciones.service;

import cl.duoc.pedidos360.notificaciones.model.NotificacionEnviada;
import cl.duoc.pedidos360.notificaciones.repository.NotificacionEnviadaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotificacionService {

    private final CorreoService correo;
    private final NotificacionEnviadaRepository repositorio;

    public NotificacionService(CorreoService correo, NotificacionEnviadaRepository repositorio) {
        this.correo = correo;
        this.repositorio = repositorio;
    }

    /** Envio de prueba, sin fecha ni productos porque no viene de una orden real. */
    public NotificacionEnviada enviar(String destinatario, Long ordenId, Integer total) {
        return enviar(destinatario, ordenId, total, null, List.of());
    }

    /** Envia el correo y deja registrado el intento, haya salido o no. */
    public NotificacionEnviada enviar(String destinatario, Long ordenId, Integer total, LocalDate fecha,
                                      List<LineaCorreo> lineas) {
        boolean enviada = correo.enviarConfirmacion(destinatario, ordenId, total, fecha, lineas);
        String asunto = correo.asuntoPara(ordenId);
        return repositorio.save(new NotificacionEnviada(destinatario, asunto, ordenId, total, enviada));
    }

    public List<NotificacionEnviada> listar() {
        return repositorio.findAllByOrderByFechaEnvioDesc();
    }
}
