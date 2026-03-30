# 💰 Financas Spring — Assistente Financeiro Pessoal via Telegram

Sistema de gestão financeira pessoal integrado a um **bot do Telegram** com inteligência artificial.
O usuário interage via mensagens de texto, áudio ou fotos de comprovantes e o sistema registra gastos, economias, orçamentos e fornece resumos financeiros — tudo de forma conversacional e em português.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Funcionalidades](#-funcionalidades)
- [Arquitetura](#-arquitetura)
- [Tecnologias](#-tecnologias)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração](#-configuração)
- [Subindo o Sistema](#-subindo-o-sistema)
- [Execução Local (dev)](#-execução-local-dev)
- [Endpoints da API](#-endpoints-da-api)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Logs](#-logs)

---

## 🔍 Visão Geral

O **Financas Spring** é uma API REST construída com Spring Boot que também opera como um bot do Telegram.
O bot utiliza IA generativa (Google Gemini, com fallback para Ollama local) para entender linguagem natural e executar ações financeiras como:

- Registrar gastos por texto, voz ou foto de comprovante
- Consultar resumos e históricos de gastos
- Gerenciar economias e orçamentos por categoria
- Consultar cotações de moedas e criptomoedas

As mensagens recebidas pelo bot são enfileiradas no **RabbitMQ** e processadas de forma assíncrona por consumidores dedicados, garantindo resiliência e escalabilidade.

---

## ✨ Funcionalidades

| Canal           | Descrição |
|-----------------|-----------|
| 💬 **Texto**    | Registra gastos, consulta resumos, gerencia orçamentos via linguagem natural |
| 🎤 **Áudio**    | Transcreve mensagens de voz com Gemini e processa como texto |
| 📷 **Imagem**   | Lê comprovantes/recibos com Gemini Vision, extrai dados e pede confirmação |
| 🔐 **Auth**     | Login e cadastro de usuário diretamente pelo chat do Telegram |
| 💱 **Cotações** | Consulta valores de dólar, euro, bitcoin e mais |
| 🎯 **Orçamento**| Define e acompanha limites por categoria de gasto |

---

## 🏗 Arquitetura

```
Telegram  ──▶  FinanceiroBot (Long Polling)
                    │
                    ▼
              RabbitMQ (fila)
                    │
                    ▼
          TelegramMessageConsumer
           ┌───────┼───────┐
           ▼       ▼       ▼
         Texto   Áudio   Imagem
           │       │       │
           ▼       ▼       ▼
      AiAssistant  Gemini  Gemini Vision
      Service      (transcrição)  (OCR)
           │
           ▼
    PostgreSQL + pgvector (memória vetorial)
```

- **FinanceiroBot** — Recebe updates do Telegram via Long Polling e enfileira no RabbitMQ
- **RabbitMQ** — Fila de mensagens para processamento assíncrono
- **TelegramMessageConsumer** — Consome a fila e delega para o serviço de IA, transcrição ou leitura de comprovantes
- **AiAssistantService** — IA conversacional usando LangChain4j com Gemini (primário) e Ollama (fallback)
- **PostgreSQL + pgvector** — Banco de dados relacional com extensão vetorial para memória semântica do bot

---

## 🛠 Tecnologias

| Componente        | Tecnologia |
|-------------------|------------|
| Backend           | Java 17, Spring Boot 3.2.7 |
| IA / LLM          | Google Gemini (via LangChain4j), Ollama (fallback local) |
| Embeddings        | Gemini Embedding 001, pgvector |
| Mensageria        | RabbitMQ (AMQP) |
| Banco de Dados    | PostgreSQL 16 (imagem pgvector) |
| Bot Telegram      | TelegramBots Spring Boot Starter 6.9.7 |
| Autenticação API  | Spring Security + JWT (jjwt 0.11.5) |
| Documentação API  | SpringDoc OpenAPI (Swagger UI) |
| Migrations        | Flyway |
| Build             | Maven, Docker multi-stage |

---

## 📌 Pré-requisitos

- **Docker** e **Docker Compose** instalados
- **Token do Bot Telegram** — crie um bot via [@BotFather](https://t.me/BotFather)
- **Chave de API do Gemini** — obtenha em [Google AI Studio](https://aistudio.google.com/apikey)
- *(Opcional)* **Ollama** instalado localmente para fallback de IA
- *(Opcional)* **Java 17** e **Maven 3.9+** se quiser rodar fora do Docker

---

## ⚙ Configuração

### 1. Clone o repositório

```bash
git clone https://github.com/Aerlonga/financas-spring.git
cd financas-spring
```

### 2. Crie o arquivo `.env`

Copie o exemplo e preencha com seus valores:

```bash
cp .env.example .env
```

Edite o `.env` com suas credenciais:

```env
# Segurança
JWT_SECRET=uma_chave_secreta_forte_com_pelo_menos_32_caracteres
JWT_EXPIRATION=3600000

# Telegram (obtenha no @BotFather)
TELEGRAM_TOKEN=123456789:ABCdefGHI...
TELEGRAM_BOT_USERNAME=nome_do_seu_bot

# Google Gemini (obtenha em aistudio.google.com)
GEMINI_API_KEY=sua_chave_gemini_aqui

# Groq (opcional, atualmente comentado no código)
GROQ_API_KEY=sua_chave_groq_aqui

# Banco de Dados
DB_HOST=localhost
DB_PORT=5432
DB_NAME=financas
DB_USERNAME=financas_user
DB_PASSWORD=financas_senha
DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
```

> ⚠️ **Importante**: O `TELEGRAM_TOKEN` e `GEMINI_API_KEY` são **obrigatórios** para o funcionamento do bot.

---

## 🚀 Subindo o Sistema

### Com Docker Compose (recomendado)

Este comando sobe os três serviços: **PostgreSQL**, **RabbitMQ** e a **Aplicação**:

```bash
docker compose up -d --build
```

Serviços disponíveis após o start:

| Serviço           | URL                          |
|-------------------|------------------------------|
| Aplicação (API)   | http://localhost:8080        |
| Swagger UI        | http://localhost:8080/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672      |
| PostgreSQL        | localhost:5432               |

> As credenciais padrão do RabbitMQ management são `financas` / `financas123`.

### Verificando se subiu corretamente

```bash
# Checar status dos containers
docker compose ps

# Ver logs da aplicação
docker compose logs -f app
```

O bot do Telegram já estará ativo automaticamente. Envie uma mensagem para o seu bot no Telegram para iniciar!

---

## 🖥 Execução Local (dev)

Para rodar fora do Docker (desenvolvimento), você precisa ter os serviços de infraestrutura rodando:

### 1. Suba apenas o PostgreSQL e RabbitMQ

```bash
docker compose up -d postgres rabbitmq
```

### 2. Configure o profile local

As configurações de desenvolvimento estão em `src/main/resources/application-local.properties`.
As variáveis de ambiente do `.env` são lidas automaticamente.

### 3. Execute a aplicação

```bash
# Com Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Ou diretamente
export $(cat .env | xargs) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🌐 Endpoints da API

A documentação completa e interativa está disponível via Swagger UI em:

```
http://localhost:8080/swagger-ui.html
```

### Principais recursos:

| Recurso         | Endpoint Base        | Descrição |
|-----------------|----------------------|-----------|
| Autenticação    | `POST /api/auth/*`   | Login e registro de usuários (JWT) |
| Usuários        | `/api/usuarios/*`    | CRUD de usuários |
| Financeiro      | `/api/financeiro/*`  | Gerenciamento de registros financeiros |
| Detalhes        | `/api/detalhes/*`    | Detalhes adicionais de lançamentos |
| Preferências    | `/api/preferencias/*`| Configurações do usuário |
| Telegram        | `/api/telegram/*`    | Vinculação de conta Telegram |

---

## 📁 Estrutura do Projeto

```
financas-spring/
├── src/main/java/dev/financas/FinancasSpring/
│   ├── bot/                    # Bot Telegram (FinanceiroBot, Consumer, Session)
│   ├── configuration/          # Configs (IA, RabbitMQ, Security, etc.)
│   ├── exceptions/             # Exceções customizadas
│   ├── model/
│   │   ├── entities/           # Entidades JPA (Usuario, Gasto, Economia, etc.)
│   │   └── dto/                # DTOs de transferência
│   ├── repository/             # Spring Data JPA repositories
│   ├── rest/
│   │   ├── controllers/        # REST Controllers
│   │   ├── dto/                # DTOs da camada REST
│   │   └── mapper/             # Mapeadores entidade ↔ DTO
│   ├── security/               # JWT Filter, Security Config
│   ├── services/               # Lógica de negócio
│   └── tasks/                  # Tarefas agendadas
├── src/main/resources/
│   ├── application-local.properties
│   └── logback-spring.xml
├── docker-compose.yml
├── Dockerfile
├── entrypoint.sh
├── pom.xml
├── .env.example
└── README.md
```

---

## 📝 Logs

Os logs são configurados via Logback e gravados em:

- **Console** — saída padrão (útil com `docker compose logs`)
- **Arquivo** — `logs/app-YYYY-MM-DD.log` (rotação diária, retenção de 30 dias)

O pacote `dev.financas` é logado no nível **DEBUG**, enquanto o restante roda em **INFO**.

---

## 📄 Licença

Este projeto é de uso pessoal/educacional.