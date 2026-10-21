package cl.tnt.backendpedidos360tnt.controller;

import cl.tnt.backendpedidos360tnt.dto.PedidoEventDTO;
import cl.tnt.backendpedidos360tnt.model.Pedido;
import cl.tnt.backendpedidos360tnt.publisher.PedidoEventPublisher;
import cl.tnt.backendpedidos360tnt.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoEventPublisher pedidoEventPublisher;

    public PedidoController(PedidoRepository pedidoRepository, PedidoEventPublisher pedidoEventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoEventPublisher = pedidoEventPublisher;
    }

    @GetMapping("/public/status")
    public ResponseEntity<Map<String, Object>> estadoServicio() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ONLINE");
        response.put("servicio", "BFF Backend Pedidos360");
        response.put("mensaje", "Conexión exitosa al backend sin autenticación requerida.");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> obtenerPedidos(@AuthenticationPrincipal Jwt jwt) {
        List<Pedido> pedidos = pedidoRepository.findAll();

        String usuario = "usuario_autenticado";
        List<String> roles = List.of();
        String scp = "";

        if (jwt != null) {
            usuario = jwt.getClaimAsString("preferred_username") != null 
                    ? jwt.getClaimAsString("preferred_username") 
                    : jwt.getSubject();
            roles = jwt.getClaimAsStringList("roles");
            scp = jwt.getClaimAsString("scp");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "¡Acceso autorizado! Lista de pedidos obtenida desde la base de datos.");
        response.put("usuario", usuario);
        response.put("roles", roles);
        response.put("scp", scp);
        response.put("pedidos", pedidos);
        response.put("totalPedidos", pedidos.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedidos/admin")
    @PreAuthorize("hasAuthority('APPROLE_Admin') or hasAuthority('SCOPE_OT.Create')")
    public ResponseEntity<Map<String, Object>> panelAdministrador(@AuthenticationPrincipal Jwt jwt) {
        String usuario = jwt != null && jwt.getClaimAsString("preferred_username") != null
                ? jwt.getClaimAsString("preferred_username")
                : "admin_autenticado";
        List<String> roles = jwt != null ? jwt.getClaimAsStringList("roles") : List.of("APPROLE_Admin");
        String scp = jwt != null ? jwt.getClaimAsString("scp") : "OT.Create";

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Acceso concedido a zona administrativa por verificación de rol/scope.");
        response.put("usuario", usuario);
        response.put("rolesEncontrados", roles);
        response.put("scopesEncontrados", scp);
        response.put("metricas", Map.of(
                "totalVentas", 1355000.0,
                "servidoresActivos", 3,
                "estadoEC2", "RUNNING"
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pedidos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearPedido(@RequestBody Pedido pedido) {
        // Validaciones de negocio: Evitar crear pedidos inválidos o con montos negativos/cero
        if (pedido.getCliente() == null || pedido.getCliente().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre del cliente es obligatorio."));
        }
        if (pedido.getProducto() == null || pedido.getProducto().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El producto o servicio es obligatorio."));
        }
        if (pedido.getMonto() == null || pedido.getMonto() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El monto del pedido debe ser mayor a 0."));
        }

        if (pedido.getEstado() == null || pedido.getEstado().trim().isEmpty()) {
            pedido.setEstado("PENDIENTE");
        }

        Pedido nuevo = pedidoRepository.save(pedido);
        // Publicar evento asíncrono desacoplado a RabbitMQ
        pedidoEventPublisher.publicarPedidoCreado(nuevo);
        return ResponseEntity.ok(nuevo);
    }

    @DeleteMapping("/pedidos/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarPedido(@PathVariable Long id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "OK", "mensaje", "Pedido #" + id + " eliminado con éxito."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "El pedido no existe."));
    }

    @PostMapping("/pedidos/simular-error-dlq")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> simularFalloDLQ() {
        // Emite un evento inválido directo a RabbitMQ para activar basicNack y DLQ sin ensuciar la base de datos
        PedidoEventDTO eventoFallido = new PedidoEventDTO(
                999L,
                "Cliente Demostración DLQ",
                "Producto con Fallo Forzado",
                -1.0,
                "ERROR_SIMULADO",
                LocalDateTime.now(),
                "PEDIDO_FALLIDO"
        );
        pedidoEventPublisher.publicarEvento(eventoFallido);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "OK");
        resp.put("mensaje", "Evento con monto negativo (-$1.0) enviado a RabbitMQ. El consumidor 'ms-notificaciones' activará basicNack y lo enviará a la DLQ (pedidos.dlq).");
        resp.put("exchange", "pedidos.exchange");
        resp.put("evento", eventoFallido);
        return ResponseEntity.ok(resp);
    }
}
