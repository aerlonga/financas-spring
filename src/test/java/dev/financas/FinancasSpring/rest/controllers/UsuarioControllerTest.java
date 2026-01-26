package dev.financas.FinancasSpring.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.financas.FinancasSpring.model.entities.*;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.*;
import dev.financas.FinancasSpring.rest.mapper.DetalhesMapper;
import dev.financas.FinancasSpring.rest.mapper.FinanceiroMapper;
import dev.financas.FinancasSpring.rest.mapper.UsuarioMapper;
import dev.financas.FinancasSpring.rest.mapper.PreferenciasMapper;
import dev.financas.FinancasSpring.security.JwtUtil;
import dev.financas.FinancasSpring.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioMapper usuarioMapper;

    @MockBean
    private DetalhesService usuarioDetalhesService;

    @MockBean
    private DetalhesMapper usuarioDetalhesMapper;

    @MockBean
    private FinanceiroService usuarioFinanceiroService;

    @MockBean
    private FinanceiroMapper usuarioFinanceiroMapper;

    @MockBean
    private PreferenciasService usuarioPreferenciasService;

    @MockBean
    private PreferenciasMapper usuarioPreferenciasMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser
    public void deveCriarUsuarioComSucesso() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UsuarioCreateDTO createDTO = UsuarioCreateDTO.builder()
                .nomeCompleto("Nome Teste")
                .email("teste@email.com")
                .senha("senha123")
                .build();

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nomeCompleto("Nome Teste")
                .email("teste@email.com")
                .build();

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id(1L)
                .nomeCompleto("Nome Teste")
                .email("teste@email.com")
                .build();

        when(usuarioMapper.toEntity(any(UsuarioCreateDTO.class))).thenReturn(usuario);
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toResponseDTO(any(Usuario.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/usuarios")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nomeCompleto").value("Nome Teste"))
                .andExpect(jsonPath("$.email").value("teste@email.com"));
    }

    @Test
    @WithMockUser
    public void deveBuscarUsuarioPorIdComSucesso() throws Exception {
        Long usuarioId = 1L;
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .nomeCompleto("Nome Teste")
                .email("teste@email.com")
                .build();

        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .id(usuarioId)
                .nomeCompleto("Nome Teste")
                .email("teste@email.com")
                .build();

        when(usuarioService.findById(usuarioId)).thenReturn(usuario);
        when(usuarioMapper.toResponseDTO(usuario)).thenReturn(responseDTO);

        mockMvc.perform(get("/usuarios/{id}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.nomeCompleto").value("Nome Teste"))
                .andExpect(jsonPath("$.email").value("teste@email.com"));
    }

    @Test
    @WithMockUser
    public void deveBuscarDetalhesDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        Detalhes detalhes = Detalhes.builder().id(1L).cpf("12345678901").build();
        DetalhesResponseDTO responseDTO = DetalhesResponseDTO.builder().cpf("12345678901").build();

        when(usuarioDetalhesService.findByUsuarioId(usuarioId)).thenReturn(detalhes);
        when(usuarioDetalhesMapper.toResponseDTO(detalhes)).thenReturn(responseDTO);

        mockMvc.perform(get("/usuarios/{id}/detalhes", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"));
    }

    @Test
    @WithMockUser
    public void deveAtualizarDetalhesDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        DetalhesUpdateDTO updateDTO = new DetalhesUpdateDTO();
        updateDTO.setCpf("09876543210");

        Detalhes detalhesAtualizados = Detalhes.builder().id(1L).cpf("09876543210").build();
        DetalhesResponseDTO responseDTO = DetalhesResponseDTO.builder().cpf("09876543210").build();

        when(usuarioDetalhesService.createOrUpdate(eq(usuarioId), any(DetalhesUpdateDTO.class))).thenReturn(detalhesAtualizados);
        when(usuarioDetalhesMapper.toResponseDTO(detalhesAtualizados)).thenReturn(responseDTO);

        mockMvc.perform(put("/usuarios/{id}/detalhes", usuarioId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("09876543210"));
    }

    @Test
    @WithMockUser
    public void deveBuscarFinanceiroDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        Financeiro financeiro = Financeiro.builder().id(1L).profissao("Engenheiro").build();
        FinanceiroResponseDTO responseDTO = FinanceiroResponseDTO.builder().profissao("Engenheiro").build();

        when(usuarioFinanceiroService.findByUsuarioId(usuarioId)).thenReturn(financeiro);
        when(usuarioFinanceiroMapper.toResponseDTO(financeiro)).thenReturn(responseDTO);

        mockMvc.perform(get("/usuarios/{id}/financeiro", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissao").value("Engenheiro"));
    }

    @Test
    @WithMockUser
    public void deveAtualizarFinanceiroDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        FinanceiroUpdateDTO updateDTO = new FinanceiroUpdateDTO();
        updateDTO.setProfissao("Desenvolvedor");

        Financeiro financeiroAtualizado = Financeiro.builder().id(1L).profissao("Desenvolvedor").build();
        FinanceiroResponseDTO responseDTO = FinanceiroResponseDTO.builder().profissao("Desenvolvedor").build();

        when(usuarioFinanceiroService.createOrUpdate(eq(usuarioId), any(FinanceiroUpdateDTO.class))).thenReturn(financeiroAtualizado);
        when(usuarioFinanceiroMapper.toResponseDTO(financeiroAtualizado)).thenReturn(responseDTO);

        mockMvc.perform(put("/usuarios/{id}/financeiro", usuarioId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissao").value("Desenvolvedor"));
    }

    @Test
    @WithMockUser
    public void deveBuscarPreferenciasDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        Preferencias preferencias = Preferencias.builder().id(1L).moedaPreferida("USD").build();
        PreferenciasResponseDTO responseDTO = PreferenciasResponseDTO.builder().moedaPreferida("USD").build();

        when(usuarioPreferenciasService.findByUsuarioId(usuarioId)).thenReturn(preferencias);
        when(usuarioPreferenciasMapper.toResponseDTO(preferencias)).thenReturn(responseDTO);

        mockMvc.perform(get("/usuarios/{id}/preferencias", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moedaPreferida").value("USD"));
    }

    @Test
    @WithMockUser
    public void deveAtualizarPreferenciasDoUsuarioComSucesso() throws Exception {
        Long usuarioId = 1L;
        PreferenciasUpdateDTO updateDTO = new PreferenciasUpdateDTO();
        updateDTO.setMoedaPreferida("EUR");

        Preferencias preferenciasAtualizadas = Preferencias.builder().id(1L).moedaPreferida("EUR").build();
        PreferenciasResponseDTO responseDTO = PreferenciasResponseDTO.builder().moedaPreferida("EUR").build();

        when(usuarioPreferenciasService.createOrUpdate(eq(usuarioId), any(PreferenciasUpdateDTO.class))).thenReturn(preferenciasAtualizadas);
        when(usuarioPreferenciasMapper.toResponseDTO(preferenciasAtualizadas)).thenReturn(responseDTO);

        mockMvc.perform(put("/usuarios/{id}/preferencias", usuarioId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moedaPreferida").value("EUR"));
    }
}
