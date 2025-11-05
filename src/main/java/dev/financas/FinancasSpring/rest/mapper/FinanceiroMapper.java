package dev.financas.FinancasSpring.rest.mapper;

import dev.financas.FinancasSpring.model.entities.Financeiro;
import dev.financas.FinancasSpring.rest.dto.FinanceiroCreateDTO;
import dev.financas.FinancasSpring.rest.dto.FinanceiroResponseDTO;
import dev.financas.FinancasSpring.rest.dto.FinanceiroUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class FinanceiroMapper {

    public Financeiro toEntity(FinanceiroCreateDTO dto) {
        if (dto == null)
            return null;

        return Financeiro.builder()
                .profissao(dto.getProfissao())
                .rendaMensal(dto.getRendaMensal())
                .tipoRenda(dto.getTipoRenda())
                .objetivoFinanceiro(dto.getObjetivoFinanceiro())
                .metaPoupancaMensal(dto.getMetaPoupancaMensal())
                .build();
    }

    public Financeiro toEntity(FinanceiroUpdateDTO dto) {
        if (dto == null)
            return null;

        return Financeiro.builder()
                .profissao(dto.getProfissao())
                .rendaMensal(dto.getRendaMensal())
                .tipoRenda(dto.getTipoRenda())
                .objetivoFinanceiro(dto.getObjetivoFinanceiro())
                .metaPoupancaMensal(dto.getMetaPoupancaMensal())
                .build();
    }

    public FinanceiroResponseDTO toResponseDTO(Financeiro entity) {
        if (entity == null)
            return null;

        return FinanceiroResponseDTO.builder()
                .id(entity.getId())
                .profissao(entity.getProfissao())
                .rendaMensal(entity.getRendaMensal())
                .tipoRenda(entity.getTipoRenda())
                .objetivoFinanceiro(entity.getObjetivoFinanceiro())
                .metaPoupancaMensal(entity.getMetaPoupancaMensal())
                .build();
    }

    public void updateEntity(Financeiro entity, FinanceiroUpdateDTO dto) {
        if (dto == null || entity == null)
            return;

        if (dto.getProfissao() != null)
            entity.setProfissao(dto.getProfissao());
        if (dto.getRendaMensal() != null)
            entity.setRendaMensal(dto.getRendaMensal());
        if (dto.getTipoRenda() != null)
            entity.setTipoRenda(dto.getTipoRenda());
        if (dto.getObjetivoFinanceiro() != null)
            entity.setObjetivoFinanceiro(dto.getObjetivoFinanceiro());
        if (dto.getMetaPoupancaMensal() != null)
            entity.setMetaPoupancaMensal(dto.getMetaPoupancaMensal());
    }
}