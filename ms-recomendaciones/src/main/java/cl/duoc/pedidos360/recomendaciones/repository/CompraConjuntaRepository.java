package cl.duoc.pedidos360.recomendaciones.repository;

import cl.duoc.pedidos360.recomendaciones.model.CompraConjunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompraConjuntaRepository extends JpaRepository<CompraConjunta, Long> {

    Optional<CompraConjunta> findByProductoIdAndRelacionadoId(Long productoId, Long relacionadoId);

    List<CompraConjunta> findTop3ByProductoIdOrderByVecesDesc(Long productoId);
}
