package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Detalhes;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.repository.DetalhesRepository;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.DetalhesUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.DetalhesMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DetalhesServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DetalhesRepository usuarioDetalhesRepository;

    @Mock
    private DetalhesMapper usuarioDetalhesMapper;

    @InjectMocks
    private DetalhesService detalhesService;

    @Test
    void deveBuscarDetalhesPorUsuarioId() {
        Detalhes detalhes = Detalhes.builder().id(1L).cpf("12345678901").build();
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioDetalhesRepository.findByUsuarioId(1L)).thenReturn(Optional.of(detalhes));

        Detalhes resultado = detalhesService.findByUsuarioId(1L);

        assertThat(resultado.getCpf()).isEqualTo("12345678901");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> detalhesService.findByUsuarioId(99L));
    }

    @Test
    void deveLancarExcecaoQuandoDetalhesNaoExistem() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioDetalhesRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> detalhesService.findByUsuarioId(1L));
    }

    @Test
    void deveCriarNovosDetalhes() {
        Usuario usuario = Usuario.builder().id(1L).build();
        DetalhesUpdateDTO dto = new DetalhesUpdateDTO();
        dto.setCpf("12345678901");
        Detalhes novosDetalhes = Detalhes.builder().cpf("12345678901").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioDetalhesMapper.toEntity(dto)).thenReturn(novosDetalhes);
        when(usuarioDetalhesRepository.save(any(Detalhes.class))).thenReturn(novosDetalhes);

        Detalhes resultado = detalhesService.createOrUpdate(1L, dto);

        assertThat(resultado.getCpf()).isEqualTo("12345678901");
        verify(usuarioDetalhesRepository).save(novosDetalhes);
    }

    @Test
    void deveAtualizarDetalhesExistentes() {
        Detalhes existente = Detalhes.builder().id(1L).cpf("antigo").build();
        Usuario usuario = Usuario.builder().id(1L).build();
        usuario.setDetalhes(existente);
        DetalhesUpdateDTO dto = new DetalhesUpdateDTO();
        dto.setCpf("12345678901");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioDetalhesRepository.save(any(Detalhes.class))).thenReturn(existente);

        detalhesService.createOrUpdate(1L, dto);

        verify(usuarioDetalhesMapper).updateEntity(existente, dto);
        verify(usuarioDetalhesRepository).save(existente);
    }

    @Test
    void deveLancarExcecaoAoCriarParaUsuarioInexistente() {
        DetalhesUpdateDTO dto = new DetalhesUpdateDTO();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> detalhesService.createOrUpdate(99L, dto));
    }
}
