package cl.duoc.pedidos360.pagos.repository;

import cl.duoc.pedidos360.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findAllByOrderByFechaDesc();

    List<Pago> findByUsuarioOidOrderByFechaDesc(String usuarioOid);

    Optional<Pago> findByToken(String token);
}
