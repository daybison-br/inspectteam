# InspecTeam Mobile

Aplicativo Android-first em React Native e Expo para executar formulários operacionais com funcionamento offline.

## O que está implementado

- autenticação real, refresh token seguro e troca obrigatória de senha temporária;
- seleção de tenant e catálogo local de formulários autorizados;
- banco SQLite criptografado com SQLCipher, WAL e versão de esquema;
- rascunho automático e fila idempotente de sincronização;
- os doze tipos de campo, incluindo foto e assinatura;
- upload de anexos por URL pré-assinada;
- sincronização manual e tentativa periódica em segundo plano;
- temas claro/escuro e controles com alvos de toque acessíveis.

## Desenvolvimento Android

Pré-requisitos: Node.js 22, JDK 21, Android Studio/SDK e a API local em execução.

1. Copie .env.example para .env.
2. Execute npm ci.
3. Gere o development build com npx expo run:android.
4. Nas execuções seguintes, use npm start.

O emulador Android acessa a API do host por http://10.0.2.2:8080/api/v1. Em aparelho físico, use o IP da máquina na rede local.

O SQLCipher exige código nativo; portanto, o fluxo completo não funciona no Expo Go.

## Validação

    npm test
    npm run typecheck
    npm run lint
    npx expo install --check
    npx expo export --platform android

## Builds com EAS

Os perfis estão em eas.json. Após autenticar e configurar o projeto no EAS:

    npx eas-cli build --platform android --profile development
    npx eas-cli build --platform android --profile production

Não grave tokens da Expo, keystores ou URLs privadas no repositório.

## Limitações atuais

Ainda faltam validação em aparelhos físicos, testes E2E, resolução assistida de conflitos, telemetria, política de retenção local, build iOS e publicação nas lojas. A execução em segundo plano depende das janelas concedidas pelo Android ou iOS.
