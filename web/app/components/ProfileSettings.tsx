"use client";
import {FormEvent,useEffect,useState} from "react";
import {api,json} from "@/app/lib/api";
import {Icons,Notice} from "./DesignSystem";

type Profile={id:string;email:string;displayName:string;platformAdmin:boolean;mustChangePassword:boolean};

export default function ProfileSettings({onUpdated}:{onUpdated?:(profile:Profile)=>void}){
 const[me,setMe]=useState<Profile>(),[displayName,setDisplayName]=useState(""),[loading,setLoading]=useState(true),[saving,setSaving]=useState(false),[saved,setSaved]=useState(false),[error,setError]=useState("");
 useEffect(()=>{api<Profile>("me").then(profile=>{setMe(profile);setDisplayName(profile.displayName)}).catch(reason=>setError(reason.message)).finally(()=>setLoading(false))},[]);
 async function save(event:FormEvent<HTMLFormElement>){
  event.preventDefault();setSaving(true);setSaved(false);setError("");
  try{const updated=await api<Profile>("me",json("PATCH",{displayName}));setMe(updated);setDisplayName(updated.displayName);onUpdated?.(updated);setSaved(true)}
  catch(reason){setError(reason instanceof Error?reason.message:"Não foi possível atualizar o perfil.")}
  finally{setSaving(false)}
 }
 const initials=(me?.displayName||displayName||"U").slice(0,2).toUpperCase();
 return <form className="panel profile-card" onSubmit={save} aria-busy={saving}>
  <div className="profile-card-head"><span className="avatar profile-avatar" aria-hidden="true">{initials}</span><div><span className="eyebrow">Conta pessoal</span><h2>Dados pessoais</h2><p className="muted">Seu nome identifica suas ações, respostas e registros de auditoria.</p></div></div>
  {saved&&<Notice tone="success">Perfil atualizado.</Notice>}
  {error&&<Notice tone="danger">{error}</Notice>}
  {loading?<div className="profile-loading" role="status"><span className="loader"/><span>Carregando seus dados...</span></div>:<><label>Nome completo<input name="displayName" value={displayName} onChange={event=>{setDisplayName(event.target.value);setSaved(false)}} autoComplete="name" required disabled={saving}/></label><label>E-mail<input value={me?.email??""} autoComplete="email" disabled/><small className="field-help">O e-mail é usado para entrar na aplicação e não pode ser alterado por esta tela.</small></label><div className="profile-actions"><button className="primary" disabled={saving||!displayName.trim()}><Icons.save/>{saving?"Salvando...":"Salvar perfil"}</button></div></>}
 </form>
}
