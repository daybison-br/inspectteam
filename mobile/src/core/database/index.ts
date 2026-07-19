import * as SQLite from 'expo-sqlite';
import { drizzle } from 'drizzle-orm/expo-sqlite';
import * as schema from './schema';
import { sessionStore } from '../security/session';

export const sqlite = SQLite.openDatabaseSync('inspecteam.db');
export const database = drizzle(sqlite, { schema });

export async function initializeDatabase() {
  const key = await sessionStore.getDatabaseKey();
  await sqlite.execAsync("PRAGMA key = '" + key + "'; PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON;");
  const version = await sqlite.getFirstAsync<{ user_version: number }>('PRAGMA user_version');
  if ((version?.user_version ?? 0) >= 1) return;
  await sqlite.execAsync(`
    CREATE TABLE IF NOT EXISTS local_session (id INTEGER PRIMARY KEY CHECK(id=1), user_json TEXT NOT NULL, tenant_id TEXT, last_online_at TEXT);
    CREATE TABLE IF NOT EXISTS tenants (tenant_id TEXT PRIMARY KEY, membership_id TEXT NOT NULL, name TEXT NOT NULL, slug TEXT NOT NULL, membership_type TEXT NOT NULL, status TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS forms (form_id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, name TEXT NOT NULL, description TEXT, version_id TEXT NOT NULL, version INTEGER NOT NULL, definition_json TEXT NOT NULL, published_at TEXT, deleted INTEGER NOT NULL DEFAULT 0);
    CREATE INDEX IF NOT EXISTS idx_forms_tenant ON forms(tenant_id, deleted, name);
    CREATE TABLE IF NOT EXISTS submissions (id TEXT PRIMARY KEY, tenant_id TEXT NOT NULL, form_id TEXT NOT NULL, form_version_id TEXT NOT NULL, status TEXT NOT NULL, answers_json TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL, updated_at TEXT NOT NULL);
    CREATE INDEX IF NOT EXISTS idx_submissions_tenant ON submissions(tenant_id, status, updated_at DESC);
    CREATE TABLE IF NOT EXISTS attachments (id TEXT PRIMARY KEY, submission_id TEXT NOT NULL REFERENCES submissions(id), field_id TEXT NOT NULL, uri TEXT NOT NULL, name TEXT NOT NULL, mime TEXT NOT NULL, size INTEGER, status TEXT NOT NULL, remote_json TEXT);
    CREATE TABLE IF NOT EXISTS mutation_queue (id TEXT PRIMARY KEY, mutation_id TEXT NOT NULL UNIQUE, tenant_id TEXT NOT NULL, submission_id TEXT NOT NULL, operation TEXT NOT NULL, status TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, error TEXT, created_at TEXT NOT NULL);
    CREATE INDEX IF NOT EXISTS idx_mutations_pending ON mutation_queue(tenant_id, status, created_at);
    CREATE TABLE IF NOT EXISTS sync_state (tenant_id TEXT PRIMARY KEY, cursor TEXT, last_synced_at TEXT);
    PRAGMA user_version = 1;
  `);
}
