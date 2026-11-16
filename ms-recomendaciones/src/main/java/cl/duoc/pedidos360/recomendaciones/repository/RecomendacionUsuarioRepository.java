package cl.duoc.pedidos360.recomendaciones.repository;

import cl.duoc.pedidos360.recomendaciones.model.RecomendacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecomendacionUsuarioRepository extends JpaRepository<RecomendacionUsuario, Long> {

    List<RecomendacionUsuario> findByUsuarioOidOrderByPosicionAsc(String usuarioOid);

    void deleteByUsuarioOid(String usuarioOid);
}
