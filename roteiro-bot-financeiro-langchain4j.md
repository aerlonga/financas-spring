# Roteiro de Construção — Bot Financeiro com Java, Spring Boot, LangChain4j e Gemini API

> **Objetivo:** Migrar a arquitetura atual (Node.js + Python + Java) para uma única aplicação Spring Boot que orquestra o bot do Telegram, a inteligência artificial (Gemini) e todas as regras de negócio financeiro.

---

## Índice

1. [Visão Geral da Nova Arquitetura](#1-visão-geral-da-nova-arquitetura)
2. [Novas Tabelas e Entidades Necessárias](#2-novas-tabelas-e-entidades-necessárias)
3. [Dependências do Projeto (pom.xml)](#3-dependências-do-projeto-pomxml)
4. [Configuração (application.yml)](#4-configuração-applicationyml)
5. [Passo 1 — Entidades Novas e Ajustes nas Existentes](#5-passo-1--entidades-novas-e-ajustes-nas-existentes)
6. [Passo 2 — Repositórios JPA](#6-passo-2--repositórios-jpa)
7. [Passo 3 — Serviços de Negócio](#7-passo-3--serviços-de-negócio)
8. [Passo 4 — Tools do LangChain4j (@Tool)](#8-passo-4--tools-do-langchain4j-tool)
9. [Passo 5 — Memória de Conversa](#9-passo-5--memória-de-conversa)
10. [Passo 6 — Configuração do Gemini + LangChain4j](#10-passo-6--configuração-do-gemini--langchain4j)
11. [Passo 7 — Serviço Principal de IA (AiAssistant)](#11-passo-7--serviço-principal-de-ia-aiassistant)
12. [Passo 8 — Bot do Telegram](#12-passo-8--bot-do-telegram)
13. [Passo 9 — Segurança e Vinculação Telegram ↔ Usuario](#13-passo-9--segurança-e-vinculação-telegram--usuario)
14. [Passo 10 — Busca Web (pesquisa financeira)](#14-passo-10--busca-web-pesquisa-financeira)
15. [Passo 11 — System Prompt](#15-passo-11--system-prompt)
16. [Passo 12 — Limpeza de Memória (Scheduler)](#16-passo-12--limpeza-de-memória-scheduler)
17. [Estrutura Final de Pacotes](#17-estrutura-final-de-pacotes)
18. [Ordem de Execução dos Passos](#18-ordem-de-execução-dos-passos)
19. [Checklist Final](#19-checklist-final)

---

## 1. Visão Geral da Nova Arquitetura

### Antes (3 serviços separados)

```
Telegram → Node.js (BullMQ/Redis) → Python (FastAPI/Ollama) → Java (Spring Boot)
```

### Depois (1 serviço unificado)

```
Telegram → Spring Boot
               ├── LangChain4j (orquestrador de IA)
               ├── Gemini API (modelo de linguagem)
               ├── @Tool Methods (ferramentas financeiras)
               └── PostgreSQL (gastos, economias, memória, orçamentos)
```

**O que é eliminado:**
- Serviço Node.js + Redis (BullMQ)
- Serviço Python (FastAPI + Ollama)
- Modelos locais de IA (Llama, Whisper, Moondream)

**O que é mantido:**
- Spring Boot com todas as entidades existentes
- PostgreSQL
- Estrutura de `Usuario`, `Financeiro`, `Detalhes`, `Preferencias`, `Demandas`

---

## 2. Novas Tabelas e Entidades Necessárias

O sistema atual não possui tabelas para **gastos**, **economias**, **orçamentos** ou **memória de conversa do bot**. Estas precisam ser criadas.

### Diagrama das novas entidades

```
Usuario (existente)
  ├── Financeiro (existente)
  ├── Detalhes (existente)
  ├── Preferencias (existente)
  ├── Demandas (existente)
  │
  ├── [NOVO] TelegramVinculo       → vincula chatId do Telegram ao Usuario
  ├── [NOVO] Gasto                 → cada gasto registrado pelo bot
  ├── [NOVO] Economia              → cada economia/poupança registrada
  ├── [NOVO] OrcamentoCategoria    → orçamento mensal por categoria
  └── [NOVO] BotMemoria            → histórico de conversa para contexto da IA
```

---

## 3. Dependências do Projeto (pom.xml)

Adicione ao seu `pom.xml` existente:

```xml
<!-- LangChain4j - Core -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>

<!-- LangChain4j - Integração com Google Gemini -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
    <version>0.36.2</version>
</dependency>

<!-- LangChain4j - Spring Boot Starter (autoconfigure) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.36.2</version>
</dependency>

<!-- Bot do Telegram para Java -->
<dependency>
    <groupId>org.telegram</groupId>
    <artifactId>telegrambots-spring-boot-starter</artifactId>
    <version>6.9.7.1</version>
</dependency>

<!-- Cliente HTTP para pesquisa web (DuckDuckGo / AwesomeAPI) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Lombok (já deve ter, confirmar) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

> **Nota:** Verifique a versão mais recente do LangChain4j em https://github.com/langchain4j/langchain4j/releases antes de usar. A versão `0.36.x` é a estável atual no momento deste roteiro.

---

## 4. Configuração (application.yml)

```yaml
spring:
  application:
    name: financas-bot
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    hibernate:
      ddl-auto: update   # use 'validate' em produção com Flyway/Liquibase
    show-sql: false

# Configurações do Bot Financeiro
bot:
  telegram:
    token: ${TELEGRAM_TOKEN}
    username: ${TELEGRAM_BOT_USERNAME}
  gemini:
    api-key: ${GEMINI_API_KEY}
    model: gemini-2.0-flash   # ou gemini-1.5-pro para respostas mais elaboradas
    max-tokens: 1024
    temperature: 0.3           # baixo = mais preciso e menos criativo (ideal para finanças)
  memory:
    max-messages: 20           # máximo de mensagens no contexto da conversa
    ttl-days: 30               # dias para expirar memórias antigas
  currency:
    api-url: https://economia.awesomeapi.com.br/json/last
```

---

## 5. Passo 1 — Entidades Novas e Ajustes nas Existentes

### 5.1 — TelegramVinculo.java

Vincula o `chatId` do Telegram a um `Usuario` do sistema. Sem isso, o bot não sabe a qual usuário a mensagem pertence.

```java
package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    // O ID único do chat no Telegram (chatId do usuário)
    @Column(name = "chat_id", nullable = false, unique = true)
    private String chatId;

    // Nome exibido no Telegram (first_name ou username)
    @Column(name = "pushname", length = 100)
    private String pushname;

    @Column(name = "vinculado_em", nullable = false)
    private LocalDateTime vinculadoEm;

    @Column(name = "ultima_atividade")
    private LocalDateTime ultimaAtividade;

    // Relação com o Usuario do sistema principal
    // nullable = true pois o bot pode ser usado por anônimos antes do cadastro
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;
}
```

---

### 5.2 — Gasto.java

```java
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

    // Rastreabilidade: qual vínculo do Telegram gerou este gasto
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
```

---

### 5.3 — Economia.java

```java
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
```

---

### 5.4 — OrcamentoCategoria.java

```java
package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orcamentos_categoria", indexes = {
    @Index(name = "idx_orc_vinculo_categoria", columnList = "telegram_vinculo_id, categoria", unique = true)
})
public class OrcamentoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gasto.CategoriaGasto categoria;

    // Valor máximo mensal definido pelo usuário para esta categoria
    @Column(name = "valor_limite", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorLimite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_vinculo_id", nullable = false)
    private TelegramVinculo telegramVinculo;
}
```

---

### 5.5 — BotMemoria.java

Armazena o histórico de conversa para o LangChain4j reconstruir o contexto entre sessões.

```java
package dev.financas.FinancasSpring.model.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bot_memorias", indexes = {
    @Index(name = "idx_memoria_chat_id", columnList = "chat_id"),
    @Index(name = "idx_memoria_criado_em", columnList = "criado_em")
})
public class BotMemoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    // "user" ou "assistant" — papel da mensagem na conversa
    @Column(nullable = false, length = 20)
    private String role;

    // Conteúdo da mensagem (texto do usuário ou resposta da IA)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}
```

---

### 5.6 — Ajuste em Usuario.java (adicionar relação com TelegramVinculo)

No seu `Usuario.java` existente, adicione a lista de vínculos do Telegram:

```java
// Adicionar dentro da classe Usuario, junto às outras listas
@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
@JsonIgnore
@Builder.Default
private List<TelegramVinculo> telegramVinculos = new ArrayList<>();
```

---

## 6. Passo 2 — Repositórios JPA

Crie o pacote `repository` (ou adicione nos repositórios existentes):

### GastoRepository.java

```java
package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Busca gastos por vínculo e intervalo de datas
    List<Gasto> findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
        TelegramVinculo vinculo, LocalDate inicio, LocalDate fim);

    // Busca gastos por vínculo, data e categoria
    List<Gasto> findByTelegramVinculoAndDataGastoBetweenAndCategoriaOrderByDataGastoDesc(
        TelegramVinculo vinculo, LocalDate inicio, LocalDate fim, CategoriaGasto categoria);

    // Soma total de gastos em um período
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM Gasto g WHERE g.telegramVinculo = :vinculo AND g.dataGasto BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(@Param("vinculo") TelegramVinculo vinculo,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    // Soma por categoria em um período
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM Gasto g WHERE g.telegramVinculo = :vinculo AND g.dataGasto BETWEEN :inicio AND :fim AND g.categoria = :categoria")
    BigDecimal somarPorPeriodoECategoria(@Param("vinculo") TelegramVinculo vinculo,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim,
                                         @Param("categoria") CategoriaGasto categoria);

    // Top 5 mais recentes para exibição no resumo
    List<Gasto> findTop5ByTelegramVinculoOrderByCriadoEmDesc(TelegramVinculo vinculo);
}
```

---

### EconomiaRepository.java

```java
package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Economia;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface EconomiaRepository extends JpaRepository<Economia, Long> {

    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Economia e WHERE e.telegramVinculo = :vinculo AND e.dataEconomia BETWEEN :inicio AND :fim")
    BigDecimal somarPorPeriodo(@Param("vinculo") TelegramVinculo vinculo,
                               @Param("inicio") LocalDate inicio,
                               @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM Economia e WHERE e.telegramVinculo = :vinculo")
    BigDecimal somarTotal(@Param("vinculo") TelegramVinculo vinculo);
}
```

---

### OrcamentoRepository.java

```java
package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.OrcamentoCategoria;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<OrcamentoCategoria, Long> {
    List<OrcamentoCategoria> findByTelegramVinculo(TelegramVinculo vinculo);
    Optional<OrcamentoCategoria> findByTelegramVinculoAndCategoria(TelegramVinculo vinculo, CategoriaGasto categoria);
}
```

---

### TelegramVinculoRepository.java

```java
package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TelegramVinculoRepository extends JpaRepository<TelegramVinculo, Long> {
    Optional<TelegramVinculo> findByChatId(String chatId);
}
```

---

### BotMemoriaRepository.java

```java
package dev.financas.FinancasSpring.repository;

import dev.financas.FinancasSpring.model.entities.BotMemoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BotMemoriaRepository extends JpaRepository<BotMemoria, Long> {

    // Retorna as últimas N mensagens de um chat (para reconstruir contexto)
    List<BotMemoria> findTop20ByChatIdOrderByCriadoEmAsc(String chatId);

    // Para o scheduler de limpeza
    @Modifying
    @Query("DELETE FROM BotMemoria m WHERE m.criadoEm < :limite")
    void deleteBycriadoEmBefore(@Param("limite") LocalDateTime limite);
}
```

---

## 7. Passo 3 — Serviços de Negócio

Estes serviços contêm a lógica pura. As `@Tool` (Passo 4) vão chamá-los.

### TelegramVinculoService.java

```java
package dev.financas.FinancasSpring.service;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TelegramVinculoService {

    private final TelegramVinculoRepository repository;

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
}
```

---

### GastoService.java

```java
package dev.financas.FinancasSpring.service;

import dev.financas.FinancasSpring.model.entities.Gasto;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository repository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public Gasto registrar(TelegramVinculo vinculo, String estabelecimento,
                           BigDecimal valor, CategoriaGasto categoria, LocalDate data) {
        Gasto gasto = Gasto.builder()
            .telegramVinculo(vinculo)
            .estabelecimento(estabelecimento)
            .valor(valor)
            .categoria(categoria)
            .dataGasto(data != null ? data : LocalDate.now())
            .build();
        return repository.save(gasto);
    }

    public String consultarResumo(TelegramVinculo vinculo, LocalDate inicio,
                                   LocalDate fim, CategoriaGasto categoria) {
        List<Gasto> gastos;
        BigDecimal total;

        if (categoria != null) {
            gastos = repository.findByTelegramVinculoAndDataGastoBetweenAndCategoriaOrderByDataGastoDesc(
                vinculo, inicio, fim, categoria);
            total = repository.somarPorPeriodoECategoria(vinculo, inicio, fim, categoria);
        } else {
            gastos = repository.findByTelegramVinculoAndDataGastoBetweenOrderByDataGastoDesc(
                vinculo, inicio, fim);
            total = repository.somarPorPeriodo(vinculo, inicio, fim);
        }

        if (gastos.isEmpty()) {
            return "Nenhum gasto encontrado no período informado.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total gasto: R$ %.2f\n", total));
        sb.append(String.format("Registros: %d\n\n", gastos.size()));
        sb.append("Últimos lançamentos:\n");

        gastos.stream().limit(5).forEach(g ->
            sb.append(String.format("• %s: %s - R$ %.2f (%s)\n",
                g.getDataGasto().format(FMT),
                g.getEstabelecimento(),
                g.getValor(),
                g.getCategoria().name()))
        );

        if (gastos.size() > 5) {
            sb.append(String.format("...e mais %d lançamentos.", gastos.size() - 5));
        }

        return sb.toString();
    }
}
```

---

### EconomiaService.java

```java
package dev.financas.FinancasSpring.service;

import dev.financas.FinancasSpring.model.entities.Economia;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.EconomiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EconomiaService {

    private final EconomiaRepository repository;

    @Transactional
    public Economia registrar(TelegramVinculo vinculo, BigDecimal valor, String descricao) {
        Economia economia = Economia.builder()
            .telegramVinculo(vinculo)
            .valor(valor)
            .descricao(descricao)
            .dataEconomia(LocalDate.now())
            .build();
        return repository.save(economia);
    }

    public String consultarTotal(TelegramVinculo vinculo, LocalDate inicio, LocalDate fim) {
        BigDecimal total;
        String periodo;

        if (inicio == null || fim == null) {
            total = repository.somarTotal(vinculo);
            periodo = "todo o histórico";
        } else {
            total = repository.somarPorPeriodo(vinculo, inicio, fim);
            periodo = String.format("%s a %s", inicio, fim);
        }

        return String.format("Total economizado (%s): R$ %.2f", periodo, total);
    }
}
```

---

### OrcamentoService.java

```java
package dev.financas.FinancasSpring.service;

import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.OrcamentoCategoria;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.repository.GastoRepository;
import dev.financas.FinancasSpring.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final GastoRepository gastoRepository;

    public String consultarOrcamento(TelegramVinculo vinculo, CategoriaGasto categoria) {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate hoje = LocalDate.now();

        if (categoria != null) {
            Optional<OrcamentoCategoria> orc = orcamentoRepository
                .findByTelegramVinculoAndCategoria(vinculo, categoria);

            BigDecimal gasto = gastoRepository.somarPorPeriodoECategoria(
                vinculo, inicioMes, hoje, categoria);

            if (orc.isEmpty()) {
                return String.format("Você não definiu um orçamento para %s. Gasto atual: R$ %.2f",
                    categoria.name(), gasto);
            }

            BigDecimal limite = orc.get().getValorLimite();
            BigDecimal saldo = limite.subtract(gasto);

            return String.format(
                "Orçamento %s:\nLimite: R$ %.2f\nGasto: R$ %.2f\nSaldo: R$ %.2f%s",
                categoria.name(), limite, gasto, saldo,
                saldo.compareTo(BigDecimal.ZERO) < 0 ? "\n⚠️ Orçamento estourado!" : ""
            );
        }

        // Visão geral de todas as categorias
        List<OrcamentoCategoria> todos = orcamentoRepository.findByTelegramVinculo(vinculo);
        BigDecimal totalGasto = gastoRepository.somarPorPeriodo(vinculo, inicioMes, hoje);

        if (todos.isEmpty()) {
            return String.format("Nenhum orçamento configurado. Gasto total este mês: R$ %.2f", totalGasto);
        }

        BigDecimal totalLimite = todos.stream()
            .map(OrcamentoCategoria::getValorLimite)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
            "Resumo do mês:\nOrçamento total: R$ %.2f\nTotal gasto: R$ %.2f\nSaldo: R$ %.2f",
            totalLimite, totalGasto, totalLimite.subtract(totalGasto)
        );
    }

    @Transactional
    public String definirOrcamento(TelegramVinculo vinculo, CategoriaGasto categoria, BigDecimal limite) {
        OrcamentoCategoria orc = orcamentoRepository
            .findByTelegramVinculoAndCategoria(vinculo, categoria)
            .orElseGet(() -> OrcamentoCategoria.builder()
                .telegramVinculo(vinculo)
                .categoria(categoria)
                .build());

        orc.setValorLimite(limite);
        orcamentoRepository.save(orc);

        return String.format("Orçamento de R$ %.2f definido para %s.", limite, categoria.name());
    }
}
```

---

## 8. Passo 4 — Tools do LangChain4j (@Tool)

Esta é a parte central da migração. As `@Tool` substituem o seu `tools_definition` JSON e o `tool_runner.py`. O Gemini chama esses métodos automaticamente quando entende que precisa deles.

Crie uma classe por domínio dentro do pacote `bot.tools`:

### FinanceiroTools.java

```java
package dev.financas.FinancasSpring.bot.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.financas.FinancasSpring.model.entities.Gasto.CategoriaGasto;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.service.GastoService;
import dev.financas.FinancasSpring.service.EconomiaService;
import dev.financas.FinancasSpring.service.OrcamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Tools financeiras expostas ao Gemini via LangChain4j.
 *
 * IMPORTANTE: O campo chatId é injetado pelo AiAssistant antes de cada chamada.
 * Não é exposto ao Gemini — é gerenciado internamente.
 */
@Component
@RequiredArgsConstructor
public class FinanceiroTools {

    private final GastoService gastoService;
    private final EconomiaService economiaService;
    private final OrcamentoService orcamentoService;
    private final dev.financas.FinancasSpring.service.TelegramVinculoService vinculoService;

    // ThreadLocal para passar o chatId sem expô-lo ao Gemini
    private static final ThreadLocal<String> CHAT_ID_CONTEXT = new ThreadLocal<>();

    public static void setChatId(String chatId) {
        CHAT_ID_CONTEXT.set(chatId);
    }

    public static void clearChatId() {
        CHAT_ID_CONTEXT.remove();
    }

    private TelegramVinculo getVinculo() {
        String chatId = CHAT_ID_CONTEXT.get();
        if (chatId == null) throw new IllegalStateException("chatId não definido no contexto.");
        return vinculoService.obterOuCriar(chatId, "sistema");
    }

    // ──────────────────────────────────────────────
    // GASTOS
    // ──────────────────────────────────────────────

    @Tool("Registra um gasto financeiro do usuário. Use quando o usuário informar local, valor e categoria de uma despesa.")
    public String registrarGasto(
        @P("Nome do estabelecimento ou local onde foi feita a compra") String estabelecimento,
        @P("Valor numérico do gasto em reais (ex: 49.90)") double valor,
        @P("Categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS") String categoria,
        @P("Data do gasto no formato dd/MM/yyyy. Se não informado, usar data de hoje.") String data
    ) {
        try {
            TelegramVinculo vinculo = getVinculo();
            CategoriaGasto cat = CategoriaGasto.valueOf(categoria.toUpperCase());
            LocalDate dataGasto = parseData(data);
            BigDecimal valorDecimal = BigDecimal.valueOf(valor);

            gastoService.registrar(vinculo, estabelecimento, valorDecimal, cat, dataGasto);

            return String.format(
                "✅ Gasto registrado!\n• Local: %s\n• Valor: R$ %.2f\n• Categoria: %s\n• Data: %s",
                estabelecimento, valor, cat.name(),
                dataGasto.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        } catch (IllegalArgumentException e) {
            return "Categoria inválida. Use: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS.";
        } catch (Exception e) {
            return "Erro ao registrar o gasto: " + e.getMessage();
        }
    }

    @Tool("Consulta o resumo de gastos do usuário em um período. Use quando perguntar quanto gastou hoje, essa semana, esse mês ou em uma categoria específica.")
    public String consultarGastos(
        @P("Período: HOJE, ONTEM, SEMANA, MES ou TOTAL") String periodo,
        @P("Categoria específica (opcional). Ex: ALIMENTACAO") String categoria
    ) {
        TelegramVinculo vinculo = getVinculo();
        LocalDate[] datas = resolverPeriodo(periodo);
        CategoriaGasto cat = categoria != null && !categoria.isBlank()
            ? parseCategoriaSegura(categoria) : null;
        return gastoService.consultarResumo(vinculo, datas[0], datas[1], cat);
    }

    // ──────────────────────────────────────────────
    // ECONOMIAS
    // ──────────────────────────────────────────────

    @Tool("Registra um valor que o usuário guardou ou economizou.")
    public String registrarEconomia(
        @P("Valor guardado em reais") double valor,
        @P("Descrição ou motivo (ex: salário, poupança, mesada)") String descricao
    ) {
        TelegramVinculo vinculo = getVinculo();
        economiaService.registrar(vinculo, BigDecimal.valueOf(valor), descricao);
        return String.format("✅ Economia de R$ %.2f registrada! (%s)", valor, descricao);
    }

    @Tool("Consulta o total economizado pelo usuário em um período.")
    public String consultarEconomias(
        @P("Período: HOJE, SEMANA, MES ou TOTAL") String periodo
    ) {
        TelegramVinculo vinculo = getVinculo();
        if ("TOTAL".equalsIgnoreCase(periodo)) {
            return economiaService.consultarTotal(vinculo, null, null);
        }
        LocalDate[] datas = resolverPeriodo(periodo);
        return economiaService.consultarTotal(vinculo, datas[0], datas[1]);
    }

    // ──────────────────────────────────────────────
    // ORÇAMENTOS
    // ──────────────────────────────────────────────

    @Tool("Consulta o orçamento mensal e saldo disponível do usuário. Pode ser geral ou por categoria específica.")
    public String consultarOrcamento(
        @P("Nome da categoria (ex: ALIMENTACAO). Deixar vazio para visão geral.") String categoria
    ) {
        TelegramVinculo vinculo = getVinculo();
        CategoriaGasto cat = (categoria != null && !categoria.isBlank())
            ? parseCategoriaSegura(categoria) : null;
        return orcamentoService.consultarOrcamento(vinculo, cat);
    }

    @Tool("Define ou atualiza o limite de orçamento mensal para uma categoria.")
    public String definirOrcamento(
        @P("Categoria: ALIMENTACAO, TRANSPORTE, SAUDE, MORADIA, LAZER, EDUCACAO ou OUTROS") String categoria,
        @P("Valor limite mensal em reais") double limite
    ) {
        TelegramVinculo vinculo = getVinculo();
        CategoriaGasto cat = CategoriaGasto.valueOf(categoria.toUpperCase());
        return orcamentoService.definirOrcamento(vinculo, cat, BigDecimal.valueOf(limite));
    }

    // ──────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank() || data.equalsIgnoreCase("hoje")) return LocalDate.now();
        if (data.equalsIgnoreCase("ontem")) return LocalDate.now().minusDays(1);
        try {
            return LocalDate.parse(data, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate[] resolverPeriodo(String periodo) {
        LocalDate hoje = LocalDate.now();
        return switch (periodo.toUpperCase()) {
            case "HOJE"   -> new LocalDate[]{hoje, hoje};
            case "ONTEM"  -> new LocalDate[]{hoje.minusDays(1), hoje.minusDays(1)};
            case "SEMANA" -> new LocalDate[]{hoje.minusDays(7), hoje};
            case "MES"    -> new LocalDate[]{hoje.withDayOfMonth(1), hoje};
            default       -> new LocalDate[]{LocalDate.of(2000, 1, 1), hoje};
        };
    }

    private CategoriaGasto parseCategoriaSegura(String categoria) {
        try {
            return CategoriaGasto.valueOf(categoria.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
```

---

### PesquisaWebTools.java

```java
package dev.financas.FinancasSpring.bot.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

/**
 * Tool de pesquisa web: cotações via AwesomeAPI e busca genérica.
 */
@Component
@RequiredArgsConstructor
public class PesquisaWebTools {

    private final WebClient webClient;

    @Value("${bot.currency.api-url}")
    private String currencyApiUrl;

    private static final Map<String, String> PARES_MOEDA = Map.of(
        "dolar", "USD-BRL",
        "euro", "EUR-BRL",
        "libra", "GBP-BRL",
        "bitcoin", "BTC-BRL",
        "ethereum", "ETH-BRL"
    );

    @Tool("Busca cotação atual de moeda ou criptomoeda. Use para perguntas como 'quanto está o dólar', 'preço do bitcoin', etc.")
    public String consultarCotacao(
        @P("Nome da moeda: dolar, euro, libra, bitcoin, ethereum") String moeda
    ) {
        String par = PARES_MOEDA.getOrDefault(moeda.toLowerCase(), "USD-BRL");
        try {
            String url = currencyApiUrl + "/" + par;
            Map response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null) return "Não consegui buscar a cotação no momento.";

            String chave = par.replace("-", "");
            Map<String, Object> dados = (Map<String, Object>) response.get(chave);
            if (dados == null) return "Cotação não disponível para " + moeda;

            return String.format(
                "Cotação %s:\n• Compra: R$ %s\n• Venda: R$ %s\n• Variação: %s%%\nFonte: AwesomeAPI",
                dados.get("name"),
                dados.get("bid"),
                dados.get("ask"),
                dados.get("pctChange")
            );
        } catch (Exception e) {
            return "Erro ao buscar cotação: " + e.getMessage();
        }
    }
}
```

---

## 9. Passo 5 — Memória de Conversa

O LangChain4j oferece `ChatMemory` nativo. Vamos criar uma implementação que persiste no PostgreSQL, para que o contexto sobreviva a reinicializações.

### BotMemoriaService.java

```java
package dev.financas.FinancasSpring.service;

import dev.financas.FinancasSpring.model.entities.BotMemoria;
import dev.financas.FinancasSpring.repository.BotMemoriaRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotMemoriaService {

    private final BotMemoriaRepository repository;

    @Value("${bot.memory.max-messages:20}")
    private int maxMessages;

    /**
     * Constrói um ChatMemory com persistência no banco para um chatId específico.
     * O LangChain4j usa este objeto para manter contexto entre mensagens.
     */
    public MessageWindowChatMemory buildMemoryForChat(String chatId) {
        return MessageWindowChatMemory.builder()
            .id(chatId)
            .maxMessages(maxMessages)
            .chatMemoryStore(new PostgresChatMemoryStore(chatId, repository))
            .build();
    }

    /**
     * Implementação do ChatMemoryStore usando o repositório JPA.
     */
    private record PostgresChatMemoryStore(
        String chatId,
        BotMemoriaRepository repository
    ) implements ChatMemoryStore {

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return repository.findTop20ByChatIdOrderByCriadoEmAsc(chatId)
                .stream()
                .map(m -> "user".equals(m.getRole())
                    ? UserMessage.from(m.getConteudo())
                    : AiMessage.from(m.getConteudo()))
                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // Persiste apenas as mensagens mais recentes (não duplica)
            repository.deleteBycriadoEmBefore(
                java.time.LocalDateTime.now().minusYears(1) // limpeza segura só no scheduler
            );
            // Estratégia: salvar apenas a última mensagem nova
            if (!messages.isEmpty()) {
                ChatMessage ultima = messages.get(messages.size() - 1);
                BotMemoria memoria = BotMemoria.builder()
                    .chatId(chatId)
                    .role(ultima instanceof UserMessage ? "user" : "assistant")
                    .conteudo(ultima.text())
                    .build();
                repository.save(memoria);
            }
        }

        @Override
        public void deleteMessages(Object memoryId) {
            // Implementação opcional para reset de contexto
        }
    }
}
```

---

## 10. Passo 6 — Configuração do Gemini + LangChain4j

### GeminiConfig.java

```java
package dev.financas.FinancasSpring.config;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.financas.FinancasSpring.bot.tools.FinanceiroTools;
import dev.financas.FinancasSpring.bot.tools.PesquisaWebTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${bot.gemini.api-key}")
    private String apiKey;

    @Value("${bot.gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${bot.gemini.max-tokens:1024}")
    private int maxTokens;

    @Value("${bot.gemini.temperature:0.3}")
    private double temperature;

    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(model)
            .maxOutputTokens(maxTokens)
            .temperature(temperature)
            .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
```

---

## 11. Passo 7 — Serviço Principal de IA (AiAssistant)

Este serviço une tudo: recebe a mensagem do Telegram, injeta o contexto do usuário, chama o Gemini com as tools disponíveis e retorna a resposta.

### AiAssistantService.java

```java
package dev.financas.FinancasSpring.service;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.financas.FinancasSpring.bot.tools.FinanceiroTools;
import dev.financas.FinancasSpring.bot.tools.PesquisaWebTools;
import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final ChatLanguageModel geminiChatModel;
    private final BotMemoriaService memoriaService;
    private final FinanceiroTools financeiroTools;
    private final PesquisaWebTools pesquisaWebTools;
    private final TelegramVinculoService vinculoService;

    // Interface que o LangChain4j implementa automaticamente via AiServices
    interface AssistentFinanceiro {
        @SystemMessage("""
            Você é um assistente financeiro pessoal via Telegram.
            Responda SEMPRE em Português do Brasil. Seja direto e objetivo.
            Seu foco é exclusivamente em finanças pessoais, controle de gastos,
            economias, orçamentos e mercado financeiro.
            
            Para qualquer tema fora de finanças, responda:
            "Sou especializado em finanças 💰. Use !help para ver os comandos disponíveis."
            
            NUNCA invente valores ou saldos. Use sempre as ferramentas disponíveis.
            Ao registrar gastos, confirme os dados antes de registrar.
            """)
        String chat(String mensagem);
    }

    /**
     * Ponto de entrada principal: processa uma mensagem do Telegram.
     *
     * @param chatId   ID único do chat do Telegram
     * @param pushname Nome do usuário no Telegram
     * @param mensagem Texto enviado pelo usuário
     * @return Resposta gerada pela IA
     */
    public String processar(String chatId, String pushname, String mensagem) {
        try {
            // 1. Garante que o vínculo existe no banco
            vinculoService.obterOuCriar(chatId, pushname);

            // 2. Injeta o chatId no contexto para as Tools acessarem
            FinanceiroTools.setChatId(chatId);

            // 3. Recupera a memória persistida do banco para este chat
            ChatMemory memoria = memoriaService.buildMemoryForChat(chatId);

            // 4. Constrói o assistente com as tools e a memória
            AssistentFinanceiro assistente = AiServices.builder(AssistentFinanceiro.class)
                .chatLanguageModel(geminiChatModel)
                .chatMemory(memoria)
                .tools(financeiroTools, pesquisaWebTools)
                .build();

            // 5. Executa
            String resposta = assistente.chat(mensagem);
            log.info("[AI] chatId={} | resposta gerada com sucesso", chatId);
            return resposta;

        } catch (Exception e) {
            log.error("[AI] Erro ao processar mensagem chatId={}: {}", chatId, e.getMessage(), e);
            return "Tive um problema para processar sua mensagem. Tente novamente em alguns segundos.";
        } finally {
            // 6. Limpa o contexto para não vazar entre threads
            FinanceiroTools.clearChatId();
        }
    }
}
```

---

## 12. Passo 8 — Bot do Telegram

### TelegramBotConfig.java

```java
package dev.financas.FinancasSpring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import dev.financas.FinancasSpring.bot.FinanceiroBot;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final FinanceiroBot financeiroBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(financeiroBot);
        return api;
    }
}
```

---

### FinanceiroBot.java

```java
package dev.financas.FinancasSpring.bot;

import dev.financas.FinancasSpring.service.AiAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FinanceiroBot extends TelegramLongPollingBot {

    private final AiAssistantService aiService;

    @Value("${bot.telegram.token}")
    private String token;

    @Value("${bot.telegram.username}")
    private String username;

    // Controla apresentação inicial (substitui o Set em memória do Node.js original)
    private final Set<String> usuariosApresentados = ConcurrentHashMap.newKeySet();

    public FinanceiroBot(AiAssistantService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getBotUsername() { return username; }

    @Override
    public String getBotToken() { return token; }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String chatId  = String.valueOf(update.getMessage().getChatId());
        String texto   = update.getMessage().getText();
        String nome    = update.getMessage().getFrom().getFirstName();

        // Comando !help tratado diretamente para não consumir tokens do Gemini
        if ("!help".equalsIgnoreCase(texto.trim())) {
            enviar(chatId, buildHelpMessage());
            return;
        }

        try {
            String resposta = aiService.processar(chatId, nome, texto);

            // Apresentação na primeira mensagem
            if (!usuariosApresentados.contains(chatId)) {
                usuariosApresentados.add(chatId);
                resposta = String.format(
                    "Olá, *%s*! 👋\n\nSou sua assistente financeira inteligente.\n" +
                    "Posso te ajudar com gastos, economias e orçamentos.\n" +
                    "Digite *!help* para ver os comandos disponíveis.\n\n%s",
                    nome, resposta
                );
            }

            enviar(chatId, resposta);

        } catch (Exception e) {
            log.error("[Bot] Erro ao processar mensagem de {}: {}", chatId, e.getMessage());
            enviar(chatId, "Ops! Tive um probleminha. Pode tentar de novo?");
        }
    }

    private void enviar(String chatId, String texto) {
        // Divide mensagens longas (limite do Telegram: 4096 caracteres)
        int limite = 4000;
        if (texto.length() <= limite) {
            enviarSimples(chatId, texto);
            return;
        }
        String[] partes = texto.split("\n");
        StringBuilder chunk = new StringBuilder();
        for (String linha : partes) {
            if (chunk.length() + linha.length() > limite) {
                enviarSimples(chatId, chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(linha).append("\n");
        }
        if (!chunk.isEmpty()) enviarSimples(chatId, chunk.toString());
    }

    private void enviarSimples(String chatId, String texto) {
        try {
            execute(SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .parseMode("Markdown")
                .build());
        } catch (TelegramApiException e) {
            // Fallback sem Markdown se falhar parsing
            try {
                execute(SendMessage.builder().chatId(chatId).text(texto).build());
            } catch (TelegramApiException ex) {
                log.error("[Bot] Falha ao enviar mensagem para {}: {}", chatId, ex.getMessage());
            }
        }
    }

    private String buildHelpMessage() {
        return """
            *Comandos disponíveis:* ✨
            
            📌 *Registrar gasto*
            Diga: "Gastei 50 reais no Mercado hoje em Alimentação"
            
            📊 *Consultar gastos*
            Diga: "Quanto gastei hoje?" ou "Meus gastos dessa semana"
            
            💰 *Economias*
            Diga: "Guardei 200 reais do salário"
            
            🎯 *Orçamento*
            Diga: "Qual meu orçamento de Alimentação?" ou "!orcamento"
            
            💱 *Cotações*
            Diga: "Quanto está o dólar?" ou "Preço do bitcoin"
            
            💡 *Dica:* Fale naturalmente! A IA entende português.
            """;
    }
}
```

---

## 13. Passo 9 — Segurança e Vinculação Telegram ↔ Usuario

O bot funciona sem login por padrão (qualquer pessoa pode usar). Para vincular um `chatId` a um `Usuario` do sistema web, crie um endpoint seguro:

### TelegramVinculoController.java

```java
package dev.financas.FinancasSpring.controller;

import dev.financas.FinancasSpring.model.entities.TelegramVinculo;
import dev.financas.FinancasSpring.model.entities.Usuario;
import dev.financas.FinancasSpring.repository.TelegramVinculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramVinculoController {

    private final TelegramVinculoRepository repository;

    /**
     * O usuário logado na web informa seu chatId do Telegram para vincular as contas.
     * Exemplo de uso: POST /api/telegram/vincular com body {"chatId": "123456789"}
     */
    @PostMapping("/vincular")
    public ResponseEntity<String> vincular(
        @AuthenticationPrincipal Usuario usuario,
        @RequestBody java.util.Map<String, String> body
    ) {
        String chatId = body.get("chatId");
        if (chatId == null || chatId.isBlank()) {
            return ResponseEntity.badRequest().body("chatId é obrigatório.");
        }

        Optional<TelegramVinculo> vinculo = repository.findByChatId(chatId);
        if (vinculo.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TelegramVinculo v = vinculo.get();
        v.setUsuario(usuario);
        repository.save(v);

        return ResponseEntity.ok("Telegram vinculado com sucesso ao usuário " + usuario.getNomeCompleto());
    }
}
```

---


```

---

## 15. Passo 11 — System Prompt

O system prompt já está embutido na interface `AssistentFinanceiro` via `@SystemMessage` no Passo 7. Para externalizar (recomendado para facilitar ajustes sem recompilar):

```yaml
# No application.yml, adicione:
bot:
  ai:
    system-prompt: |
      Você é um assistente financeiro pessoal via Telegram.
      Responda SEMPRE em Português do Brasil. Seja direto e objetivo.
      Foco exclusivo em finanças pessoais, gastos, economias, orçamentos e mercado financeiro.
      Para temas fora de finanças, responda: "Sou especializado em finanças 💰. Use !help."
      NUNCA invente valores. Use as ferramentas disponíveis.
```

```java
// Em AiAssistantService.java, injete e use:
@Value("${bot.ai.system-prompt}")
private String systemPrompt;

// E passe dinamicamente para o AiServices via ChatMessage ao invés do @SystemMessage fixo
```

---

## 16. Passo 12 — Limpeza de Memória (Scheduler)

Substitui o `cleanup_expired_memories` que existia no Python.

### MemoriaCleanupTask.java

```java
package dev.financas.FinancasSpring.tasks;

import dev.financas.FinancasSpring.repository.BotMemoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoriaCleanupTask {

    private final BotMemoriaRepository repository;

    @Value("${bot.memory.ttl-days:30}")
    private int ttlDias;

    // Executa todo dia à meia-noite
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void limparMemoriasAntigas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(ttlDias);
        repository.deleteBycriadoEmBefore(limite);
        log.info("[Scheduler] Limpeza de memórias anteriores a {} concluída.", limite);
    }
}
```

Habilite o scheduling na classe principal:

```java
// Em sua classe main (FinancasSpringApplication.java), adicione:
@EnableScheduling
```

---

## 17. Estrutura Final de Pacotes

```
dev.financas.FinancasSpring
├── FinancasSpringApplication.java          ← @EnableScheduling aqui
│
├── model/
│   └── entities/
│       ├── Usuario.java                    (existente — adicionar lista TelegramVinculos)
│       ├── Financeiro.java                 (existente)
│       ├── Detalhes.java                   (existente)
│       ├── Preferencias.java               (existente)
│       ├── Demandas.java                   (existente)
│       ├── TelegramVinculo.java            ← NOVO
│       ├── Gasto.java                      ← NOVO
│       ├── Economia.java                   ← NOVO
│       ├── OrcamentoCategoria.java         ← NOVO
│       └── BotMemoria.java                 ← NOVO
│
├── repository/
│   ├── TelegramVinculoRepository.java      ← NOVO
│   ├── GastoRepository.java                ← NOVO
│   ├── EconomiaRepository.java             ← NOVO
│   ├── OrcamentoRepository.java            ← NOVO
│   └── BotMemoriaRepository.java           ← NOVO
│
├── service/
│   ├── TelegramVinculoService.java         ← NOVO
│   ├── GastoService.java                   ← NOVO
│   ├── EconomiaService.java                ← NOVO
│   ├── OrcamentoService.java               ← NOVO
│   ├── BotMemoriaService.java              ← NOVO
│   └── AiAssistantService.java             ← NOVO (orquestrador central)
│
├── bot/
│   ├── FinanceiroBot.java                  ← NOVO (listener do Telegram)
│   └── tools/
│       ├── FinanceiroTools.java            ← NOVO (@Tool gastos/economias/orçamentos)
│       └── PesquisaWebTools.java           ← NOVO (@Tool cotações e busca web)
│
├── config/
│   ├── GeminiConfig.java                   ← NOVO
│   └── TelegramBotConfig.java              ← NOVO
│
├── controller/
│   └── TelegramVinculoController.java      ← NOVO (vinculação web ↔ telegram)
│
└── tasks/
    └── MemoriaCleanupTask.java             ← NOVO
```

---

## 18. Ordem de Execução dos Passos

Siga esta sequência para evitar erros de dependência entre classes:

```
1.  Adicionar dependências no pom.xml
2.  Configurar application.yml com as variáveis de ambiente
3.  Criar as 5 novas entidades (TelegramVinculo, Gasto, Economia, OrcamentoCategoria, BotMemoria)
4.  Ajustar Usuario.java para incluir a lista de TelegramVinculos
5.  Criar os 5 repositórios JPA
6.  Criar TelegramVinculoService (sem dependências internas)
7.  Criar GastoService, EconomiaService, OrcamentoService
8.  Criar BotMemoriaService
9.  Criar FinanceiroTools e PesquisaWebTools (dependem dos services)
10. Criar GeminiConfig (depende das tools e do WebClient)
11. Criar AiAssistantService (depende de tudo acima)
12. Criar FinanceiroBot (depende do AiAssistantService)
13. Criar TelegramBotConfig (registra o FinanceiroBot)
14. Criar TelegramVinculoController (opcional, para vinculação web)
15. Criar MemoriaCleanupTask + @EnableScheduling na main
16. Testar build: mvn clean package -DskipTests
17. Testar localmente com variáveis de ambiente configuradas
18. Deploy
```

---

## 19. Checklist Final

### Variáveis de Ambiente Necessárias

```env
DATABASE_URL=jdbc:postgresql://host:5432/financas
DB_USER=postgres
DB_PASS=sua_senha
TELEGRAM_TOKEN=seu_token_do_botfather
TELEGRAM_BOT_USERNAME=nome_do_bot_sem_arroba
GEMINI_API_KEY=sua_chave_da_google_ai_studio
```

### Para obter a Gemini API Key

1. Acesse https://aistudio.google.com/app/apikey
2. Clique em "Create API Key"
3. Copie e cole em `GEMINI_API_KEY`

### O que foi eliminado

| Componente antigo         | Substituído por                          |
|---------------------------|------------------------------------------|
| Node.js + Telegraf        | `FinanceiroBot.java` (TelegramBots)     |
| Redis + BullMQ            | Processamento síncrono no Spring         |
| Python FastAPI            | `AiAssistantService.java`               |
| Ollama (modelos locais)   | Gemini API (Google)                      |
| `tools_definition` JSON   | Anotações `@Tool` no Java               |
| `tool_runner.py`          | Roteamento automático do LangChain4j    |
| `context_service.py`      | `BotMemoriaService.java` + PostgreSQL   |
| `cleanup_expired_memories`| `MemoriaCleanupTask.java` + @Scheduled  |
| Whisper (transcrição)     | *(não incluído nesta versão — opcional)*|

### Melhorias em relação à arquitetura anterior

- **Zero saltos de rede internos:** As tools chamam os services Java diretamente na mesma JVM
- **Memória persistente real:** Histórico de conversa no PostgreSQL (antes era em memória no Python)
- **Vinculação de contas:** O `TelegramVinculo` conecta o bot ao usuário do sistema web
- **Orçamento por usuário do bot:** Antes não havia tabela dedicada para isso
- **Um único container** para deploy (antes eram 3+ serviços)
- **Sem GPU necessária:** O Gemini roda na nuvem da Google

---

*Roteiro gerado em março de 2026 com base nas entidades e arquitetura existentes do projeto `dev.financas.FinancasSpring`.*
