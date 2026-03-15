package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.BusinessException;
import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.UsuarioUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.UsuarioMapper;
import dev.financas.FinancasSpring.model.entities.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
// import java.util.Optional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional(readOnly = true)
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + id));
    }

    public Usuario findByEmailComTudo(String email) {
        return usuarioRepository.findWithAllRelationsByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o e-mail: " + email));
    }

    @Transactional
    public Usuario save(Usuario usuario) {
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new BusinessException("O e-mail informado já está em uso.");
        }
        usuario.setCriadoPor(usuario.getEmail());
        usuario.setAtualizadoPor(usuario.getEmail());

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = this.findById(id);

        usuarioMapper.updateEntity(usuario, dto);
        return usuarioRepository.save(usuario);
    }

}
