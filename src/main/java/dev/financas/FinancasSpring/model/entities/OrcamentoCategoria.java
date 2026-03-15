package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orcamentos_categoria", indexes = {
    @Index(name = "idx_orc_vinculo_categoria", columnList = "telegram_vinculo_id, categoria", unique = true)
})
public class OrcamentoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gasto.CategoriaGasto categoria;

    @Column(name = "valor_limite", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorLimite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_vinculo_id", nullable = false)
    private TelegramVinculo telegramVinculo;
}
