package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Un grupo de consumidores con su estado y su lag: lo que le falta por leer a cada particion. */
public record GrupoConsumidor(String grupo, String estado, long lagTotal, List<LagParticion> particiones) {

    public record LagParticion(String topico, int particion, long offsetActual, long offsetFinal, long lag) {
    }
}
