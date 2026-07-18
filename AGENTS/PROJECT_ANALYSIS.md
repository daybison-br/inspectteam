# Análise técnica e estado de implementação do InspecTeam

## Objetivo

Este documento registra o estado observado em 18 de julho de 2026 e é o ponto de retomada do projeto. As funcionalidades descritas como implementadas existem nos arquivos atuais. A seção de pendências contém recomendações, não funcionalidades prontas.

## Visão geral

O InspecTeam é um SaaS multi-tenant para criação, distribuição e preenchimento de formulários operacionais. O caso de uso de referência é um proprietário de empresa de guincho criando checklists na web e liberando-os para funcionários preencherem em dispositivos móveis, inclusive offline.

A raiz contém:

```text
inspecTeam/
├── AGENTS/PROJECT_ANALYSIS.md
├── api/                 # API Spring Boot
├── web/                 # protótipo web React/Vinext
├── compose.yaml         # PostgreSQL e MinIO locais
└── .env.example         # variáveis de ambiente de exemplo
```

Existe um repositório Git inicializado na raiz. Artefatos gerados, como `api/target`, `web/node_modules`, `web/dist` e `web/.vinext`, não são código-fonte e não devem ser versionados.

O arquivo `.gitignore` da raiz protege variáveis locais, chaves privadas, keystores, configurações com segredos, backups, logs, dependências e artefatos de build. O arquivo `.env.example` permanece versionável por conter apenas placeholders.

## Stack confirmada

### Backend

- Java 21;
- Spring Boot 4.1.0;
- Maven Wrapper 3.9.16;
- API HTTP com Spring MVC;
- Spring Security com JWT HMAC;
- Spring JDBC e PostgreSQL 18;
- Flyway com nove migrações;
- MinIO para armazenamento de fotos e assinaturas;
- Testcontainers com PostgreSQL real nos testes;
- empacotamento JAR.

### Web

- React 19.2.6;
- Next 16.2.6 executado por Vinext 0.0.50 e Vite 8;
- TypeScript 5.9.3;
- Node.js 22.13 ou superior.

O aplicativo React atual é um protótipo interativo do construtor de formulários. Ele ainda não consome a API.

## Arquitetura do backend

O pacote raiz é `com.inspecteam`. A organização é por funcionalidade, mantendo API, aplicação, domínio e infraestrutura próximos:

```text
com.inspecteam/
├── auth/          # registro, login, refresh e logout
├── tenant/        # seleção e consulta de tenants
├── user/          # funcionários e memberships
├── permission/    # papéis, permissões e grants por formulário
├── form/          # drafts, validação e versões publicadas
├── submission/    # respostas, revisão otimista e conclusão
├── sync/          # dispositivos, pull, push e idempotência
├── file/          # sessões de upload no MinIO
├── audit/         # trilha de auditoria
└── shared/        # segurança e tratamento de erros
```

O isolamento multi-tenant ocorre em duas camadas:

1. serviços validam a membership ativa e as permissões do ator;
2. PostgreSQL aplica Row-Level Security nas tabelas pertencentes ao tenant.

O tenant ativo é definido localmente na transação com `set_config`. As tabelas globais de identidade e catálogo de tenants permanecem fora de RLS para permitir autenticação e seleção do tenant.

## Funcionalidades implementadas

### Identidade e tenants

- cadastro transacional de tenant e proprietário;
- login com senha BCrypt;
- access token de curta duração;
- refresh token rotativo armazenado como hash;
- logout por revogação do refresh token;
- listagem dos tenants acessíveis ao usuário.

### Usuários e autorização

- cadastro e suspensão de funcionários do tenant;
- papéis customizados;
- catálogo de permissões;
- associação de permissões a papéis;
- associação de papéis a memberships;
- grants detalhados por formulário para membership ou papel;
- política padrão de negar o acesso quando não houver permissão.

### Formulários

- criação e edição de rascunho;
- validação da definição JSON;
- publicação de versão imutável;
- criação automática do próximo rascunho;
- campos suportados: texto, texto longo, número, data, hora, seleção, multisseleção, checkbox, foto, assinatura, título e instruções.

### Respostas, arquivos e offline

- criação idempotente de respostas por UUID do cliente;
- salvamento de rascunho com controle otimista de revisão;
- conclusão imutável da resposta;
- upload direto por URL pré-assinada do MinIO, limitado a 20 MB;
- registro de dispositivos;
- sincronização pull de formulários publicados e tombstones;
- sincronização push idempotente com `mutationId`;
- mutações `CREATE`, `UPDATE` e `COMPLETE`;
- auditoria das principais operações administrativas e operacionais.

## Banco de dados

As migrações em `api/src/main/resources/db/migration` são:

