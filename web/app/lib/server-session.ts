import {cookies,headers} from "next/headers";

const API=process.env.API_INTERNAL_URL??"http://127.0.0.1:8080";
const accessName="inspecteam_access";
const refreshName="inspecteam_refresh";

type TokenSet={accessToken:string;accessTokenExpiresAt:string;refreshToken:string;refreshTokenExpiresAt:string};
type AuthPayload={userId:string;tenantId?:string;membershipId?:string;tokens:TokenSet};

export async function apiRequest(path:string,init:RequestInit={}){
 const jar=await cookies();
 const access=readCookie(jar.get(accessName)?.value);
 const refreshToken=readCookie(jar.get(refreshName)?.value);
 let response=await call(path,init,access);
 let sessionHeaders:string[]=[];
 if(response.status===401&&refreshToken){
  const refreshed=await refresh(refreshToken);
  sessionHeaders=refreshed.cookies;
  if(refreshed.accessToken)response=await call(path,init,refreshed.accessToken);
 }
 return forwardResponse(response,sessionHeaders);
}

async function call(path:string,init:RequestInit,access?:string){
 const requestHeaders=new Headers(init.headers);
 if(access)requestHeaders.set("Authorization",`Bearer ${access}`);
 if(!requestHeaders.has("Content-Type")&&init.body)requestHeaders.set("Content-Type","application/json");
 return fetch(`${API}${path}`,{...init,headers:requestHeaders,cache:"no-store"});
}

export async function establish(endpoint:"login"|"register-tenant",body:unknown){
 const response=await fetch(`${API}/api/v1/auth/${endpoint}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body),cache:"no-store"});
 if(!response.ok)return forwardResponse(response);
 const payload=await response.json() as AuthPayload;
 return jsonResponse({userId:payload.userId,tenantId:payload.tenantId,membershipId:payload.membershipId},200,sessionCookies(payload.tokens));
}

export async function clearSession(){
 const jar=await cookies();
 const refreshToken=readCookie(jar.get(refreshName)?.value);
 if(refreshToken)await fetch(`${API}/api/v1/auth/logout`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken}),cache:"no-store"}).catch(()=>undefined);
 return emptyResponse(204,expiredSessionCookies());
}

export async function verifyOrigin(request:Request){
 if(["GET","HEAD","OPTIONS"].includes(request.method))return true;
 const origin=request.headers.get("origin");if(!origin)return true;
 const requestHeaders=await headers();
 const host=requestHeaders.get("x-forwarded-host")??requestHeaders.get("host");
 return !!host&&new URL(origin).host===host;
}

async function refresh(refreshToken:string):Promise<{accessToken?:string;cookies:string[]}>
{
 const response=await fetch(`${API}/api/v1/auth/refresh`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken,deviceName:"InspecTeam Web"}),cache:"no-store"});
 if(!response.ok)return{cookies:expiredSessionCookies()};
 const tokens=await response.json() as TokenSet;
 return{accessToken:tokens.accessToken,cookies:sessionCookies(tokens)};
}

function sessionCookies(tokens:TokenSet){
 return[
  serializeCookie(accessName,tokens.accessToken,new Date(tokens.accessTokenExpiresAt)),
  serializeCookie(refreshName,tokens.refreshToken,new Date(tokens.refreshTokenExpiresAt)),
 ];
}

function expiredSessionCookies(){
 const expired=new Date(0);
 return[serializeCookie(accessName,"",expired,0),serializeCookie(refreshName,"",expired,0)];
}

function serializeCookie(name:string,value:string,expires:Date,maxAge?:number){
 const secure=process.env.NODE_ENV==="production"?"; Secure":"";
 const age=maxAge===undefined?"":`; Max-Age=${maxAge}`;
 return`${name}=${encodeURIComponent(value)}; Path=/; HttpOnly; SameSite=Lax; Expires=${expires.toUTCString()}${age}${secure}`;
}

function readCookie(value?:string){
 if(!value)return undefined;
 try{return decodeURIComponent(value)}catch{return value}
}

function responseHeaders(upstream?:Response,setCookies:string[]=[]){
 const result=new Headers({"Cache-Control":"no-store"});
 for(const name of ["content-type","content-disposition","etag","location","retry-after","www-authenticate"]){
  const value=upstream?.headers.get(name);if(value)result.set(name,value);
 }
 for(const cookie of setCookies)result.append("Set-Cookie",cookie);
 return result;
}

function forwardResponse(upstream:Response,setCookies:string[]=[]){
 return new Response(upstream.body,{status:upstream.status,statusText:upstream.statusText,headers:responseHeaders(upstream,setCookies)});
}

function jsonResponse(payload:unknown,status:number,setCookies:string[]=[]){
 const outgoingHeaders=responseHeaders(undefined,setCookies);
 outgoingHeaders.set("Content-Type","application/json; charset=utf-8");
 return new Response(JSON.stringify(payload),{status,headers:outgoingHeaders});
}

function emptyResponse(status:number,setCookies:string[]=[]){
 return new Response(null,{status,headers:responseHeaders(undefined,setCookies)});
}
