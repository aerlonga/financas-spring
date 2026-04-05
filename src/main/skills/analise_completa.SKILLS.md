# 🔍 Análise Completa — Financas Spring

Análise abrangente do projeto cobrindo as 5 dimensões das Skills: problemas de código, arquitetura, configuração, performance e boas práticas.

---

## 1. Problemas de Código

### 🔴 CRÍTICO

**Arquivo:** [application-local.properties](file:///home/aerlon/financas-spring/src/main/resources/application-local.properties#L7)
**Problema:** `JWT_SECRET` hardcoded no arquivo de propriedades: `local_secret_key_1234567890_abcdef_123456`
**Impacto:** Se o [.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties) for commitado no Git, o secret fica exposto. Em produção, se a variável de ambiente `${JWT_SECRET}` não for definida, o fallback pode vazar.
**Solução:** Mova o `JWT_SECRET` totalmente para variáveis de ambiente usando a sintaxe `JWT_SECRET=${JWT_SECRET}` (como já está em [application-prod.properties](file:///home/aerlon/financas-spring/src/main/resources/application-prod.properties)). Nunca tenha valores reais de segredos em arquivos versionados — use um `.env` ou vault.

---

### 🔴 CRÍTICO

**Arquivo:** [FinanceiroBot.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java#L233-L238)
**Problema:** O bot faz chamada HTTP para si mesmo via `localhost` para vincular conta (`http://localhost:` + serverPort + `/api/telegram/confirmar`). Além de ser um anti-pattern (self-referencing via HTTP), isso quebra em containers Docker onde o serviço pode não estar acessível via `localhost`.
**Impacto:** Falha silenciosa em produção (Docker), latência desnecessária, e acoplamento frágil.
**Solução:** Injetar `TelegramVinculoController` ou um service diretamente no bot em vez de fazer chamada HTTP:
```java
// Em vez de chamar via HTTP:
vinculoService.confirmarVinculo(chatId, codigo);
```

---

### 🔴 CRÍTICO

**Arquivo:** [MessageQueueService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/MessageQueueService.java#L33)
**Problema:** `ScheduledExecutorService` criado com `Executors.newScheduledThreadPool(2)` nunca é desligado (`shutdown()`). Isso causa **vazamento de threads** — o pool persiste mesmo se o Spring context for reiniciado.
**Impacto:** Vazamento de recursos em ogni restart, acumulando threads zumbis.
**Solução:** Adicionar `@PreDestroy` para desligar o pool:
```java
@PreDestroy
public void destroy() {
    typingScheduler.shutdownNow();
}
```
Ou melhor: usar o `TaskScheduler` do Spring que é gerenciado pelo container.

---

### 🔴 CRÍTICO

**Arquivo:** [BotSessionManager.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/BotSessionManager.java)
**Problema:** Senhas são armazenadas como `String` em memória no `ConcurrentHashMap` (via [setDado(chatId, "email/senha", ...)](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/BotSessionManager.java#38-42)). Nunca há limpeza explícita das senhas após uso. Com múltiplas instâncias ou restarts, os dados ficam em memória indefinidamente.
**Impacto:** Senhas ficam em memória em texto plano. Em caso de heap dump, são expostas. Sem TTL, sessões abandonadas acumulam indefinidamente → **memory leak**.
**Solução:** 
1. Adicionar um TTL com `ScheduledExecutorService` que limpe sessões antigas (ex: 10 min).
2. Zerar a referência do campo senha (`sessionManager.setDado(chatId, "senha", null)`) logo após autenticação.

---

### 🟡 ATENÇÃO

**Arquivo:** [FinanceiroBot.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java#L88-L90)
**Problema:** `PhotoSize::getFileSize` pode retornar `null` em certas condições da API do Telegram, causando `NullPointerException` no `.max()`.
**Impacto:** Crash no processamento de fotos enviadas pelo usuário.
**Solução:** Usar comparação segura:
```java
PhotoSize fotoMaior = fotos.stream()
    .max(Comparator.comparingInt(p -> p.getFileSize() != null ? p.getFileSize() : 0))
    .orElseThrow();
```

---

### 🟡 ATENÇÃO

**Arquivo:** [RabbitMQConfig.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/RabbitMQConfig.java#L99)
**Problema:** O comentário diz "Acknowledge manual" mas o `AcknowledgeMode` está em `AUTO`. Com `AUTO`, a mensagem é confirmada assim que chega ao listener, **antes** do processamento ser concluído. Se der erro durante o [processarMensagem()](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/TelegramMessageConsumer.java#44-65), a mensagem é perdida.
**Impacto:** Perda de mensagens em caso de falha no processamento (ex: timeout da API Gemini). Gastos podem ser registrados pela metade ou não registrados.
**Solução:** Usar `AcknowledgeMode.MANUAL` para controlar explicitamente quando confirmar a mensagem:
```java
factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
```
E ajustar o consumer para fazer `channel.basicAck()` após sucesso.

---

### 🟡 ATENÇÃO

**Arquivo:** [BotMemoriaService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#L235)
**Problema:** `@Transactional` em método de uma `record` que implementa uma interface. A anotação não funciona em métodos de classes internas/records because Spring AOP cria proxies baseados na classe, e `record` não pode ser proxied.
**Impacto:** Os saves de memória podem não rodar dentro de uma transação, permitindo dados inconsistentes.
**Solução:** Mover a lógica de persistência para o [BotMemoriaService](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#21-273) (que é um bean gerenciado pelo Spring e pode ser proxied) ou usar `TransactionTemplate`.

---

### 🟡 ATENÇÃO

**Arquivo:** [TelegramMediaService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/TelegramMediaService.java)
**Problema:** Todas as chamadas HTTP (Telegram API e Gemini API) usam `.block()` sem timeout. Se a API remota travar, a thread fica presa indefinidamente.
**Impacto:** Com 3-5 consumers concorrentes, basta 3-5 requisições travadas para bloquear todo o sistema.
**Solução:** Adicionar `.timeout(Duration.ofSeconds(30))` antes de cada `.block()`:
```java
.bodyToMono(String.class)
.timeout(Duration.ofSeconds(30))
.block();
```

---

## 2. Problemas Estruturais / Arquiteturais

### 🟡 ATENÇÃO

**Arquivo:** [UsuarioController.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/rest/controllers/UsuarioController.java#L80-L84)
**Problema:** Todos os `@PreAuthorize` estão comentados (`// @PreAuthorize`). Qualquer usuário autenticado pode acessar dados de QUALQUER outro usuário: GET `/usuarios/{id}`, PUT `/usuarios/{id}`, detalhes, financeiro e preferências.
**Impacto:** **Broken Access Control** — usuário A pode consultar e alterar dados do usuário B. Classificação OWASP #1.
**Solução:** Descomentar e ativar os `@PreAuthorize`:
```java
@PreAuthorize("@securityUtil.isOwnerOrAdmin(#id)")
```

---

### 🟡 ATENÇÃO

**Arquivo:** [FinanceiroBot.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java)
**Problema:** O [FinanceiroBot](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java#23-358) tem 358 linhas e acumula responsabilidades: recebe updates, envia respostas, monta mensagem de help, processa autenticação (login/cadastro com 7 estados), e faz chamadas HTTP de vinculação. Viola Single Responsibility Principle.
**Impacto:** Dificuldade de manutenção, testes acoplados, e alta chance de bugs ao modificar uma funcionalidade afetando outra.
**Solução:** Extrair em:
- `AuthenticationFlowHandler` (fluxo de login/cadastro)
- `BotResponseSender` (envio de mensagens com split)
- Manter [FinanceiroBot](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java#23-358) apenas como dispatcher de updates

---

### 🟢 MELHORIA

**Arquivo:** [SecurityConfig.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/SecurityConfig.java)
**Problema:** Sem configuração de CORS. O frontend precisa de headers CORS para funcionar com o backend.
**Impacto:** Requisições de frontend em outro domínio/porta serão bloqueadas pelo browser.
**Solução:** Adicionar configuração de CORS:
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

---

### 🟢 MELHORIA

**Arquivo:** Pacote `rest/controllers/`
**Problema:** A entidade [Usuario](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/model/entities/Usuario.java#18-174) implementa `UserDetails` diretamente, misturando lógica de segurança com lógica de domínio. Embora os DTOs existam e sejam bem utilizados para a API REST, a entidade JPA carrega responsabilidades de security.
**Impacto:** Acoplamento entre a camada de domínio e Spring Security.
**Solução:** Criar uma classe `CustomUserDetails` que delega para [Usuario](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/model/entities/Usuario.java#18-174), separando as responsabilidades.

---

## 3. Configuração e Properties

### 🔴 CRÍTICO

**Arquivo:** [application-local.properties](file:///home/aerlon/financas-spring/src/main/resources/application-local.properties#L5)
**Problema:** `spring.jpa.hibernate.ddl-auto=update` em local. O perfil local é ativado por padrão em [application.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties). Se acidentalmente usar em produção sem mudar o profile, o Hibernate pode criar/alterar tabelas.
**Impacto:** O [application.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties) define `ddl-auto=validate` (correto), mas logo abaixo força `spring.profiles.active=local`, que sobrescreve com [update](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#234-266). **Em produção, basta esquecer de definir o profile pra `prod` e o Hibernate vai alterar o schema.**
**Solução:** 
1. Remover `spring.profiles.active=local` do [application.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties) base — profiles devem ser ativados externamente (variável de ambiente `SPRING_PROFILES_ACTIVE`).
2. Considerar usar Flyway exclusivamente para migrações (já está configurado!), e deixar `ddl-auto=none` em todos os profiles.

---

### 🟡 ATENÇÃO

**Arquivo:** [application-prod.properties](file:///home/aerlon/financas-spring/src/main/resources/application-prod.properties)
**Problema:** O [application-prod.properties](file:///home/aerlon/financas-spring/src/main/resources/application-prod.properties) não define propriedades cruciais: `bot.telegram.token`, `bot.gemini.api-key`, `bot.ai.system-prompt`, `spring.rabbitmq.*`, etc. Essas propriedades só existem no profile `local`. Se produção não tiver essas variáveis, o app falha sem mensagem clara.
**Impacto:** Deploy em produção falha em tempo de startup com erros genéricos de binding.
**Solução:** Garantir que [application-prod.properties](file:///home/aerlon/financas-spring/src/main/resources/application-prod.properties) tenha pelo menos referências a variáveis de ambiente para todas as propriedades necessárias, ou consolidar em [application.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties) com defaults seguros.

---

### 🟡 ATENÇÃO

**Arquivo:** [application-local.properties](file:///home/aerlon/financas-spring/src/main/resources/application-local.properties#L36-L47)
**Problema:** O system prompt está inteiramente no [application-local.properties](file:///home/aerlon/financas-spring/src/main/resources/application-local.properties). Ele contém instruções detalhadas da IA incluindo restrições de formatação. Embora externalizar o prompt seja bom, com um `@Value` simples não há validação de conteúdo.
**Impacto:** Risco baixo de injeção de prompt (o prompt vem de config interna, não do usuário). Porém, editar um prompt tão longo em uma única linha de [.properties](file:///home/aerlon/financas-spring/src/main/resources/application.properties) é propenso a erros.
**Solução:** Mover o system prompt para um arquivo separado (ex: `classpath:prompts/system-prompt.txt`) e carregar com `@Value("classpath:prompts/system-prompt.txt")` ou `ResourceLoader`.

---

## 4. Performance e Escalabilidade

### 🔴 CRÍTICO

**Arquivo:** [AiAssistantService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/AiAssistantService.java#L78-L80)
**Problema:** **Toda mensagem com mais de 5 caracteres** é embeddada e armazenada no `pgvector`. Isso significa que "oi tudo bem?" gera 1 embedding, "quanto gastei hoje?" outro embedding, etc. Sem limpeza ou TTL, o embedding store cresce indefinidamente.
**Impacto:** Custo crescente de API do Gemini (embedding), storage do pgvector cresce sem controle, e queries de similaridade ficam mais lentas.
**Solução:** 
1. Aumentar o threshold (ex: 20+ chars) ou filtrar apenas mensagens com conteúdo financeiro relevante
2. Implementar TTL/limpeza de embeddings antigos
3. Verificar se existe índice HNSW/IVFFlat na tabela `bot_memorias_vetoriais`

---

### 🟡 ATENÇÃO

**Arquivo:** [BotMemoriaService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#L113)
**Problema:** `findTop20ByChatIdOrderByCriadoEmDesc` é chamada **duas vezes** a cada interação: uma em [getMessages()](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#111-131) e outra em [updateMessages()](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#234-266) para checagem de duplicidade. São 2 queries ao banco por mensagem.
**Impacto:** Dobro da carga no banco por mensagem processada. Com 5 consumers concorrentes, são 10 queries/mensagem.
**Solução:** Cachear o resultado da query em memória durante o processamento da mensagem, ou usar uma flag boolean para controlar se o save é necessário.

---

### 🟡 ATENÇÃO

**Arquivo:** [PesquisaWebTools.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/tools/PesquisaWebTools.java#L33-L61)
**Problema:** Cotações de moeda são buscadas em tempo real a cada consulta sem nenhum cache. Se 10 usuários perguntarem "quanto está o dólar?" em 1 minuto, são 10 chamadas à API externa.
**Impacto:** Rate limiting da AwesomeAPI, latência desnecessária. Cotações mudam a cada minuto no máximo.
**Solução:** Adicionar `@Cacheable` do Spring com TTL de 5 minutos:
```java
@Cacheable(value = "cotacoes", key = "#moeda", unless = "#result.startsWith('Erro')")
@Tool("Busca cotação atual...")
public String consultarCotacao(String moeda) { ... }
```
Requer adicionar `spring-boot-starter-cache` e `@EnableCaching`.

---

### 🟢 MELHORIA

**Arquivo:** [RabbitMQConfig.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/RabbitMQConfig.java#L96-L97)
**Problema:** `concurrentConsumers=3` e `maxConcurrentConsumers=5` são valores fixos. Não há configuração via properties para ajustar sem recompilar.
**Impacto:** Não pode escalar o número de consumers sem rebuild.
**Solução:** Externalizar para properties:
```properties
app.rabbitmq.consumers.min=3
app.rabbitmq.consumers.max=5
```

---

## 5. Melhorias e Boas Práticas

### 🟡 ATENÇÃO

**Arquivo:** Todo o projeto
**Problema:** Apenas 6 classes de teste: `AiTest`, `FinancasSpringApplicationTests`, `TelegramMessageConsumerTest`, `UsuarioControllerTest`, `MessageQueueServiceTest`, `UsuarioServiceTest`. Camadas sem cobertura: Security, Tools, GastoService, EconomiaService, OrcamentoService, BotMemoriaService, TelegramMediaService, e FailoverChatModel.
**Impacto:** Mudanças em serviços críticos (como [GastoService](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/GastoService.java#15-129) que registra gastos) não têm rede de segurança. Bugs regressivos passam despercebidos.
**Solução:** Priorizar testes para:
1. [GastoService](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/GastoService.java#15-129) (lógica de negócio principal)
2. [FailoverChatModel](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/FailoverChatModel.java#17-85) (failover é crítico)
3. [FinanceiroTools](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/tools/FinanceiroTools.java#19-188) (interpretação de dados do usuário)
4. Usar **Testcontainers** para testar com PostgreSQL + pgvector reais

---

### 🟡 ATENÇÃO

**Arquivo:** [TelegramMessageConsumer.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/TelegramMessageConsumer.java)
**Problema:** **Falta de idempotência** no processamento de mensagens. Se o RabbitMQ reentregar uma mensagem (retry), o gasto pode ser registrado **duas vezes**. Não há verificação de `messageId` ou `timestamp` para detectar duplicatas.
**Impacto:** Gastos duplicados no banco de dados do usuário.
**Solução:** 
1. Adicionar `messageId` ao [TelegramMessageDTO](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/model/dto/TelegramMessageDTO.java#15-77) (UUID gerado no publish)
2. Usar tabela de `processed_messages` ou verificar por `chatId + timestamp` antes de registrar

---

### 🟢 MELHORIA

**Arquivo:** Vários services
**Problema:** Uso extensivo de `@Value` espalhados por diversas classes ([AiConfig](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/AiConfig.java#19-133), [PesquisaWebTools](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/tools/PesquisaWebTools.java#14-95), [TelegramMediaService](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/TelegramMediaService.java#20-206), [BotMemoriaService](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/BotMemoriaService.java#21-273), [FinanceiroBot](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java#23-358)). Dificulta a visão geral das configurações e testes.
**Impacto:** Dificuldade de manutenção e testes unitários.
**Solução:** Adotar `@ConfigurationProperties`:
```java
@ConfigurationProperties(prefix = "bot.gemini")
public record GeminiProperties(String apiKey, String model, int maxTokens, double temperature) {}
```

---

### 🟢 MELHORIA

**Arquivo:** Todo o projeto
**Problema:** Sem MDC (Mapped Diagnostic Context) para correlacionar logs por `chatId`. Quando múltiplos chats são processados em paralelo, é difícil rastrear o fluxo de uma conversa específica nos logs.
**Impacto:** Debugging em produção se torna caótico com logs intercalados de diferentes chats.
**Solução:**
```java
MDC.put("chatId", chatId);
try { /* ... */ } finally { MDC.remove("chatId"); }
```
E no [logback-spring.xml](file:///home/aerlon/financas-spring/src/main/resources/logback-spring.xml), incluir `%X{chatId}` no pattern.

---

### 🟢 MELHORIA

**Arquivo:** [AiConfig.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/AiConfig.java)
**Problema:** Código comentado de Groq (20+ linhas comentadas). Polui a leitura do código.
**Impacto:** Confusão sobre o que está ativo e o que é legado.
**Solução:** Remover código comentado. Use o histórico do Git para referência futura.

---

### 🟢 MELHORIA

**Arquivo:** [FailoverChatModel.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/FailoverChatModel.java)
**Problema:** Três métodos [generate()](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/FailoverChatModel.java#47-65) com código idêntico de failover (try-catch repetido 3x). Viola DRY.
**Impacto:** Bug em um método pode não ser corrigido nos outros.
**Solução:** Extrair lógica de failover em método utilitário:
```java
private <T> T withFailover(Supplier<T> primary, Supplier<T> backup, String label) {
    try { return primary.get(); }
    catch (Exception e) {
        log.warn("[AI-Failover] {} falhou. Tentando backup...", label);
        return backup.get();
    }
}
```

---

## 📋 Resumo Priorizado — Top 5 Ações Urgentes

| # | Severidade | Item | Arquivo |
|---|------------|------|---------|
| 1 | 🔴 CRÍTICO | **Habilitar `@PreAuthorize`** — qualquer usuário acessa dados de outros | [UsuarioController.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/rest/controllers/UsuarioController.java) |
| 2 | 🔴 CRÍTICO | **Adicionar timeouts** nas chamadas HTTP bloqueantes (`WebClient.block()`) | [TelegramMediaService.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/services/TelegramMediaService.java), [PesquisaWebTools.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/tools/PesquisaWebTools.java) |
| 3 | 🔴 CRÍTICO | **Corrigir `AcknowledgeMode`** — mensagens RabbitMQ são perdidas em caso de erro | [RabbitMQConfig.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/configuration/RabbitMQConfig.java) |
| 4 | 🔴 CRÍTICO | **Remover self-call HTTP no FinanceiroBot** — substituir por chamada direta ao service | [FinanceiroBot.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/FinanceiroBot.java) |
| 5 | 🟡 ATENÇÃO | **Implementar idempotência** no consumer para evitar gastos duplicados | [TelegramMessageConsumer.java](file:///home/aerlon/financas-spring/src/main/java/dev/financas/FinancasSpring/bot/TelegramMessageConsumer.java) |

---

> [!TIP]
> O projeto tem uma boa base arquitetural: separação em camadas, uso de DTOs/Mappers, DLQ no RabbitMQ, Flyway para migrações, failover de IA, e ferramentas bem desenhadas para o LangChain4j. Os problemas encontrados são de **maturidade de produção**, não de design fundamental.
