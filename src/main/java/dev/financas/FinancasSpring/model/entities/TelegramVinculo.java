package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "telegram_vinculos", indexes = {
    @Index(name = "idx_telegram_chat_id", columnList = "chat_id", unique = true)
})
public class TelegramVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private String chatId;

    @Column(name = "pushname", length = 100)
    private String pushname;

    @Column(name = "vinculado_em", nullable = false)
    private LocalDateTime vinculadoEm;

    @Column(name = "ultima_atividade")
    private LocalDateTime ultimaAtividade;

    @Column(name = "codigo_vinculo", length = 20)
    private String codigoVinculo;

    @Column(name = "codigo_expira")
    private LocalDateTime codigoExpira;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @PostPersist
    public void logNew() {
        log.info("[Entity] Novo vínculo criado: chatId={}, pushname={}", chatId, pushname);
    }

    @PostUpdate
    public void logUpdate() {
        log.debug("[Entity] Vínculo atualizado: chatId={}, usuario={}", chatId, (usuario != null ? usuario.getEmail() : "null"));
    }
}
