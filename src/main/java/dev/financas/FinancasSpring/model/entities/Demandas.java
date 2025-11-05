package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "demandas", indexes = {
        @Index(name = "idx_demandas_usuario_id", columnList = "usuario_id")
})
public class Demandas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String prioridades;

    @Column(length = 50)
    private String quereres;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
