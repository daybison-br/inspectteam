import test from "node:test";import assert from "node:assert/strict";import {readFile} from "node:fs/promises";
const read=(file)=>readFile(new URL(`../${file}`,import.meta.url),"utf8");
test("aplicação expõe autenticação e áreas funcionais",async()=>{const workspace=await read("app/components/Workspace.tsx");const auth=await read("app/components/AuthForm.tsx");for(const label of ["Formulários","Respostas","Equipe","Acessos","Configurações","Administração global"])assert.match(workspace,new RegExp(label));assert.match(auth,/gateway\/auth/);});
test("sessão mantém tokens em cookies protegidos e headers mutáveis",async()=>{const session=await read("app/lib/server-session.ts");assert.match(session,/HttpOnly/);assert.match(session,/SameSite=Lax/);assert.match(session,/NODE_ENV==="production"/);assert.match(session,/append\("Set-Cookie"/);assert.match(session,/Max-Age=/);assert.doesNotMatch(session,/jar\.(?:set|delete)\(/);assert.doesNotMatch(await read("app/components/Workspace.tsx"),/localStorage\.setItem\([^,]*token/i);});
test("construtor suporta todos os tipos validados pela API",async()=>{const editor=await read("app/components/FormEditor.tsx");for(const type of ["text","textarea","number","date","time","select","multiselect","checkbox","photo","signature","heading","instructions"])assert.match(editor,new RegExp(`['\"]${type}['\"]`));});
test("senha temporária bloqueia a aplicação até a redefinição",async()=>{const modal=await read("app/components/ForcedPasswordModal.tsx");const workspace=await read("app/components/Workspace.tsx");assert.match(workspace,/mustChangePassword&&<ForcedPasswordModal/);assert.match(modal,/aria-modal="true"/);assert.match(modal,/me\/password/);assert.match(modal,/confirmPassword/);assert.doesNotMatch(modal,/onMouseDown|Fechar|Cancelar/);});
test("design system corporativo é centralizado e acessível",async()=>{const design=await read("app/components/DesignSystem.tsx");const styles=await read("app/styles/foundation.css");const workspace=await read("app/components/Workspace.tsx");assert.match(design,/@fluentui\/react-icons/);for(const token of ["--surface","--text-secondary","--focus","--accent-600"])assert.match(styles,new RegExp(token));assert.match(workspace,/Auditoria/);assert.doesNotMatch(workspace,/\bconfirm\(|\balert\(/);});

test("efeitos de carregamento não retornam Promise como cleanup",async()=>{for(const file of ["app/components/Workspace.tsx","app/components/AccessPanel.tsx"]){const source=await read(file);assert.doesNotMatch(source,/useEffect\s*\(\s*load\s*,/);assert.doesNotMatch(source,/useEffect\s*\(\s*async\b/);}const boundary=await read("app/error.tsx");assert.match(boundary,/reset/);assert.match(boundary,/Tentar novamente/);});

test("Vite filtra somente sourcemaps inválidos do Griffel",async()=>{const config=await read("vite.config.ts");assert.match(config,/createLogger/);assert.match(config,/node_modules\/@griffel/);assert.match(config,/outside its package/);assert.doesNotMatch(config,/logLevel\s*:\s*["']silent/);});
test("gateway preserva cookies em login, refresh e logout",async()=>{const session=await read("app/lib/server-session.ts");const proxy=await read("app/gateway/[...path]/route.ts");const logout=await read("app/gateway/auth/logout/route.ts");assert.match(session,/sessionCookies\(payload\.tokens\)/);assert.match(session,/sessionHeaders=refreshed\.cookies/);assert.match(session,/expiredSessionCookies\(\)/);assert.match(proxy,/return apiRequest\(/);assert.doesNotMatch(proxy,/new Headers\(\)/);assert.match(logout,/return clearSession\(\)/);});
test("perfil possui página própria no menu do avatar",async()=>{const workspace=await read("app/components/Workspace.tsx");const profile=await read("app/components/ProfileSettings.tsx");const settings=workspace.slice(workspace.indexOf("function Settings"),workspace.indexOf("function AdminContent"));assert.match(workspace,/<details className="user-menu">/);assert.doesNotMatch(workspace,/className="sidebar-user"/);assert.match(workspace,/aria-label="Abrir menu do usuário"/);assert.match(workspace,/href="\/app\/perfil"/);assert.match(workspace,/<strong>Meu perfil<\/strong>/);assert.match(workspace,/loggingOut\?"Saindo\.\.\.":"Sair"/);assert.match(workspace,/isProfile\?<ProfilePage/);assert.doesNotMatch(settings,/ProfileSettings/);assert.match(profile,/api<Profile>\("me"/);assert.match(profile,/json\("PATCH"/);});

test("formulários publicados podem ser preenchidos na web e excluídos logicamente",async()=>{
 const workspace=await read("app/components/Workspace.tsx");
 const runner=await read("app/components/FormRunner.tsx");
 for(const endpoint of ["/available","/published","submissions","upload-sessions","/complete"])assert.match(workspace+runner,new RegExp(endpoint.replace("/","\\/")));
 assert.match(workspace,/form\.id\}\/usar/);
 for(const label of ["Usar formulário","Editar formulário","Arquivar","Excluir"])assert.match(workspace,new RegExp(label));
 assert.match(workspace,/method:"DELETE"/);
 assert.match(workspace,/exclusão lógica/);
 for(const type of ["text","textarea","number","date","time","select","multiselect","checkbox","photo","signature","heading","instructions"])assert.match(runner,new RegExp('"' + type + '"'));
 assert.match(runner,/aria-live="polite"/);
 assert.match(runner,/<canvas/);
 assert.match(runner,/Alternativa por teclado/);
 assert.match(runner,/assinatura\.png/);
});

test("editor de opções usa lista acessível com inclusão, ordenação e validação",async()=>{
 const editor=await read("app/components/FormEditor.tsx");
 const options=await read("app/components/OptionsEditor.tsx");
 assert.match(editor,/OptionsEditor/);
 assert.doesNotMatch(editor,/<label>Opções<textarea/);
 assert.match(editor,/requestPublish/);
 assert.match(editor,/validateChoiceOptions/);
 assert.match(editor,/item\.type==="select"\?<select/);
 assert.match(editor,/item\.type==="multiselect"\?<select multiple/);
 assert.match(options,/Adicionar opção/);
 assert.match(options,/event\.key==="Enter"/);
 assert.match(options,/event\.key==="Backspace"/);
 assert.match(options,/Mover opção/);
 assert.match(options,/Remover opção/);
 assert.match(options,/aria-live="polite"/);
 assert.match(options,/normalize\("NFD"\)/);
 assert.match(options,/Adicione pelo menos duas opções/);
});
test("menu do construtor diferencia visualmente cada tipo de campo",async()=>{
 const editor=await read("app/components/FormEditor.tsx");
 const design=await read("app/components/DesignSystem.tsx");
 for(const icon of ["fieldText","fieldLongText","fieldNumber","fieldDate","fieldTime","fieldSelect","fieldMultiSelect","fieldCheckbox","fieldPhoto","fieldSignature","fieldHeading","fieldInstructions"]){
  assert.match(editor,new RegExp("Icons\\."+icon));
  assert.match(design,new RegExp(icon+":"));
 }
 assert.match(editor,/types\.map\(\(\[type,label,FieldIcon\]\)/);
 assert.doesNotMatch(editor,/<Icons\.add\/>\{label\}/);
});
test("construtor cria, seleciona e reorganiza seções de forma acessível",async()=>{
 const editor=await read("app/components/FormEditor.tsx");
 const styles=await read("app/styles/builder.css");
 assert.match(editor,/activeSectionId/);
 assert.match(editor,/function addSection/);
 assert.match(editor,/function moveSection/);
 assert.match(editor,/className="add-section-card"/);
 assert.match(editor,/Adicionar campo em/);
 assert.match(editor,/aria-live="polite"/);
 assert.match(editor,/s\.id===targetId/);
 assert.match(editor,/disabled=\{sectionIndex===0\}/);
 assert.match(editor,/disabled=\{sectionIndex===sections\.length-1\}/);
 assert.doesNotMatch(editor,/className="section-add"/);
 assert.match(styles,/border:2px dashed/);
 assert.match(styles,/\.section-card\.active/);
 assert.match(styles,/width:44px;min-height:44px/);
});
test("preenchimento web usa etapas, revisão e captura profissional de foto",async()=>{
 const runner=await read("app/components/FormRunner.tsx");
 const styles=await read("app/styles/runner.css");
 for(const behavior of ["sectionIndex","runner-progress","role=\"progressbar\"","continueForm","previousSection","Revise suas respostas","focusField","submitPhase"])assert.match(runner,new RegExp(behavior));
 assert.match(runner,/className="runner-choice-group"/);
 assert.match(runner,/className={"photo-capture/);
 assert.match(runner,/capture="environment"/);
 assert.match(runner,/accept="image\/\*"/);
 assert.match(runner,/URL\.createObjectURL/);
 assert.match(runner,/URL\.revokeObjectURL/);
 assert.match(runner,/A imagem deve ter no máximo 20 MB/);
 assert.match(runner,/Substituir foto/);
 assert.match(runner,/uploadedFiles/);
 assert.match(runner,/submissionCreated/);
 assert.match(styles,/\.runner-hero/);
 assert.match(styles,/\.photo-preview/);
 assert.match(styles,/\.runner-review/);
 assert.match(styles,/min-height:44px/);
});
