import { cookies, headers } from "next/headers";

const API = process.env.API_INTERNAL_URL ?? "http://127.0.0.1:8080";
const accessName = "inspecteam_access";
const refreshName = "inspecteam_refresh";

export async function apiRequest(path: string, init: RequestInit = {}) {
  const jar = await cookies();
  const access = jar.get(accessName)?.value;
  let response = await call(path, init, access);
  if (response.status === 401 && jar.get(refreshName)?.value) {
    const refreshed = await refresh();
    if (refreshed) response = await call(path, init, refreshed);
  }
  return response;
}

async function call(path: string, init: RequestInit, access?: string) {
  const requestHeaders = new Headers(init.headers);
  if (access) requestHeaders.set("Authorization", `Bearer ${access}`);
  if (!requestHeaders.has("Content-Type") && init.body) requestHeaders.set("Content-Type", "application/json");
  return fetch(`${API}${path}`, { ...init, headers: requestHeaders, cache: "no-store" });
}

export async function establish(endpoint: "login" | "register-tenant", body: unknown) {
  const response = await fetch(`${API}/api/v1/auth/${endpoint}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body), cache: "no-store" });
  if (!response.ok) return response;
  const payload = await response.json();
  await setTokens(payload.tokens);
  return Response.json({ userId: payload.userId, tenantId: payload.tenantId, membershipId: payload.membershipId });
}

export async function clearSession() {
  const jar = await cookies(); const refreshToken = jar.get(refreshName)?.value;
  if (refreshToken) await fetch(`${API}/api/v1/auth/logout`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }) }).catch(() => undefined);
  jar.delete(accessName); jar.delete(refreshName);
}

export async function verifyOrigin(request: Request) {
  if (["GET", "HEAD", "OPTIONS"].includes(request.method)) return true;
  const origin = request.headers.get("origin"); if (!origin) return true;
  const h = await headers(); const host = h.get("x-forwarded-host") ?? h.get("host");
  return !!host && new URL(origin).host === host;
}

async function refresh() {
  const jar = await cookies(); const refreshToken = jar.get(refreshName)?.value; if (!refreshToken) return null;
  const response = await fetch(`${API}/api/v1/auth/refresh`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken, deviceName: "InspecTeam Web" }), cache: "no-store" });
  if (!response.ok) { jar.delete(accessName); jar.delete(refreshName); return null; }
  const tokens = await response.json(); await setTokens(tokens); return tokens.accessToken as string;
}

async function setTokens(tokens: { accessToken: string; accessTokenExpiresAt: string; refreshToken: string; refreshTokenExpiresAt: string }) {
  const jar = await cookies(); const secure = process.env.NODE_ENV === "production";
  jar.set(accessName, tokens.accessToken, { httpOnly: true, sameSite: "lax", secure, path: "/", expires: new Date(tokens.accessTokenExpiresAt) });
  jar.set(refreshName, tokens.refreshToken, { httpOnly: true, sameSite: "lax", secure, path: "/", expires: new Date(tokens.refreshTokenExpiresAt) });
}
