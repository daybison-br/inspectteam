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
├── web/                 # aplicação web React/Vinext integrada
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
- Flyway com doze migrações;
- MinIO para armazenamento de fotos e assinaturas;
- Testcontainers com PostgreSQL real nos testes;
- empacotamento JAR.

### Web

- React 19.2.6;
- Next 16.2.6 executado por Vinext 0.0.50 e Vite 8;
- TypeScript 5.9.3;
- Node.js 22.13 ou superior.

O aplicativo React é um painel multi-rotas integrado à API por um gateway de sessão com cookies HttpOnly. Possui login, cadastro, gestão do tenant, editor persistido, respostas, equipe, acessos, configurações, perfil pessoal em `/app/perfil` e administração global. A identificação do usuário aparece somente no cabeçalho: o avatar abre um menu de conta com acesso ao perfil e logout. Nome e cargo não são repetidos na navegação lateral, e dados pessoais não ficam misturados às configurações do tenant. Usuários criados com senha temporária recebem um modal bloqueante imediatamente após o login e só acessam a aplicação depois de redefinir a senha. A interface possui um Design System interno baseado em tokens semânticos, iconografia Fluent, temas claro e escuro, shell responsivo, tabelas corporativas, estados vazios, avisos e diálogos acessíveis. A auditoria do tenant pode ser consultada em `/app/auditoria`.

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
- campos suportados: texto, texto longo, número, data, hora, seleção, multisseleção, checkbox, foto, assinatura, título e instruções;
- editor de seleção simples e múltipla com opções individuais, inclusão por botão ou Enter, ordenação, remoção, foco gerenciado e validação de vazio, duplicidade e quantidade mínima;
- paleta de tipos do construtor com ícones Fluent específicos para texto, número, data, hora, seleções, checkbox, foto, assinatura, título e instruções;
- composição contextual por seção ativa: campos são incluídos na seção selecionada, novas seções são criadas pelo cartão tracejado ao final e podem ser reordenadas por controles acessíveis;
- listagem operacional com ações minimalistas por ícone para utilizar, editar, arquivar, restaurar e excluir;
- preenchimento de formulários publicados diretamente na web, incluindo validação obrigatória, fotos e assinatura desenhada ou registrada por teclado, com upload pré-assinado;
- exclusão lógica de formulários com deleted = true, autoria/data, auditoria e tombstone, preservando versões e respostas.

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
9. políticas de Row-Level Security;
10. expansão da gestão web, senha temporária e membership administrativa;
11. auditoria global da plataforma;
12. exclusão lógica de formulários, autoria da exclusão e índice parcial de itens ativos.

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

- 4 testes executados;
- 0 falhas e 0 erros;
- PostgreSQL 18 iniciado por Testcontainers;
- doze migrações Flyway aplicadas;
- contexto Spring carregado;
- fluxo integrado aprovado: cadastro do proprietário, criação/publicação e consulta web do formulário, registro de dispositivo, pull de sincronização, criação/conclusão da resposta e exclusão lógica com preservação do histórico.

O frontend foi validado com `npm test` e `npm run lint`: build concluído, 13 testes aprovados e nenhum erro de lint.

A navegação entre rotas possui proteção contra efeitos assíncronos retornados como cleanup do React e uma barreira global em `web/app/error.tsx` para recuperação de falhas inesperadas sem deixar a interface em branco.

O gateway de sessão lê os cookies recebidos como dados imutáveis e sempre devolve uma nova `Response` com `Headers` mutáveis. Login, cadastro, renovação e logout preservam os dois cabeçalhos `Set-Cookie`; access e refresh tokens continuam `HttpOnly`, `SameSite=Lax`, com expiração explícita e `Secure` em produção. Essa regra evita o erro `Can't modify immutable headers` observado no runtime Vinext.

O `web/vite.config.ts` usa um logger específico para ignorar somente sourcemaps inválidos publicados por `@griffel`; warnings do código da aplicação e de outras dependências continuam ativos.

## Limitações e próximos passos

Ainda não estão implementados:

1. aplicativo React Native e armazenamento offline no dispositivo;
2. recuperação de senha, MFA e rate limiting;
3. convite por e-mail e expiração automática de convite;
4. exportação de respostas, OpenAPI/Swagger e CI/CD;
5. observabilidade, políticas LGPD e automação de backups de produção.

## Ordem recomendada para retomar

1. ampliar os testes negativos de autorização e isolamento RLS;
2. formalizar OpenAPI para o cliente mobile;
3. projetar o banco local e os conflitos do aplicativo offline;
4. implementar recuperação de senha e MFA.
## Observações

- A aplicação usa PostgreSQL, não H2, inclusive no teste de integração.
- `target/`, `node_modules/`, `dist/` e `.vinext/` são descartáveis.
- O aviso do Mockito sobre carregamento dinâmico do agente não falha o build, mas deve ser acompanhado em atualizações futuras do Java.
- O validador de formulários utiliza uma API marcada como deprecated pelo compilador; o build passa, mas a chamada deve ser atualizada em uma manutenção futura.
- O MinIO está adequado ao ambiente local atual; a escolha de armazenamento de produção deve ser reavaliada antes da implantação.
