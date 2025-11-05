package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "usuario_detalhes", indexes = {
        @Index(name = "idx_usuario_detalhes_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_usuario_detalhes_cpf", columnList = "cpf")
})
public class Detalhes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dataNascimento;
    private String genero;
    private String telefone;

    @Column(length = 14, unique = true)
    private String cpf;

    @Column(length = 9)
    private String cep;

    @Column(length = 255)
    private String endereco;

    @Column(length = 10)
    private String numero;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
}