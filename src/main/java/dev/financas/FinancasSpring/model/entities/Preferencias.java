package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuario_preferencias")
public class Preferencias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TemaInterface temaInterface = TemaInterface.CLARO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean notificacoesAtivadas = true;

    @Builder.Default
    @Column(length = 10)
    private String moedaPreferida = "BRL";

    @Column(length = 500)
    private String avatarUrl;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public enum TemaInterface {
        CLARO, ESCURO
    }
}