package cl.duoc.pedidos360.recomendaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Lo que hizo un usuario con un producto: verlo (1), agregarlo al carrito (2) o comprarlo (3). */
@Entity
@Table(name = "interaccion")
public class Interaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String usuarioOid;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String nombreProducto;

    @Column(nullable = false, length = 20)
    private String accion;

    @Column(nullable = false)
    private int peso;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Interaccion() {
    }

    public Interaccion(String usuarioOid, Long productoId, String nombreProducto, String accion, int peso) {
        this.usuarioOid = usuarioOid;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.accion = accion;
        this.peso = peso;
        this.fecha = LocalDateTime.now();
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getPeso() {
        return peso;
    }
}
