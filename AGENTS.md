# Regras permanentes de trabalho do InspecTeam

Este arquivo contém lembretes obrigatórios para qualquer pessoa ou agente que trabalhe neste projeto.

## Regras principais

1. Toda alteração aprovada deve ser implementada na aplicação real, e não apenas descrita em um plano, resposta ou documento.
2. Sempre atualizar a documentação quando houver mudança no código, na arquitetura, na configuração, nos comandos, no banco de dados, nos endpoints ou no processo de implantação.
3. Manter `docs/index.html` sincronizado com o comportamento atual da aplicação.
4. Atualizar `AGENTS/PROJECT_ANALYSIS.md` quando a mudança alterar o estado técnico, as funcionalidades existentes, as limitações ou os próximos passos.
5. Não documentar funcionalidades planejadas como se já estivessem implementadas.

## Checklist obrigatório para cada alteração

- Analisar o código e a configuração existentes antes de editar.
- Implementar a alteração no módulo correto, respeitando a organização por funcionalidade.
- Criar uma nova migração Flyway quando houver mudança no esquema do banco; não reescrever migrações já aplicadas.
- Atualizar testes ou criar novos testes proporcionais ao comportamento alterado.
- Executar as validações relevantes do backend, frontend, Docker ou banco.
- Atualizar a documentação técnica e operacional relacionada.
- Conferir se exemplos, comandos, variáveis de ambiente e endpoints documentados continuam corretos.
- Registrar claramente qualquer limitação ou trabalho ainda pendente.

## Documentos de referência

- `docs/index.html`: documentação completa para desenvolvimento, API, arquitetura e produção.
- `AGENTS/PROJECT_ANALYSIS.md`: retrato técnico e ponto de retomada do projeto.
- `.env.example`: variáveis de ambiente de referência, sem segredos reais.
- `compose.yaml`: infraestrutura local de desenvolvimento.

## Diretrizes permanentes de interface e Design System

Toda tela nova ou alterada deve usar o InspecTeam Design System e manter uma linguagem única. Material Design 3, Fluent 2 e Carbon são referências de técnica; não misturar bibliotecas ou copiar estilos divergentes na mesma interface.

### Fundamentos obrigatórios

- Usar tokens globais e tokens semânticos para cor, tipografia, espaçamento, raio, borda, elevação e movimento. Não espalhar valores hexadecimais ou medidas arbitrárias pelos componentes.
- Nomear tokens pela função (`surface`, `text-secondary`, `status-danger`, `focus`) e garantir equivalência nos temas claro, escuro e alto contraste.
- Aplicar uma escala de espaçamento baseada em 4 px. Usar proximidade e espaço em branco para formar grupos antes de adicionar bordas e cards.
- Manter hierarquia tipográfica lógica e escaneável: um `h1` por página, subtítulos em ordem, texto secundário visualmente subordinado e linguagem curta e direta.
- Usar cores semanticamente: verde para sucesso/ação positiva, amarelo para atenção, vermelho para erro ou ação destrutiva e azul para informação. Nunca comunicar estado somente por cor.
- Preferir superfícies planas e elevação moderada. Sombras devem comunicar sobreposição ou mudança de nível, não decorar todos os componentes.

### Layout, navegação e responsividade

- Construir o primeiro viewport em torno da tarefa da página, não de cards genéricos de dashboard.
- Navegação deve ser curta, previsível, orientada ao objetivo, com rótulos claros, ícones da família Fluent e indicação persistente da rota ativa.
- Em telas pequenas, reordenar, resumir ou revelar conteúdo progressivamente; não apenas comprimir a versão desktop.
- Validar no mínimo em 320, 390, 768, 1024 e 1440 px. Conteúdo deve refluir com zoom de texto em 200% e zoom da página em 400% sem perda de funcionalidade.
- Alvos de toque devem ter pelo menos 44 x 44 px quando usados no mobile.
- Tabelas devem usar toolbar para busca/filtros/ações, cabeçalhos semânticos, estados vazio e erro, ordenação acessível quando existir e alternativa responsiva para os dados essenciais.
- Permissões, configurações complexas e grandes volumes de dados devem usar revelação progressiva, abas, drawers ou master-detail para evitar paredes de controles.

