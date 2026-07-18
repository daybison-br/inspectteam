/* eslint-disable @typescript-eslint/no-explicit-any */
export class ApiError extends Error { constructor(message:string, public status:number, public fields?:Record<string,string>){super(message);} }
export async function api<T>(path:string, init:RequestInit={}):Promise<T>{
  const response=await fetch(`/gateway/${path.replace(/^\//,"")}`,{...init,headers:{"Content-Type":"application/json",...init.headers}});
  if(response.status===401){location.href=`/login?returnTo=${encodeURIComponent(location.pathname)}`;throw new ApiError("Sessão expirada",401);}
  if(!response.ok){let data:any={};try{data=await response.json();}catch{}throw new ApiError(data.detail??"Não foi possível concluir a operação",response.status,data.errors);}
  return response.status===204?undefined as T:response.json();
}
export const json=(method:string,body:unknown):RequestInit=>({method,body:JSON.stringify(body)});
export function formatDate(value?:string|null){return value?new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short"}).format(new Date(value)):"—";}
