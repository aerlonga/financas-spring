package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Financeiro;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.repository.FinanceiroRepository;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.FinanceiroUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.FinanceiroMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FinanceiroRepository usuarioFinanceiroRepository;

    @Mock
    private FinanceiroMapper usuarioFinanceiroMapper;

    @InjectMocks
    private FinanceiroService financeiroService;

    @Test
    void deveBuscarFinanceiroPorUsuarioId() {
        Financeiro financeiro = Financeiro.builder().id(1L).profissao("Dev").build();
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioFinanceiroRepository.findByUsuarioId(1L)).thenReturn(Optional.of(financeiro));

        Financeiro resultado = financeiroService.findByUsuarioId(1L);

        assertThat(resultado.getProfissao()).isEqualTo("Dev");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> financeiroService.findByUsuarioId(99L));
    }

    @Test
    void deveLancarExcecaoQuandoFinanceiroNaoExiste() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioFinanceiroRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> financeiroService.findByUsuarioId(1L));
    }

    @Test
    void deveCriarNovoFinanceiro() {
        Usuario usuario = Usuario.builder().id(1L).build();
        FinanceiroUpdateDTO dto = new FinanceiroUpdateDTO("Dev", BigDecimal.valueOf(5000), "CLT", "Investir", BigDecimal.valueOf(1000));
        Financeiro novoFinanceiro = Financeiro.builder().profissao("Dev").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioFinanceiroMapper.toEntity(dto)).thenReturn(novoFinanceiro);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Financeiro resultado = financeiroService.createOrUpdate(1L, dto);

        assertThat(resultado.getProfissao()).isEqualTo("Dev");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveAtualizarFinanceiroExistente() {
        Financeiro existente = Financeiro.builder().id(1L).profissao("Antigo").build();
        Usuario usuario = Usuario.builder().id(1L).build();
        usuario.setFinanceiro(existente);
        FinanceiroUpdateDTO dto = new FinanceiroUpdateDTO("Novo", BigDecimal.valueOf(8000), "PJ", "Aposentar", BigDecimal.valueOf(2000));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        financeiroService.createOrUpdate(1L, dto);

        verify(usuarioFinanceiroMapper).updateEntity(existente, dto);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveLancarExcecaoAoCriarParaUsuarioInexistente() {
        FinanceiroUpdateDTO dto = new FinanceiroUpdateDTO();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> financeiroService.createOrUpdate(99L, dto));
    }
}
