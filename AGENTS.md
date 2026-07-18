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
