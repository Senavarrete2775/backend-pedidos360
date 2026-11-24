package cl.tnt.backendpedidos360tnt.publisher;

import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Productor Kafka de eventos de pedidos. Publica el DTO (serializado a JSON) en dos tópicos,
 * usando el id del pedido como clave para que todos los eventos de un pedido vayan a la misma partición.
 */
@Service
public class KafkaPedidoEventPublisher implements PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPedidoEventPublisher.class);

    private final KafkaTemplate<String, PedidoEventDTO> kafkaTemplate;
    private final String topicEventos;
    private final String topicEstado;

    public KafkaPedidoEventPublisher(KafkaTemplate<String, PedidoEventDTO> kafkaTemplate,
                                     @Value("${app.kafka.topics.eventos}") String topicEventos,
                                     @Value("${app.kafka.topics.estado}") String topicEstado) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicEventos = topicEventos;
        this.topicEstado = topicEstado;
    }

    @Override
    public void publicarPedidoCreado(Pedido pedido) {
        publicarEvento(new PedidoEventDTO(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProducto(),
                pedido.getMonto(),
                pedido.getEstado(),
                pedido.getFecha(),
                "PEDIDO_CREADO"));
    }

    @Override
    public void publicarEvento(PedidoEventDTO evento) {
        // En segundo plano para no bloquear la petición HTTP si Kafka tarda en responder
        CompletableFuture.runAsync(() -> {
            String clave = String.valueOf(evento.getId());
            enviar(topicEventos, clave, evento);
            enviar(topicEstado, clave, evento);
        });
    }

    private void enviar(String topic, String clave, PedidoEventDTO evento) {
        try {
            kafkaTemplate.send(topic, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Error al publicar en Kafka [topic '{}', clave '{}']: {}", topic, clave, error.getMessage());
                } else {
                    log.info("Evento publicado en Kafka [topic '{}', partición {}, offset {}, clave '{}']",
                            topic, resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset(), clave);
                }
            });
        } catch (Exception e) {
            log.error("No se pudo enviar a Kafka [topic '{}']: {}", topic, e.getMessage());
        }
    }
}
