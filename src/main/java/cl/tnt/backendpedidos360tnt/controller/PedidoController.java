package cl.tnt.backendpedidos360tnt.controller;

import cl.tnt.backendpedidos360tnt.model.Pedido;
import cl.tnt.backendpedidos360tnt.repository.PedidoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.tnt.backendpedidos360tnt.publisher.PedidoEventPublisher;

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
    public ResponseEntity<Pedido> crearPedido(@RequestBody Pedido pedido) {
        Pedido nuevo = pedidoRepository.save(pedido);
        // Publicar evento asíncrono desacoplado
        pedidoEventPublisher.publicarPedidoCreado(nuevo);
        return ResponseEntity.ok(nuevo);
    }
}