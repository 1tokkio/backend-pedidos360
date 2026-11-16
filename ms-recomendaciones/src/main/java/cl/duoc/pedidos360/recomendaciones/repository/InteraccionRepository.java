package cl.duoc.pedidos360.recomendaciones.repository;

import cl.duoc.pedidos360.recomendaciones.model.Interaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteraccionRepository extends JpaRepository<Interaccion, Long> {

    List<Interaccion> findByUsuarioOid(String usuarioOid);
}
