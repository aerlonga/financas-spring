package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuario_financeiro")
public class Financeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String profissao;

    @Column(precision = 10, scale = 2)
    private BigDecimal rendaMensal;

    @Column(length = 50)
    private String tipoRenda;

    @Column(length = 255)
    private String objetivoFinanceiro;

    @Column(precision = 10, scale = 2)
    private BigDecimal metaPoupancaMensal;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}