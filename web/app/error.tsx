"use client";
import {useEffect} from "react";
import {Icons} from "./components/DesignSystem";
export default function GlobalError({error,reset}:{error:Error&{digest?:string};reset:()=>void}){useEffect(()=>{console.error("Falha na interface do InspecTeam",error)},[error]);return <main className="center-state" role="alert"><span className="empty-icon"><Icons.warning/></span><h1>Não foi possível abrir esta tela</h1><p className="muted">A navegação foi preservada. Tente carregar a tela novamente; se o problema continuar, informe o código abaixo ao suporte.</p>{error.digest&&<code>{error.digest}</code>}<button className="primary" onClick={reset}>Tentar novamente</button></main>}
