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
@Table(name = "economias", indexes = {
    @Index(name = "idx_economias_telegram_vinculo", columnList = "telegram_vinculo_id"),
    @Index(name = "idx_economias_data", columnList = "data_economia")
})
public class Economia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 255)
    private String descricao;

    @Column(name = "data_economia", nullable = false)
    private LocalDate dataEconomia;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_vinculo_id", nullable = false)
    private TelegramVinculo telegramVinculo;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.dataEconomia == null) {
            this.dataEconomia = LocalDate.now();
        }
    }
}
