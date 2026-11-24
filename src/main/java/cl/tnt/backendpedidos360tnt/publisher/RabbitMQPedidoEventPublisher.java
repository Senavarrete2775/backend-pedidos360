package cl.tnt.backendpedidos360tnt.publisher;

import cl.tnt.backendpedidos360tnt.config.RabbitMQProperties;
import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class RabbitMQPedidoEventPublisher implements PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQPedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public RabbitMQPedidoEventPublisher(RabbitTemplate rabbitTemplate, RabbitMQProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public void publicarPedidoCreado(Pedido pedido) {
        PedidoEventDTO evento = new PedidoEventDTO(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getMonto(),
                pedido.getEstado(),
                pedido.getFecha(),
                "PEDIDO_CREADO"
        );
        publicarEvento(evento);
    }

    /**
     * Publica el evento en dos rutas:
     *  - exchange TOPIC (pedidos.exchange) -> colas de notificación y auditoría
     *  - exchange DIRECT (tickets.exchange) -> cola de tickets
     */
    @Override
    public void publicarEvento(PedidoEventDTO evento) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Publicando evento a RabbitMQ [topic: '{}' / key '{}'] y [direct: '{}' / key '{}']: {}",
                        props.exchanges().pedidos(), props.routingKeys().pedidoCreado(),
                        props.exchanges().tickets(), props.routingKeys().ticket(), evento);
                rabbitTemplate.convertAndSend(props.exchanges().pedidos(), props.routingKeys().pedidoCreado(), evento);
                rabbitTemplate.convertAndSend(props.exchanges().tickets(), props.routingKeys().ticket(), evento);
                log.info("Evento publicado exitosamente para pedido ID={}", evento.getId());
            } catch (Exception e) {
                log.error("Error al publicar evento de pedido en RabbitMQ: {}", e.getMessage(), e);
            }
        });
    }
}
