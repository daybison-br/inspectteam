import Constants from 'expo-constants';
import { sessionStore } from '../security/session';
import type { LoginResult, TokenPair } from './contracts';

const configured = process.env.EXPO_PUBLIC_API_URL || Constants.expoConfig?.extra?.apiUrl;
export const API_URL = String(configured || 'http://10.0.2.2:8080/api/v1').replace(/\/$/, '');
let accessToken: string | null = null;

async function refresh(): Promise<boolean> {
  const refreshToken = await sessionStore.getRefreshToken();
  if (!refreshToken) return false;
  const response = await fetch(API_URL + '/auth/refresh', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken, deviceName: 'InspecTeam Mobile' }) });
  if (!response.ok) { await clearSessionTokens(); return false; }
  await setSessionTokens(await response.json() as TokenPair);
  return true;
}
export async function setSessionTokens(tokens: TokenPair) {
  accessToken = tokens.accessToken;
  await sessionStore.setRefreshToken(tokens.refreshToken);
}
export async function clearSessionTokens() {
  accessToken = null;
  await sessionStore.clearRefreshToken();
}
export async function apiRequest<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', 'Bearer ' + accessToken);
  const response = await fetch(API_URL + path, { ...init, headers });
  if (response.status === 401 && retry && await refresh()) return apiRequest<T>(path, init, false);
  if (!response.ok) {
    let message = 'Não foi possível concluir a operação.';
    try { const problem = await response.json(); message = problem.detail || problem.title || message; } catch { /* resposta sem JSON */ }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export async function login(email: string, password: string) {
  const response = await fetch(API_URL + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password, deviceName: 'InspecTeam Mobile' }) });
  if (!response.ok) throw new Error('E-mail ou senha inválidos.');
  const result = await response.json() as LoginResult;
  await setSessionTokens(result.tokens);
  return result;
}
export async function logout() {
  const refreshToken = await sessionStore.getRefreshToken();
  if (refreshToken) {
    try { await fetch(API_URL + '/auth/logout', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }) }); } catch { /* revogação será concluída pela expiração no servidor */ }
  }
  await clearSessionTokens();
}export const restoreSession = refresh;
