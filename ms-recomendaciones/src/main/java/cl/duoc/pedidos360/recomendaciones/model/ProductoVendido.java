package cl.duoc.pedidos360.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cuantas unidades se han vendido de cada producto. De aca salen los mas vendidos. */
@Entity
@Table(name = "producto_vendido")
public class ProductoVendido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String nombreProducto;

    @Column(nullable = false)
    private Long cantidad;

    public ProductoVendido() {
    }

    public ProductoVendido(Long productoId, String nombreProducto) {
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = 0L;
    }

    public void sumar(int unidades) {
        this.cantidad += unidades;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public Long getCantidad() {
        return cantidad;
    }
}
