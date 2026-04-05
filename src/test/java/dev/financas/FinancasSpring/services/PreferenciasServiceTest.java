package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Preferencias;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.repository.PreferenciasRepository;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.PreferenciasUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.PreferenciasMapper;
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
class PreferenciasServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PreferenciasRepository usuarioPreferenciasRepository;

    @Mock
    private PreferenciasMapper usuarioPreferenciasMapper;

    @InjectMocks
    private PreferenciasService preferenciasService;

    @Test
    void deveBuscarPreferenciasPorUsuarioId() {
        Preferencias prefs = Preferencias.builder().id(1L).moedaPreferida("USD").build();
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioPreferenciasRepository.findByUsuarioId(1L)).thenReturn(Optional.of(prefs));

        Preferencias resultado = preferenciasService.findByUsuarioId(1L);

        assertThat(resultado.getMoedaPreferida()).isEqualTo("USD");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> preferenciasService.findByUsuarioId(99L));
    }

    @Test
    void deveLancarExcecaoQuandoPreferenciasNaoExistem() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioPreferenciasRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> preferenciasService.findByUsuarioId(1L));
    }

    @Test
    void deveCriarNovasPreferencias() {
        Usuario usuario = Usuario.builder().id(1L).build();
        PreferenciasUpdateDTO dto = new PreferenciasUpdateDTO(Preferencias.TemaInterface.ESCURO, true, "EUR", null);
        Preferencias novas = Preferencias.builder().moedaPreferida("EUR").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioPreferenciasMapper.toEntity(dto)).thenReturn(novas);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Preferencias resultado = preferenciasService.createOrUpdate(1L, dto);

        assertThat(resultado.getMoedaPreferida()).isEqualTo("EUR");
    }

    @Test
    void deveAtualizarPreferenciasExistentes() {
        Preferencias existente = Preferencias.builder().id(1L).moedaPreferida("BRL").build();
        Usuario usuario = Usuario.builder().id(1L).build();
        usuario.setPreferencias(existente);
        PreferenciasUpdateDTO dto = new PreferenciasUpdateDTO(null, null, "USD", null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        preferenciasService.createOrUpdate(1L, dto);

        verify(usuarioPreferenciasMapper).updateEntity(existente, dto);
    }

    @Test
    void deveLancarExcecaoAoCriarParaUsuarioInexistente() {
        PreferenciasUpdateDTO dto = new PreferenciasUpdateDTO();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> preferenciasService.createOrUpdate(99L, dto));
    }
}
