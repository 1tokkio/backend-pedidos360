package cl.duoc.pedidos360.pagos.repository;

import cl.duoc.pedidos360.pagos.model.EstadoFraude;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoFraudeRepository extends JpaRepository<EstadoFraude, String> {
}
