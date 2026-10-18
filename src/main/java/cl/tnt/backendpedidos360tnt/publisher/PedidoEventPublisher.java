package cl.tnt.backendpedidos360tnt.publisher;

import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;

/**
 * Interfaz que desacopla la lógica de negocio del mecanismo de mensajería (Indicador 3).
 */
public interface PedidoEventPublisher {

    /**
     * Publica un evento asíncrono cuando se registra un nuevo pedido.
     *
     * @param pedido Pedido registrado
     */
    void publicarPedidoCreado(Pedido pedido);

    /**
     * Permite publicar directamente un DTO de evento.
     *
     * @param evento Evento a publicar
     */
    void publicarEvento(PedidoEventDTO evento);
}
