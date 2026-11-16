package cl.duoc.pedidos360.pagos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ordenId;

    @Column(nullable = false, length = 64)
    private String usuarioOid;

    @Column(nullable = false)
    private Integer monto;

    // PENDIENTE, APROBADO, RECHAZADO o FRAUDE
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 300)
    private String motivo;

    // WEBPAY o DIRECTO
    @Column(nullable = false, length = 20)
    private String medio;

    // Token de la transaccion en WebPay; no sale hacia el frontend por la respuesta de la API.
    @JsonIgnore
    @Column(length = 100)
    private String token;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Pago() {
    }

    public Pago(Long ordenId, String usuarioOid, Integer monto, String medio) {
        this.ordenId = ordenId;
        this.usuarioOid = usuarioOid;
        this.monto = monto;
        this.medio = medio;
        this.estado = "PENDIENTE";
        this.fecha = LocalDateTime.now();
    }

    public void resolver(String estado, String motivo) {
        this.estado = estado;
        this.motivo = motivo;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public Long getOrdenId() {
        return ordenId;
    }

    public String getUsuarioOid() {
        return usuarioOid;
    }

    public Integer getMonto() {
        return monto;
    }

    public String getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getMedio() {
        return medio;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
