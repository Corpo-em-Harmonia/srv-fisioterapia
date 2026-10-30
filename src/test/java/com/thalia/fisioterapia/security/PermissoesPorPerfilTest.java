package com.thalia.fisioterapia.security;

import com.thalia.fisioterapia.infrastructure.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que cada perfil só alcança o que deve. Só testa as negações: elas acontecem antes de
 * chegar ao banco, então o teste não precisa de Mongo.
 */
@SpringBootTest(properties = {
        "spring.mongodb.uri=mongodb://localhost:27017/fisioterapia_test",
        "spring.data.mongodb.auto-index-creation=false",
        "app.mongo.transactions=false",
        "auth.admin.password=senha-de-teste-123"
})
class PermissoesPorPerfilTest {

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext context;

    // Corpo válido: a validação do @RequestBody roda antes do @PreAuthorize, então com corpo
    // inválido a resposta seria 400 e o teste não provaria a regra de perfil.
    private static final String EVOLUCAO_VALIDA = "{\"observacoes\":\"ok\",\"nivelDor\":2}";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void semLogin_recebe401() throws Exception {
        mvc.perform(get("/api/sessoes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void paciente_naoAcessaLeadsAvaliacoesNemAgendaDaClinica() throws Exception {
        mvc.perform(get("/api/leads")).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes/historico")).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes/pendentes")).andExpect(status().isForbidden());
        mvc.perform(get("/api/pacientes/ativos")).andExpect(status().isForbidden());
        mvc.perform(get("/api/sessoes/estatisticas")).andExpect(status().isForbidden());
        mvc.perform(get("/api/agendamentos/disponibilidade").param("date", "2026-11-03")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void paciente_naoAlteraSessoes() throws Exception {
        mvc.perform(patch("/api/sessoes/s1/cancelar")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/sessoes/s1/compareceu")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/sessoes/s1/evolucao").contentType("application/json").content(EVOLUCAO_VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcao_naoAcessaProntuario() throws Exception {
        mvc.perform(get("/api/avaliacoes/historico")).andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes/av1")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/sessoes/s1/evolucao").contentType("application/json").content(EVOLUCAO_VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FISIOTERAPEUTA")
    void fisioterapeuta_naoMexeEmLeadsNemUsuarios() throws Exception {
        mvc.perform(get("/api/leads")).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }
}
