package dev.financas.FinancasSpring.rest.mapper;

import dev.financas.FinancasSpring.model.entities.Preferencias;
import dev.financas.FinancasSpring.rest.dto.PreferenciasCreateDTO;
import dev.financas.FinancasSpring.rest.dto.PreferenciasResponseDTO;
import dev.financas.FinancasSpring.rest.dto.PreferenciasUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class PreferenciasMapper {

    public Preferencias toEntity(PreferenciasCreateDTO dto) {
        if (dto == null)
            return null;

        Preferencias entity = new Preferencias();
        updateEntity(entity, dto);
        return entity;
    }

    public Preferencias toEntity(PreferenciasUpdateDTO dto) {
        if (dto == null)
            return null;

        Preferencias entity = new Preferencias();
        updateEntity(entity, dto);
        return entity;
    }

    public PreferenciasResponseDTO toResponseDTO(Preferencias entity) {
        if (entity == null)
            return null;

        return PreferenciasResponseDTO.builder()
                .id(entity.getId())
                .temaInterface(entity.getTemaInterface())
                .notificacoesAtivadas(entity.getNotificacoesAtivadas())
                .moedaPreferida(entity.getMoedaPreferida())
                .avatarUrl(entity.getAvatarUrl())
                .build();
    }

    public void updateEntity(Preferencias entity, PreferenciasUpdateDTO dto) {
        if (dto == null || entity == null)
            return;

        if (dto.getTemaInterface() != null)
            entity.setTemaInterface(dto.getTemaInterface());
        if (dto.getNotificacoesAtivadas() != null)
            entity.setNotificacoesAtivadas(dto.getNotificacoesAtivadas());
        if (dto.getMoedaPreferida() != null)
            entity.setMoedaPreferida(dto.getMoedaPreferida());
        if (dto.getAvatarUrl() != null)
            entity.setAvatarUrl(dto.getAvatarUrl());
    }

    private void updateEntity(Preferencias entity, PreferenciasCreateDTO dto) {
        if (dto.getTemaInterface() != null)
            entity.setTemaInterface(dto.getTemaInterface());
        if (dto.getNotificacoesAtivadas() != null)
            entity.setNotificacoesAtivadas(dto.getNotificacoesAtivadas());
        if (dto.getMoedaPreferida() != null)
            entity.setMoedaPreferida(dto.getMoedaPreferida());
        if (dto.getAvatarUrl() != null)
            entity.setAvatarUrl(dto.getAvatarUrl());
    }
}