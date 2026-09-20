package com.thalia.fisioterapia.web.exception;

import com.thalia.fisioterapia.application.exception.BusinessException;
import com.thalia.fisioterapia.application.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @RestController
    static class LancadorController {
        @GetMapping("/erro/{tipo}")
        String lancar(@PathVariable String tipo) {
            return switch (tipo) {
                case "400" -> throw new BusinessException("dado inválido");
                case "403" -> throw new AccessDeniedException("Você só pode cadastrar pacientes.");
                case "403-padrao" -> throw new AccessDeniedException("Access Denied");
                case "409" -> throw new ConflictException("E-mail já cadastrado");
                default -> "ok";
            };
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LancadorController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void badRequestTrazMensagem() throws Exception {
        mvc.perform(get("/erro/400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("dado inválido"));
    }

    @Test
    void forbiddenTrazMensagemDoServico() throws Exception {
        mvc.perform(get("/erro/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você só pode cadastrar pacientes."));
    }

    @Test
    void forbiddenPadraoDoSpringViraMensagemEmPortugues() throws Exception {
        mvc.perform(get("/erro/403-padrao"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para realizar esta ação."));
    }

    @Test
    void emailDuplicadoRetorna409ComMensagem() throws Exception {
        mvc.perform(get("/erro/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }
}