1. usuários e refresh tokens;
2. tenants e memberships;
3. papéis, permissões e grants;
4. formulários e versões;
5. respostas e revisões;
6. metadados de arquivos;
7. dispositivos e controle de sincronização;
8. eventos de auditoria;
9. políticas de Row-Level Security.

O Hibernate está configurado apenas para validar o esquema. A evolução estrutural do banco deve ocorrer exclusivamente por novas migrações Flyway; migrações existentes não devem ser reescritas após serem compartilhadas.

## Contratos HTTP existentes

Todos os contratos usam o prefixo `/api/v1`.

- `/auth`: cadastrar tenant, login, refresh e logout;
- `/tenants`: listar tenants do usuário;
- `/tenants/{tenantId}/users`: listar, cadastrar e suspender usuários;
- `/tenants/{tenantId}/permissions`: papéis, permissões e grants;
- `/tenants/{tenantId}/forms`: criar, listar, editar draft e publicar;
- `/tenants/{tenantId}/submissions`: criar, editar, concluir e listar respostas;
- `/tenants/{tenantId}/sync`: registrar dispositivo, pull e push;
- `/tenants/{tenantId}/files`: iniciar e concluir upload;
- `/tenants/{tenantId}/audit`: consultar auditoria.

Erros HTTP são representados por Problem Details. Rotas protegidas esperam `Authorization: Bearer <token>`.

## Configuração local

`compose.yaml` fornece PostgreSQL 18 e MinIO. `.env.example` contém somente valores de desenvolvimento e deve ser copiado para um `.env` local com segredos próprios. A API importa opcionalmente o `.env` da raiz e usa `POSTGRES_*` e `MINIO_ROOT_*` como fallback local; em produção, `DATABASE_*` e `STORAGE_*` têm prioridade.

### Iniciar infraestrutura

```powershell
docker compose up -d
```

### Backend no Windows

```powershell
cd api
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

### Backend em Unix

```bash
cd api
./mvnw test
./mvnw clean package
./mvnw spring-boot:run
```

### Frontend

```powershell
cd web
npm install
npm run dev
npm run build
```

Portas locais padrão: API `8080`, PostgreSQL `5432`, web `3000`, MinIO API `9000` e console MinIO `9001`.

## Validação realizada

O backend foi validado com `.\mvnw.cmd test`.

- 2 testes executados;
- 0 falhas e 0 erros;
- PostgreSQL 18 iniciado por Testcontainers;
- nove migrações Flyway aplicadas;
- contexto Spring carregado;
- fluxo integrado aprovado: cadastro do proprietário, criação e publicação de formulário, registro de dispositivo, pull de sincronização, criação e conclusão da resposta.

O frontend foi validado com `npm run build`, concluído sem erro.

## Limitações e próximos passos

Ainda não estão implementados:

1. integração do frontend com autenticação e endpoints reais;
2. aplicativo React Native e armazenamento offline no dispositivo;
3. resolução avançada de conflitos de sincronização e fila de uploads offline;
4. convite por e-mail, expiração de convite e troca obrigatória da senha temporária;
5. painel global completo para o administrador da plataforma — a leitura global existe no modelo de segurança, mas mutações administrativas entre tenants ainda exigem um fluxo explícito de suporte/impersonação;
6. OpenAPI/Swagger e exemplos formais dos contratos;
7. recuperação de senha, MFA, rate limiting e gestão/rotação de segredos;
8. testes de isolamento RLS entre dois tenants, autorização negativa, uploads MinIO e concorrência de sincronização;
9. observabilidade de produção, CI/CD, backups e implantação;
10. políticas operacionais de LGPD, retenção, exportação e exclusão de dados.

## Ordem recomendada para retomar

1. criar testes de segurança provando que um tenant não lê ou altera dados de outro;
2. conectar o painel web ao cadastro/login e à listagem real de formulários;
3. implementar no frontend o editor persistido de drafts e a publicação;
4. formalizar OpenAPI e os contratos usados posteriormente pelo React Native;
5. projetar o banco local e o protocolo de conflitos do aplicativo offline antes de iniciar o mobile.

## Observações

- A aplicação usa PostgreSQL, não H2, inclusive no teste de integração.
- `target/`, `node_modules/`, `dist/` e `.vinext/` são descartáveis.
- O aviso do Mockito sobre carregamento dinâmico do agente não falha o build, mas deve ser acompanhado em atualizações futuras do Java.
- O validador de formulários utiliza uma API marcada como deprecated pelo compilador; o build passa, mas a chamada deve ser atualizada em uma manutenção futura.
- O MinIO está adequado ao ambiente local atual; a escolha de armazenamento de produção deve ser reavaliada antes da implantação.
