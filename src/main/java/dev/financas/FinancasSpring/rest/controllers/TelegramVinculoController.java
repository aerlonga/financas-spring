package dev.financas.FinancasSpring.rest.controllers;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramVinculoController {

    private final TelegramVinculoRepository repository;

    /**
     * Passo 1: Usuário logado na web clica em "Vincular Telegram".
     * O sistema gera um código temporário válido por 10 minutos.
     * A resposta diz ao usuário qual comando enviar no bot.
     *
     * GET /api/telegram/codigo
     */
    @GetMapping("/codigo")
    public ResponseEntity<Map<String, String>> gerarCodigo(
        @AuthenticationPrincipal Usuario usuario
    ) {
        // Gera código curto e legível (ex: "A3F7B2")
        String codigo = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        LocalDateTime expira = LocalDateTime.now().plusMinutes(10);

        // Busca ou cria o vínculo do usuário logado (pode ainda não ter chatId Telegram)
        // Persiste o código no vínculo existente ou cria um temporário sem chatId
        repository.findAll().stream()
                .filter(v -> usuario.equals(v.getUsuario()))
                .findFirst()
                .ifPresent(v -> {
                    v.setCodigoVinculo(codigo);
                    v.setCodigoExpira(expira);
                    repository.save(v);
                });

        // Se ainda não existe nenhum vínculo para este usuário, cria um placeholder
        boolean temVinculo = repository.findAll().stream().anyMatch(v -> usuario.equals(v.getUsuario()));
        if (!temVinculo) {
            TelegramVinculo placeholder = TelegramVinculo.builder()
                    .chatId("PENDENTE_" + usuario.getId())
                    .pushname(usuario.getNomeCompleto())
                    .vinculadoEm(LocalDateTime.now())
                    .ultimaAtividade(LocalDateTime.now())
                    .codigoVinculo(codigo)
                    .codigoExpira(expira)
                    .usuario(usuario)
                    .build();
            repository.save(placeholder);
        }

        return ResponseEntity.ok(Map.of(
            "codigo", codigo,
            "instrucao", "Envie este comando no bot do Telegram: /vincular " + codigo,
            "expiraEm", expira.toString()
        ));
    }

    /**
     * Passo 2: O bot do Telegram chama este endpoint quando o usuário envia "/vincular <codigo>".
     * O bot informa o chatId real do Telegram e o código fornecido pelo usuário.
     * Este endpoint é interno — não requer autenticação JWT (chamado pelo próprio bot).
     *
     * POST /api/telegram/confirmar
     * Body: { "chatId": "123456789", "codigo": "A3F7B2" }
     */
    @PostMapping("/confirmar")
    public ResponseEntity<String> confirmarVinculo(
        @RequestBody Map<String, String> body
    ) {
        String chatId = body.get("chatId");
        String codigo = body.get("codigo");

        if (chatId == null || codigo == null) {
            return ResponseEntity.badRequest().body("chatId e codigo são obrigatórios.");
        }

        Optional<TelegramVinculo> optVinculo = repository.findByCodigoVinculo(codigo.toUpperCase());

        if (optVinculo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TelegramVinculo vinculo = optVinculo.get();

        if (vinculo.getCodigoExpira() == null || LocalDateTime.now().isAfter(vinculo.getCodigoExpira())) {
            return ResponseEntity.badRequest().body("Código expirado. Gere um novo código na web.");
        }

        // Atualiza o chatId real do Telegram (remove o placeholder "PENDENTE_...")
        vinculo.setChatId(chatId);
        vinculo.setCodigoVinculo(null); // limpa o código após uso
        vinculo.setCodigoExpira(null);
        vinculo.setUltimaAtividade(LocalDateTime.now());
        repository.save(vinculo);

        String nomeUsuario = vinculo.getUsuario() != null
                ? vinculo.getUsuario().getNomeCompleto()
                : "usuário";

        return ResponseEntity.ok("Telegram vinculado com sucesso! Olá, " + nomeUsuario + " 🎉");
    }
}
