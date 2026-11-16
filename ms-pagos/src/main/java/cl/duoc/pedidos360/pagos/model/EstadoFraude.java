package cl.duoc.pedidos360.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Cuantos intentos de pago seguidos lleva un usuario y hasta cuando esta bloqueado. */
@Entity
@Table(name = "estado_fraude")
public class EstadoFraude {

    @Id
    @Column(name = "usuario_oid", length = 150)
    private String usuarioOid;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_intento")
    private LocalDateTime ultimoIntento;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    protected EstadoFraude() {
    }

    public EstadoFraude(String usuarioOid) {
        this.usuarioOid = usuarioOid;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    public String getUsuarioOid() {
        return usuarioOid;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public LocalDateTime getUltimoIntento() {
        return ultimoIntento;
    }

    public void setUltimoIntento(LocalDateTime ultimoIntento) {
        this.ultimoIntento = ultimoIntento;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }
}