### Interação e acessibilidade

- Meta mínima: WCAG 2.2 AA. Texto normal deve alcançar contraste 4.5:1; texto grande e componentes visuais, pelo menos 3:1.
- Toda funcionalidade deve operar por teclado, com foco visível, ordem previsível e restauração de foco ao fechar modal, drawer ou menu.
- Usar HTML semântico primeiro; ARIA complementa a semântica, não substitui elementos nativos.
- Modais precisam de `role`, nome acessível, `aria-modal`, controle de foco, fechamento previsível e ação primária inequívoca. Operações destrutivas exigem confirmação contextual.
- Sempre implementar estados de carregamento, vazio, erro, sucesso, desabilitado, acesso negado e recuperação. Nenhuma falha deve deixar a interface em branco.
- Movimento deve ser curto, funcional e consistente; respeitar `prefers-reduced-motion` e nunca depender de animação para comunicar informação.
- Não usar `alert`, `confirm`, símbolos improvisados ou ações disponíveis somente no hover. Usar os componentes e ícones do Design System.

### Segurança de implementação React

- Uma função passada ao `useEffect` só pode retornar `undefined` ou uma função de limpeza. Nunca passar diretamente uma função que retorna `Promise` (`useEffect(load, ...)`) e nunca usar `useEffect(async () => ...)`; envolver com `useEffect(() => { void load(); }, [...])`.
- Toda nova rota deve permanecer utilizável quando uma requisição falhar e deve oferecer mensagem compreensível e tentativa de recuperação.
- Em Route Handlers do Vinext/Next, tratar `cookies()` e `headers()` de `next/headers`, além de respostas recebidas por `fetch`, como somente leitura. Cookies de sessão devem ser emitidos em uma nova `Response`, usando uma nova instância de `Headers`; nunca alterar headers de uma resposta upstream nem usar `cookies().set/delete` nesse gateway.
- Antes de concluir alterações de frontend, executar build, lint, testes e pesquisar regressões de efeitos assíncronos, `alert`, `confirm`, erros de console e controles sem nome acessível.

### Fontes oficiais de referência

- Material Design 3 — tokens e layout: https://m3.material.io/foundations/design-tokens/overview e https://m3.material.io/foundations/layout/understanding-layout/overview
- Fluent 2 — tokens, layout, acessibilidade e movimento: https://fluent2.microsoft.design/design-tokens, https://fluent2.microsoft.design/layout, https://fluent2.microsoft.design/accessibility e https://fluent2.microsoft.design/motion
- Carbon — tabelas e acessibilidade: https://carbondesignsystem.com/components/data-table/usage/ e https://carbondesignsystem.com/guidelines/accessibility/developers/

## Critério de conclusão

Uma tarefa que altera a aplicação somente é considerada concluída quando:

1. a implementação está presente no código;
2. as validações relevantes foram executadas;
3. a documentação foi atualizada;
4. funcionalidades futuras continuam identificadas como pendentes.

## Versionamento e envio obrigatório

Ao final de toda atualização concluída:

1. verificar se nenhum segredo, arquivo local, build ou backup será versionado;
2. atualizar a documentação relacionada;
3. revisar o resultado de `git status` e `git diff`;
4. adicionar somente os arquivos corretos ao Git;
5. criar um commit com mensagem clara sobre a alteração;
6. enviar o commit para o repositório remoto:

```bash
git remote add origin https://github.com/daybison-br/inspectteam.git
git push -u origin <branch>
```

Se o remoto `origin` já existir com essa URL, não adicioná-lo novamente. Se o envio falhar por autenticação, conflito, indisponibilidade ou falta de permissão, não ocultar a falha: informar o bloqueio e preservar o commit local.

Nunca enviar arquivos ignorados, credenciais, tokens, chaves privadas, dumps de banco ou configurações locais com segredos.
