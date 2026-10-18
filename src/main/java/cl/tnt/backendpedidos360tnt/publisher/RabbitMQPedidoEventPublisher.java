package cl.tnt.backendpedidos360tnt.publisher;

import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementación del publicador de eventos asíncronos mediante RabbitMQ.
 * Cumple con el Indicador 3: Los servicios y controladores de negocio solo
 * consumen esta interfaz sin conocer detalles de configuración de colas.
 */
@Service
public class RabbitMQPedidoEventPublisher implements PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQPedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public RabbitMQPedidoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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

    @Override
    public void publicarEvento(PedidoEventDTO evento) {
        try {
            log.info("Publicando evento asíncrono a RabbitMQ [Exchange: '{}', RoutingKey: '{}']: {}",
                    exchange, routingKey, evento);
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            log.info("Evento publicado exitosamente para pedido ID={}", evento.getId());
        } catch (Exception e) {
            log.error("Error al publicar evento de pedido en RabbitMQ: {}", e.getMessage(), e);
            // No interrumpe la transacción del negocio, garantizando tolerancia a fallos
        }
    }
}
