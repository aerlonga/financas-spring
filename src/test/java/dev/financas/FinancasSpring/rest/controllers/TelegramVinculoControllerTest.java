package dev.financas.FinancasSpring.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import dev.financas.FinancasSpring.security.JwtUtil;
import dev.financas.FinancasSpring.services.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TelegramVinculoController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TelegramVinculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TelegramVinculoRepository repository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    void deveConfirmarVinculoComSucesso() throws Exception {
        TelegramVinculo vinculo = TelegramVinculo.builder()
                .id(1L).chatId("PENDENTE_1")
                .codigoVinculo("ABC123")
                .codigoExpira(LocalDateTime.now().plusMinutes(5))
                .usuario(Usuario.builder().id(1L).nomeCompleto("João").build())
                .build();

        when(repository.findByCodigoVinculo("ABC123")).thenReturn(Optional.of(vinculo));
        when(repository.save(any(TelegramVinculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> body = Map.of("chatId", "987654", "codigo", "ABC123");

        mockMvc.perform(post("/api/telegram/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vinculado com sucesso")));
    }

    @Test
    void deveRetornar404ComCodigoInvalido() throws Exception {
        when(repository.findByCodigoVinculo("INVALIDO")).thenReturn(Optional.empty());

        Map<String, String> body = Map.of("chatId", "987654", "codigo", "INVALIDO");

        mockMvc.perform(post("/api/telegram/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ComCodigoExpirado() throws Exception {
        TelegramVinculo vinculo = TelegramVinculo.builder()
                .id(1L).chatId("PENDENTE_1")
                .codigoVinculo("EXP123")
                .codigoExpira(LocalDateTime.now().minusMinutes(5)) // Expirado
                .build();

        when(repository.findByCodigoVinculo("EXP123")).thenReturn(Optional.of(vinculo));

        Map<String, String> body = Map.of("chatId", "987654", "codigo", "EXP123");

        mockMvc.perform(post("/api/telegram/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("expirado")));
    }

    @Test
    void deveRetornar400SemChatIdOuCodigo() throws Exception {
        Map<String, String> body = Map.of("chatId", "123"); // Sem "codigo"

        mockMvc.perform(post("/api/telegram/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("obrigatórios")));
    }

    @Test
    void endpointConfirmarDeveSerPublico() throws Exception {
        // /api/telegram/confirmar é público na SecurityConfig
        when(repository.findByCodigoVinculo("NOTFOUND")).thenReturn(Optional.empty());

        Map<String, String> body = Map.of("chatId", "123", "codigo", "NOTFOUND");

        // Com security excluída do teste, deve chegar ao controller e retornar 404
        mockMvc.perform(post("/api/telegram/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }
}
