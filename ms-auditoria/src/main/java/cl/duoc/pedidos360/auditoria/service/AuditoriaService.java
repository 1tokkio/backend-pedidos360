package cl.duoc.pedidos360.auditoria.service;

import cl.duoc.pedidos360.auditoria.model.EventoAuditoria;
import cl.duoc.pedidos360.auditoria.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repositorio;

    public AuditoriaService(EventoAuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void registrar(String servicio, String tipo, String detalle) {
        repositorio.save(new EventoAuditoria(servicio, tipo, detalle == null ? "" : detalle));
    }

    public List<EventoAuditoria> listar() {
        return repositorio.findAllByOrderByFechaRegistroDesc();
    }
}
