"use client";

import { useMemo, useState } from "react";

type FieldType = "text" | "select" | "checkbox" | "photo" | "signature";
type Field = { id: string; type: FieldType; label: string; required: boolean };

const options: Array<{ type: FieldType; label: string; icon: string }> = [
  { type: "text", label: "Texto", icon: "T" },
  { type: "select", label: "Seleção", icon: "⌄" },
  { type: "checkbox", label: "Checklist", icon: "✓" },
  { type: "photo", label: "Foto", icon: "▣" },
  { type: "signature", label: "Assinatura", icon: "✎" },
];

const initialFields: Field[] = [
  { id: "plate", type: "text", label: "Placa do veículo", required: true },
  { id: "condition", type: "select", label: "Condição geral", required: true },
  { id: "photos", type: "photo", label: "Fotos antes da remoção", required: true },
  { id: "signature", type: "signature", label: "Assinatura do responsável", required: false },
];

export default function Home() {
  const [fields, setFields] = useState(initialFields);
  const [selectedId, setSelectedId] = useState(initialFields[0].id);
  const [published, setPublished] = useState(false);
  const selected = useMemo(() => fields.find((field) => field.id === selectedId), [fields, selectedId]);

  function addField(type: FieldType, label: string) {
    const field = { id: crypto.randomUUID(), type, label: `Novo campo de ${label.toLowerCase()}`, required: false };
    setFields((current) => [...current, field]);
    setSelectedId(field.id);
    setPublished(false);
  }

  function updateSelected(patch: Partial<Field>) {
    setFields((current) => current.map((field) => field.id === selectedId ? { ...field, ...patch } : field));
    setPublished(false);
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">I</span><span>InspecTeam</span></div>
        <div className="tenant-switcher"><span className="tenant-avatar">GC</span><div><strong>Guincho Central</strong><small>Plano profissional</small></div><span>⌄</span></div>
        <nav aria-label="Navegação principal">
          <a href="#overview"><span>⌂</span>Visão geral</a>
          <a className="active" href="#forms"><span>▤</span>Formulários</a>
          <a href="#submissions"><span>◎</span>Respostas</a>
          <a href="#team"><span>♙</span>Equipe</a>
          <a href="#access"><span>◇</span>Acessos</a>
        </nav>
        <div className="sidebar-bottom"><a href="#settings"><span>⚙</span>Configurações</a><div className="user-card"><span className="user-avatar">RM</span><div><strong>Rafael Martins</strong><small>Proprietário</small></div></div></div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div><p className="eyebrow">FORMULÁRIOS / EDITOR</p><h1>Checklist de remoção</h1></div>
          <div className="header-actions"><span className={`status ${published ? "published" : ""}`}><i />{published ? "Publicado" : "Rascunho"}</span><button className="ghost">Pré-visualizar</button><button className="primary" onClick={() => setPublished(true)}>Publicar versão</button></div>
        </header>

        <div className="editor-layout">
          <aside className="field-palette">
            <div><p className="eyebrow">CAMPOS</p><h2>Adicionar ao formulário</h2></div>
            <div className="palette-grid">{options.map((option) => <button key={option.type} onClick={() => addField(option.type, option.label)}><span>{option.icon}</span>{option.label}</button>)}</div>
            <div className="tip"><strong>Pronto para o campo</strong><p>Este formulário será baixado no celular e poderá ser preenchido sem internet.</p></div>
          </aside>

          <section className="canvas" id="forms">
            <div className="form-heading"><div><span className="form-icon">▤</span><div><h2>Checklist de remoção</h2><p>Registre as condições do veículo antes do transporte.</p></div></div><span>{fields.length} campos</span></div>
            <div className="section-card">
              <div className="section-title"><div><span>01</span><div><h3>Dados da ocorrência</h3><p>Informações obrigatórias para iniciar o atendimento</p></div></div><button aria-label="Mais opções">•••</button></div>
              <div className="fields-list">{fields.map((field, index) => <button className={`field-card ${selectedId === field.id ? "selected" : ""}`} key={field.id} onClick={() => setSelectedId(field.id)}><span className="drag">⠿</span><span className="field-number">{String(index + 1).padStart(2, "0")}</span><div><strong>{field.label}</strong><small>{options.find((item) => item.type === field.type)?.label}{field.required ? " · Obrigatório" : " · Opcional"}</small></div><span className="field-type">{options.find((item) => item.type === field.type)?.icon}</span></button>)}</div>
              <button className="add-section" onClick={() => addField("text", "Texto")}>＋ Adicionar campo</button>
            </div>
          </section>

          <aside className="properties">
            <div><p className="eyebrow">PROPRIEDADES</p><h2>Configurar campo</h2></div>
            {selected ? <div className="property-form"><label>Rótulo do campo<input value={selected.label} onChange={(event) => updateSelected({ label: event.target.value })} /></label><label>Tipo de campo<select value={selected.type} onChange={(event) => updateSelected({ type: event.target.value as FieldType })}>{options.map((option) => <option value={option.type} key={option.type}>{option.label}</option>)}</select></label><label className="toggle-row"><div><strong>Campo obrigatório</strong><small>Impede finalizar sem resposta</small></div><input type="checkbox" checked={selected.required} onChange={(event) => updateSelected({ required: event.target.checked })} /></label><button className="danger" onClick={() => { setFields((current) => current.filter((field) => field.id !== selected.id)); setSelectedId(""); }}>Remover campo</button></div> : <p className="empty">Selecione um campo no formulário.</p>}
          </aside>
        </div>
      </section>
    </main>
  );
}
