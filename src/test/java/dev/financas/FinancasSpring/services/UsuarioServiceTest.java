package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.BusinessException;
import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.UsuarioUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.UsuarioMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveSalvarNovoUsuarioComSucesso() {
        // Cenário
        Usuario novoUsuario = Usuario.builder().email("novo@email.com").build();
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(novoUsuario);

        // Execução
        Usuario usuarioSalvo = usuarioService.save(novoUsuario);

        // Verificação
        assertThat(usuarioSalvo).isNotNull();
        assertThat(usuarioSalvo.getCriadoPor()).isEqualTo("novo@email.com");
        verify(usuarioRepository).save(novoUsuario); // Verifica se o método save foi chamado
    }

    @Test
    void naoDeveSalvarUsuarioComEmailDuplicado() {
        // Cenário
        Usuario usuarioExistente = Usuario.builder().email("existente@email.com").build();
        when(usuarioRepository.existsByEmail("existente@email.com")).thenReturn(true);

        // Execução e Verificação
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            usuarioService.save(usuarioExistente);
        });

        assertThat(exception.getMessage()).isEqualTo("O e-mail informado já está em uso.");
        verify(usuarioRepository, never()).save(any(Usuario.class)); // Verifica que o save NUNCA foi chamado
    }

    @Test
    void deveEncontrarUsuarioPorIdComSucesso() {
        // Cenário
        Long id = 1L;
        Usuario usuario = Usuario.builder().id(id).build();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        // Execução
        Usuario usuarioEncontrado = usuarioService.findById(id);

        // Verificação
        assertThat(usuarioEncontrado).isNotNull();
        assertThat(usuarioEncontrado.getId()).isEqualTo(id);
    }

    @Test
    void deveLancarExcecaoAoNaoEncontrarUsuarioPorId() {
        // Cenário
        Long id = 99L;
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        // Execução e Verificação
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            usuarioService.findById(id);
        });

        assertThat(exception.getMessage()).isEqualTo("Usuário não encontrado com o ID: " + id);
    }

    @Test
    void deveListarTodosUsuarios() {
        // Cenário
        Usuario usuario = Usuario.builder().id(1L).build();
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        // Execução
        List<Usuario> usuarios = usuarioService.findAll();

        // Verificação
        assertThat(usuarios).isNotEmpty();
        assertThat(usuarios).hasSize(1);
    }

    @Test
    void deveBuscarUsuarioPorEmailComTudo() {
        // Cenário
        String email = "teste@email.com";
        Usuario usuario = Usuario.builder().email(email).build();
        when(usuarioRepository.findWithAllRelationsByEmail(email)).thenReturn(Optional.of(usuario));

        // Execução
        Usuario resultado = usuarioService.findByEmailComTudo(email);

        // Verificação
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo(email);
    }

    @Test
    void deveDeletarUsuarioComSucesso() {
        // Cenário
        Long id = 1L;
        when(usuarioRepository.existsById(id)).thenReturn(true);

        // Execução
        usuarioService.deleteById(id);

        // Verificação
        verify(usuarioRepository).deleteById(id);
    }

    @Test
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        // Cenário
        Long id = 99L;
        when(usuarioRepository.existsById(id)).thenReturn(false);

        // Execução e Verificação
        assertThrows(ResourceNotFoundException.class, () -> usuarioService.deleteById(id));
        verify(usuarioRepository, never()).deleteById(anyLong());
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {
        // Cenário
        Long id = 1L;
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNomeCompleto("Nome Novo");

        Usuario usuarioExistente = Usuario.builder().id(id).nomeCompleto("Nome Antigo").build();
        Usuario usuarioSalvo = Usuario.builder().id(id).nomeCompleto("Nome Novo").build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        // Execução
        Usuario resultado = usuarioService.atualizar(id, dto);

        // Verificação
        assertThat(resultado.getNomeCompleto()).isEqualTo("Nome Novo");
        verify(usuarioMapper).updateEntity(usuarioExistente, dto); // Verifica se o mapper foi chamado
        verify(usuarioRepository).save(usuarioExistente);
    }
}
