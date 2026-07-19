import {apiRequest,verifyOrigin} from "@/app/lib/server-session";
type Context={params:Promise<{path:string[]}>};
async function proxy(request:Request,context:Context){
 if(!(await verifyOrigin(request)))return new Response("Origem inválida",{status:403});
 const{path}=await context.params;const source=new URL(request.url);
 const body=["GET","HEAD"].includes(request.method)?undefined:await request.text();
 return apiRequest(`/api/v1/${path.join("/")}${source.search}`,{method:request.method,body,headers:{"Content-Type":request.headers.get("content-type")??"application/json"}});
}
export const GET=proxy;export const POST=proxy;export const PATCH=proxy;export const PUT=proxy;export const DELETE=proxy;
