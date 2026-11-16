package cl.duoc.pedidos360.recomendaciones.repository;

import cl.duoc.pedidos360.recomendaciones.model.ProductoVendido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoVendidoRepository extends JpaRepository<ProductoVendido, Long> {

    Optional<ProductoVendido> findByProductoId(Long productoId);

    List<ProductoVendido> findTop4ByOrderByCantidadDesc();
}
