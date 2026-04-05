# 🔍 Prompt — Análise Completa do Projeto Financas Spring

> Cole este prompt no Claude e, em seguida, anexe os arquivos do projeto um a um.

---

Você é um engenheiro sênior especialista em Java, Spring Boot 3 e arquitetura de sistemas. Vou te fornecer o código-fonte do meu projeto **Financas Spring** — uma API REST + bot do Telegram com IA generativa, RabbitMQ, PostgreSQL e pgvector.

Preciso de uma análise **completa, crítica e construtiva** cobrindo as seguintes dimensões:

---

## 1. Problemas de Código

- Bugs em potencial (race conditions, NPEs, vazamentos de recursos, falhas silenciosas)
- Uso incorreto de anotações Spring (`@Transactional`, `@Async`, escopos de bean)
- Tratamento de exceções inadequado ou genérico demais
- Problemas com injeção de dependência (uso de `@Autowired` em campo, dependências circulares)
- Código duplicado ou lógica de negócio vazando para camadas erradas

---

## 2. Problemas Estruturais / Arquiteturais

- Violações de separação de responsabilidades (Controller fazendo lógica de negócio, Service acessando HTTP diretamente etc.)
- Camadas mal definidas entre `bot/`, `services/`, `rest/controllers/`
- Uso correto do padrão DTO (entidades JPA sendo expostas diretamente na API?)
- Configuração do RabbitMQ: consumidores thread-safe? Dead Letter Queue configurada?
- Integração com Gemini/Ollama: há fallback robusto? Timeouts configurados?
- Segurança JWT: filtros na ordem correta? Token sendo validado corretamente em todas as rotas?

---

## 3. Configuração e Properties

- Segredos hardcoded ou mal protegidos no `application-local.properties`
- Propriedades sem valor padrão seguro (ex: `JWT_SECRET` fraco em dev vazando para prod?)
- `spring.jpa.hibernate.ddl-auto=update` em uso — isso é seguro para o ambiente atual?
- System prompt externalizado nas properties — há risco de injeção de prompt?

---

## 4. Performance e Escalabilidade

- Consultas N+1 no JPA (uso de `@OneToMany` sem `fetch = LAZY` + paginação)
- Ausência de cache onde seria benéfico (cotações de moeda, por exemplo)
- Uso de `pgvector` — as queries de similaridade estão indexadas?
- O consumidor do RabbitMQ está configurado com `concurrency` adequada?

---

## 5. Melhorias e Boas Práticas

- Sugestões para adotar `@ConfigurationProperties` em vez de `@Value` espalhados
- Uso de `record` do Java 17+ para DTOs imutáveis
- Testes: quais camadas estão sem cobertura? Sugestão de estratégia com Testcontainers
- Logging: MDC para correlacionar logs por `chatId` do Telegram?
- Tratamento de idempotência nas mensagens do RabbitMQ (mensagem duplicada registra gasto duas vezes?)

---

## Formato esperado da resposta

Para cada problema encontrado, use a estrutura abaixo:

```
🔴 CRÍTICO / 🟡 ATENÇÃO / 🟢 MELHORIA

Arquivo: caminho/do/arquivo
Problema: descrição clara do problema
Impacto: o que pode acontecer se não corrigir
Solução: código ou passos concretos para resolver
```

Ao final, forneça um **resumo priorizado** com os 5 itens mais urgentes para corrigir primeiro.

---

## Instruções de uso

Vou colar o código dos arquivos relevantes a seguir. Comece a análise **somente após receber todos os arquivos** e confirmar que entendeu o contexto completo.

Ordem recomendada para colar os arquivos:

1. `AiAssistantService`
2. `FinanceiroBot`
3. `TelegramMessageConsumer`
4. Entidades JPA (`Usuario`, `Gasto`, `Economia` etc.)
5. Configurações de segurança (JWT Filter, Security Config)
6. Configuração do RabbitMQ
7. REST Controllers
8. Demais services e repositórios