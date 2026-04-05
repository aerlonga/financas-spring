package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import dev.financas.FinancasSpring.rest.dto.UsuarioCreateDTO;
import dev.financas.FinancasSpring.rest.mapper.UsuarioMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramVinculoServiceTest {

    @Mock
    private TelegramVinculoRepository repository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private TelegramVinculoService vinculoService;

    @Test
    void deveRetornarVinculoExistenteEAtualizar() {
        TelegramVinculo existente = TelegramVinculo.builder()
                .id(1L).chatId("123").pushname("Antigo").build();

        when(repository.findByChatId("123")).thenReturn(Optional.of(existente));
        when(repository.save(any(TelegramVinculo.class))).thenAnswer(inv -> inv.getArgument(0));

        TelegramVinculo resultado = vinculoService.obterOuCriar("123", "Novo Nome");

        assertThat(resultado.getPushname()).isEqualTo("Novo Nome");
        assertThat(resultado.getUltimaAtividade()).isNotNull();
        verify(repository).save(existente);
    }

    @Test
    void deveCriarNovoVinculoQuandoNaoExiste() {
        when(repository.findByChatId("456")).thenReturn(Optional.empty());
        when(repository.save(any(TelegramVinculo.class))).thenAnswer(inv -> inv.getArgument(0));

        TelegramVinculo resultado = vinculoService.obterOuCriar("456", "João");

        assertThat(resultado.getChatId()).isEqualTo("456");
        assertThat(resultado.getPushname()).isEqualTo("João");
        verify(repository).save(any(TelegramVinculo.class));
    }

    @Test
    void deveAutenticarEVincularComSucesso() {
        Usuario usuario = Usuario.builder().id(1L).email("user@email.com").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(usuario);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        TelegramVinculo vinculo = TelegramVinculo.builder().id(1L).chatId("123").build();
        when(repository.findByChatId("123")).thenReturn(Optional.of(vinculo));
        when(repository.save(any(TelegramVinculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = vinculoService.autenticarEVincular("123", "user@email.com", "senha123");

        assertThat(resultado.getEmail()).isEqualTo("user@email.com");
        assertThat(vinculo.getUsuario()).isEqualTo(usuario);
        verify(repository, atLeast(1)).save(any());
    }

    @Test
    void deveLancarExcecaoComCredenciaisInvalidas() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThrows(BadCredentialsException.class,
                () -> vinculoService.autenticarEVincular("123", "user@email.com", "senhaErrada"));
    }

    @Test
    void deveCadastrarEVincularComSucesso() {
        Usuario novoUsuario = Usuario.builder().id(1L).email("novo@email.com").nomeCompleto("Nome").build();
        when(usuarioMapper.toEntity(any(UsuarioCreateDTO.class))).thenReturn(novoUsuario);
        when(usuarioService.save(any(Usuario.class))).thenReturn(novoUsuario);

        TelegramVinculo vinculo = TelegramVinculo.builder().id(1L).chatId("123").build();
        when(repository.findByChatId("123")).thenReturn(Optional.of(vinculo));
        when(repository.save(any(TelegramVinculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = vinculoService.cadastrarEVincular("123", "João", "Nome", "novo@email.com", "senha123");

        assertThat(resultado.getEmail()).isEqualTo("novo@email.com");
        verify(usuarioService).save(any());
        verify(repository, atLeast(1)).save(any());
    }

    @Test
    void deveDesvincularComSucesso() {
        TelegramVinculo vinculo = TelegramVinculo.builder()
                .id(1L).chatId("123")
                .usuario(Usuario.builder().id(1L).build())
                .build();
        when(repository.findByChatId("123")).thenReturn(Optional.of(vinculo));

        vinculoService.desvincular("123");

        assertThat(vinculo.getUsuario()).isNull();
        verify(repository).save(vinculo);
    }

    @Test
    void desvincularNaoDeveFalharComChatIdInexistente() {
        when(repository.findByChatId("999")).thenReturn(Optional.empty());

        // Não deve lançar exceção
        vinculoService.desvincular("999");

        verify(repository, never()).save(any());
    }
}
