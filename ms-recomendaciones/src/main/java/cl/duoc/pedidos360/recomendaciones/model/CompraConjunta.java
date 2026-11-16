package cl.duoc.pedidos360.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cuantas veces el producto relacionado se compro en la misma orden que el producto. */
@Entity
@Table(name = "compra_conjunta")
public class CompraConjunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Long relacionadoId;

    @Column(nullable = false, length = 120)
    private String nombreRelacionado;

    @Column(nullable = false)
    private Long veces;

    public CompraConjunta() {
    }

    public CompraConjunta(Long productoId, Long relacionadoId, String nombreRelacionado) {
        this.productoId = productoId;
        this.relacionadoId = relacionadoId;
        this.nombreRelacionado = nombreRelacionado;
        this.veces = 0L;
    }

    public void sumar() {
        this.veces++;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public Long getRelacionadoId() {
        return relacionadoId;
    }

    public String getNombreRelacionado() {
        return nombreRelacionado;
    }

    public Long getVeces() {
        return veces;
    }
}
