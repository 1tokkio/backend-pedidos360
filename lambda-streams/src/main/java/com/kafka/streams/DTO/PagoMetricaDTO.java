package com.kafka.streams.DTO;

/** Un pago tal como lo recibe el panel en vivo. Los campos salen del item de MetricasPagos. */
public class PagoMetricaDTO {
    private String idPago;
    private Long ordenId;
    private Long monto;
    private String estado;
    private String motivo;
    private String medio;
    private Long timestamp;

    public void setIdPago(String idPago) {
        this.idPago = idPago;
    }

    public String getIdPago() {
        return this.idPago;
    }

    public void setOrdenId(Long ordenId) {
        this.ordenId = ordenId;
    }

    public Long getOrdenId() {
        return this.ordenId;
    }

    public void setMonto(Long monto) {
        this.monto = monto;
    }

    public Long getMonto() {
        return this.monto;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return this.motivo;
    }

    public void setMedio(String medio) {
        this.medio = medio;
    }

    public String getMedio() {
        return this.medio;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }
}
