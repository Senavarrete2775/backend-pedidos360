package cl.tnt.backendpedidos360tnt.config;

import cl.tnt.backendpedidos360tnt.model.Pedido;
import cl.tnt.backendpedidos360tnt.repository.PedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(PedidoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Pedido("Empresa Alfa SpA", "Licencia Cloud Pro", 125000.0, "COMPLETADO"));
                repository.save(new Pedido("Logística Global Ltda", "Servidor Dedicado EC2", 340000.0, "PENDIENTE"));
                repository.save(new Pedido("Retail 360 Chile", "Consultoría Arquitectura Cloud", 890000.0, "EN_PROCESO"));
            }
        };
    }
}
