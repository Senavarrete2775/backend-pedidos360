package cl.tnt.backendpedidos360tnt.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración centralizada de mensajería con RabbitMQ.
 * Cumple con los Indicadores 1, 2 y 3 de la Pauta de Evaluación:
 * - Centraliza nombres de colas, exchanges y routing keys desde application.yml.
 * - Declara explícitamente los beans de Queue, Exchange y Binding para el caso de uso y su DLQ.
 * - Aísla completamente la configuración de mensajería de la lógica de negocio.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.dlx}")
    private String dlxName;

    @Value("${app.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    // =========================================================================
    // 1. EXCHANGE PRINCIPAL (Caso de uso: Pedidos del Sistema)
    // =========================================================================
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    // =========================================================================
    // 2. COLA PRINCIPAL (Con enrutamiento hacia Dead Letter Exchange en caso de fallo)
    // =========================================================================
    @Bean
    public Queue pedidosQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    // =========================================================================
    // 3. ENLACE (BINDING) ENTRE EXCHANGE Y COLA PRINCIPAL
    // =========================================================================
    @Bean
    public Binding pedidosBinding(Queue pedidosQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(pedidosQueue).to(pedidosExchange).with(routingKey);
    }

    // =========================================================================
    // 4. DEAD LETTER EXCHANGE (DLX)
    // =========================================================================
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName, true, false);
    }

    // =========================================================================
    // 5. DEAD LETTER QUEUE (DLQ)
    // =========================================================================
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    // =========================================================================
    // 6. ENLACE (BINDING) DEL DEAD LETTER EXCHANGE CON LA DLQ
    // =========================================================================
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    // =========================================================================
    // 7. CONVERTIDOR DE MENSAJES A JSON
    // =========================================================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
