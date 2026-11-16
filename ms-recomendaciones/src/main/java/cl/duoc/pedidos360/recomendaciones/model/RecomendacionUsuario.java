package cl.duoc.pedidos360.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uno de los cinco productos que mejor puntaje tienen para un usuario. Se recalcula con cada interaccion. */
@Entity
@Table(name = "recomendacion_usuario")
public class RecomendacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String usuarioOid;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String nombreProducto;

    @Column(nullable = false)
    private Long puntaje;

    @Column(nullable = false)
    private int posicion;

    public RecomendacionUsuario() {
    }

    public RecomendacionUsuario(String usuarioOid, Long productoId, String nombreProducto, Long puntaje, int posicion) {
        this.usuarioOid = usuarioOid;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.puntaje = puntaje;
        this.posicion = posicion;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public Long getPuntaje() {
        return puntaje;
    }
}
