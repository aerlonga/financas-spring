package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.exceptions.ResourceNotFoundException;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.model.entities.Preferencias;
import dev.financas.FinancasSpring.model.repository.PreferenciasRepository;
import dev.financas.FinancasSpring.model.repository.UsuarioRepository;
import dev.financas.FinancasSpring.rest.dto.PreferenciasUpdateDTO;
import dev.financas.FinancasSpring.rest.mapper.PreferenciasMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenciasService {

    private final UsuarioRepository usuarioRepository;
    private final PreferenciasRepository usuarioPreferenciasRepository;
    private final PreferenciasMapper usuarioPreferenciasMapper;

    public PreferenciasService(UsuarioRepository usuarioRepository,
            PreferenciasRepository usuarioPreferenciasRepository,
            PreferenciasMapper usuarioPreferenciasMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioPreferenciasRepository = usuarioPreferenciasRepository;
        this.usuarioPreferenciasMapper = usuarioPreferenciasMapper;
    }

    @Transactional(readOnly = true)
    public Preferencias findByUsuarioId(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário não encontrado com o ID: " + usuarioId);
        }
        return usuarioPreferenciasRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Preferências não encontradas para o usuário com ID: " + usuarioId));
    }

    @Transactional
    public Preferencias createOrUpdate(Long usuarioId, PreferenciasUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o ID: " + usuarioId));

        Preferencias preferencias = usuario.getPreferencias();

        if (preferencias == null) {
            preferencias = usuarioPreferenciasMapper.toEntity(dto);
            preferencias.setUsuario(usuario);
            usuario.setPreferencias(preferencias);
        } else {
            usuarioPreferenciasMapper.updateEntity(preferencias, dto);
        }

        usuarioRepository.save(usuario);

        return preferencias;
    }
}