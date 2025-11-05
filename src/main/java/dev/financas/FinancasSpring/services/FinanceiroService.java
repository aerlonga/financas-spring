package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.entities.Financeiro;
import dev.financas.FinancasSpring.model.repository.FinanceiroRepository;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.FinanceiroUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.FinanceiroMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceiroService {

    private final UsuarioRepository usuarioRepository;
    private final FinanceiroRepository usuarioFinanceiroRepository;
    private final FinanceiroMapper usuarioFinanceiroMapper;

    public FinanceiroService(UsuarioRepository usuarioRepository,
            FinanceiroRepository usuarioFinanceiroRepository,
            FinanceiroMapper usuarioFinanceiroMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioFinanceiroRepository = usuarioFinanceiroRepository;
        this.usuarioFinanceiroMapper = usuarioFinanceiroMapper;
    }

    @Transactional(readOnly = true)
    public Financeiro findByUsuarioId(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + usuarioId);
        }
        return usuarioFinanceiroRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dados financeiros não encontrados para o usuário com ID: " + usuarioId));
    }

    @Transactional
    public Financeiro createOrUpdate(Long usuarioId, FinanceiroUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + usuarioId));

        Financeiro financeiro = usuario.getFinanceiro();

        if (financeiro == null) {
            financeiro = usuarioFinanceiroMapper.toEntity(dto);
            financeiro.setUsuario(usuario);
            usuario.setFinanceiro(financeiro);
        } else {
            usuarioFinanceiroMapper.updateEntity(financeiro, dto);
        }

        usuarioRepository.save(usuario);

        return financeiro;
    }
}