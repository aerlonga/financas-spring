package dev.financas.FinancasSpring.services;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import dev.financas.FinancasSpring.rest.dto.UsuarioCreateDTO;
import dev.financas.FinancasSpring.rest.mapper.UsuarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramVinculoService {

    private final TelegramVinculoRepository repository;
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    /**
     * Busca ou cria um vínculo para o chatId recebido do Telegram.
     * Este método é o "porteiro" — toda mensagem do bot passa por aqui.
     */
    @Transactional
    public TelegramVinculo obterOuCriar(String chatId, String pushname) {
        return repository.findByChatId(chatId).map(vinculo -> {
            // Atualiza nome e atividade se já existir
            vinculo.setPushname(pushname);
            vinculo.setUltimaAtividade(LocalDateTime.now());
            return repository.save(vinculo);
        }).orElseGet(() -> {
            // Cria novo vínculo para usuário nunca visto antes
            TelegramVinculo novo = TelegramVinculo.builder()
                .chatId(chatId)
                .pushname(pushname)
                .vinculadoEm(LocalDateTime.now())
                .ultimaAtividade(LocalDateTime.now())
                .build();
            return repository.save(novo);
        });
    }

    /**
     * Autentica no Spring Security usando e-mail e senha.
     * Se der sucesso, vincula a conta do Usuario ao chatId do Telegram.
     */
    @Transactional
    public Usuario autenticarEVincular(String chatId, String email, String senha) {
        // Usa o mecanismo pronto do Spring Security (o mesmo do /api/auth/login)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, senha)
        );

        Usuario usuario = (Usuario) authentication.getPrincipal();

        // Encontra o vínculo deste chatId e associa o usuário
        TelegramVinculo vinculo = obterOuCriar(chatId, "Bot Auth");
        vinculo.setUsuario(usuario);
        repository.save(vinculo);

        log.info("[Vinculo] ChatId {} autenticado com sucesso para o usuário {}", chatId, email);
        return usuario;
    }

    /**
     * Cria um novo usuário no banco de dados e já vincula ao chatId.
     */
    @Transactional
    public Usuario cadastrarEVincular(String chatId, String pushname, String nome, String email, String senha) {
        // Monta o DTO como se fosse a requisição web
        UsuarioCreateDTO dto = UsuarioCreateDTO.builder()
                .nomeCompleto(nome)
                .email(email)
                .senha(senha)
                .build();

        // Converte pra entidade e salva pelo UsuarioService (que valida unicidade, faz hash, etc)
        Usuario novoUsuario = usuarioMapper.toEntity(dto);
        novoUsuario = usuarioService.save(novoUsuario);

        // Associa ao Telegram
        TelegramVinculo vinculo = obterOuCriar(chatId, pushname);
        vinculo.setUsuario(novoUsuario);
        repository.save(vinculo);

        log.info("[Vinculo] Novo usuário registrado e vinculado via Telegram: {} ({})", email, chatId);
        return novoUsuario;
    }

    /**
     * Desvincula uma conta do Telegram, marcando o usuario como nulo.
     */
    @Transactional
    public void desvincular(String chatId) {
        repository.findByChatId(chatId).ifPresent(vinculo -> {
            vinculo.setUsuario(null);
            repository.save(vinculo);
            log.info("[Vinculo] ChatId {} deslogado com sucesso.", chatId);
        });
    }
}
