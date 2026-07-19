import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { apiRequest, login as requestLogin, logout as requestLogout, restoreSession } from '@/core/api/client';
import type { Account, Tenant } from '@/core/api/contracts';
import { initializeDatabase } from '@/core/database';
import { clearUserData, listTenants, loadSession, saveSession, saveTenants } from '@/core/database/repository';

type AuthValue = {
  loading: boolean; account: Account | null; tenants: Tenant[]; tenantId?: string; offline: boolean;
  signIn(email: string, password: string): Promise<void>; signOut(): Promise<void>; selectTenant(id: string): Promise<void>;
  changeTemporaryPassword(currentPassword: string, nextPassword: string): Promise<void>;
};
const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(true);
  const [account, setAccount] = useState<Account | null>(null);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [tenantId, setTenantId] = useState<string>();
  const [offline, setOffline] = useState(false);

  async function loadOnline(selected?: string) {
    const [me, available] = await Promise.all([apiRequest<Account>('/me'), apiRequest<Tenant[]>('/tenants')]);
    const target = selected && available.some(item => item.tenantId === selected) ? selected : available[0]?.tenantId;
    setAccount(me); setTenants(available); setTenantId(target); setOffline(false);
    await saveTenants(available); await saveSession(me, target);
  }

  useEffect(() => {
    void (async () => {
      try {
        await initializeDatabase();
        const cached = await loadSession();
        if (cached) { setAccount(cached.account); setTenantId(cached.tenantId); setTenants(await listTenants()); setOffline(true); }
        if (await restoreSession()) await loadOnline(cached?.tenantId);
      } catch { /* cache local continua disponível */ }
      finally { setLoading(false); }
    })();
  }, []);

  async function signIn(email: string, password: string) {
    await requestLogin(email.trim().toLowerCase(), password);
    await loadOnline();
  }
  async function signOut() {
    await requestLogout(); await clearUserData();
    setAccount(null); setTenants([]); setTenantId(undefined); setOffline(false);
  }
  async function selectTenant(id: string) {
    setTenantId(id);
    if (account) await saveSession(account, id);
  }
  async function changeTemporaryPassword(currentPassword: string, nextPassword: string) {
    await apiRequest<void>('/me/password', { method: 'POST', body: JSON.stringify({ currentPassword, newPassword: nextPassword }) });
    if (account) { const updated = { ...account, mustChangePassword: false }; setAccount(updated); await saveSession(updated, tenantId); }
  }
  const value = { loading, account, tenants, tenantId, offline, signIn, signOut, selectTenant, changeTemporaryPassword };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('AuthProvider ausente');
  return value;
}
