package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "gastos", indexes = {
    @Index(name = "idx_gastos_telegram_vinculo", columnList = "telegram_vinculo_id"),
    @Index(name = "idx_gastos_data", columnList = "data_gasto"),
    @Index(name = "idx_gastos_categoria", columnList = "categoria")
})
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String estabelecimento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaGasto categoria;

    @Column(name = "data_gasto", nullable = false)
    private LocalDate dataGasto;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_vinculo_id", nullable = false)
    private TelegramVinculo telegramVinculo;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.dataGasto == null) {
            this.dataGasto = LocalDate.now();
        }
    }

    public enum CategoriaGasto {
        ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO, OUTROS
    }
}
