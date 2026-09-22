package cl.tnt.backendpedidos360tnt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointPublicoDeberiaResponder200SinToken() throws Exception {
        mockMvc.perform(get("/api/public/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    void endpointPedidosSinTokenDeberiaResponder401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "admin@duocuc.cl", authorities = {"APPROLE_Admin"})
    void endpointAdminConRolAdecuadoDeberiaResponder200() throws Exception {
        mockMvc.perform(get("/api/pedidos/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @WithMockUser(username = "usuario@duocuc.cl", authorities = {"ROLE_USER"})
    void endpointAdminSinRolAdecuadoDeberiaResponder403() throws Exception {
        mockMvc.perform(get("/api/pedidos/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
