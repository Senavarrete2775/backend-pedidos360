package cl.tnt.backendpedidos360tnt.publisher;

import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;

public interface PedidoEventPublisher {


    void publicarPedidoCreado(Pedido pedido);


    void publicarEvento(PedidoEventDTO evento);
}
