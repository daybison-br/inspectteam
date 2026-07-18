import { establish, verifyOrigin } from "@/app/lib/server-session";
export async function POST(request: Request) { if (!(await verifyOrigin(request))) return new Response("Origem inválida", { status: 403 }); return establish("register-tenant", await request.json()); }
