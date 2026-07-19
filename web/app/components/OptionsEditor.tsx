"use client";
import {useRef,useState} from "react";
import {Icons} from "./DesignSystem";

type Props={
 options:string[];
 onChange:(options:string[])=>void;
 showErrors?:boolean;
};

function normalized(value:string){
 return value.trim().normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLocaleLowerCase("pt-BR");
}

export function validateChoiceOptions(options:string[]){
 if(options.length<2)return false;
 const normalizedOptions=options.map(normalized);
 return normalizedOptions.every(Boolean)&&new Set(normalizedOptions).size===normalizedOptions.length;
}

export default function OptionsEditor({options,onChange,showErrors=false}:Props){
 const inputs=useRef<Array<HTMLInputElement|null>>([]),[announcement,setAnnouncement]=useState("");
 const normalizedOptions=options.map(normalized);
 const duplicateCounts=normalizedOptions.reduce<Record<string,number>>((counts,value)=>{if(value)counts[value]=(counts[value]??0)+1;return counts},{});
 const focus=(index:number)=>requestAnimationFrame(()=>inputs.current[index]?.focus());
 function update(index:number,value:string){const next=[...options];next[index]=value;onChange(next)}
 function add(after?:number){const index=after===undefined?options.length:after+1;const next=[...options];next.splice(index,0,"");onChange(next);setAnnouncement("Nova opção adicionada.");focus(index)}
 function remove(index:number){const next=options.filter((_,current)=>current!==index);onChange(next);setAnnouncement("Opção "+(index+1)+" removida.");focus(Math.max(0,index-1))}
 function move(index:number,direction:-1|1){const target=index+direction;if(target<0||target>=options.length)return;const next=[...options];[next[index],next[target]]=[next[target],next[index]];onChange(next);setAnnouncement("Opção movida para a posição "+(target+1)+".");focus(target)}
 function error(index:number){if(!showErrors)return "";if(!normalizedOptions[index])return "Informe um texto para esta opção.";if(duplicateCounts[normalizedOptions[index]]>1)return "Esta opção está duplicada.";return ""}
 return <fieldset className="options-editor"><legend>Opções</legend><p className="options-help">Defina as alternativas na ordem em que aparecerão no formulário.</p><div className="options-list">{options.map((option,index)=>{const message=error(index),errorId="option-"+index+"-error";return <div className={"option-item "+(message?"has-error":"")} key={index}><span className="option-number" aria-hidden="true">{index+1}</span><div className="option-input"><label className="sr-only" htmlFor={"option-"+index}>Opção {index+1}</label><input ref={element=>{inputs.current[index]=element}} id={"option-"+index} value={option} placeholder={"Opção "+(index+1)} aria-invalid={!!message} aria-describedby={message?errorId:undefined} onChange={event=>update(index,event.target.value)} onKeyDown={event=>{if(event.key==="Enter"){event.preventDefault();add(index)}else if(event.key==="Backspace"&&!option&&options.length>1){event.preventDefault();remove(index)}}}/>{message&&<small id={errorId} className="option-error" role="alert">{message}</small>}</div><div className="option-actions"><button type="button" className="icon-button" onClick={()=>move(index,-1)} disabled={index===0} aria-label={"Mover opção "+(index+1)+" para cima"} title="Mover para cima"><Icons.arrowUp/></button><button type="button" className="icon-button" onClick={()=>move(index,1)} disabled={index===options.length-1} aria-label={"Mover opção "+(index+1)+" para baixo"} title="Mover para baixo"><Icons.arrowDown/></button><button type="button" className="icon-button danger" onClick={()=>remove(index)} aria-label={"Remover opção "+(index+1)} title="Remover opção"><Icons.delete/></button></div></div>})}</div>{showErrors&&options.length<2&&<p className="options-summary-error" role="alert">Adicione pelo menos duas opções.</p>}<button type="button" className="add-option" onClick={()=>add()}><Icons.add/>Adicionar opção</button><span className="sr-only" aria-live="polite">{announcement}</span></fieldset>
}
