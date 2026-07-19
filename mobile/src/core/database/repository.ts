import * as Crypto from 'expo-crypto';
import type { Account, FormDefinition, PublishedForm, Tenant } from '../api/contracts';
import { sqlite } from './index';

export type LocalForm = PublishedForm;
export type LocalSubmission = {
  id: string; tenantId: string; formId: string; formVersionId: string;
  status: 'DRAFT' | 'READY' | 'SYNCED' | 'CONFLICT';
  answers: Record<string, unknown>; revision: number; createdAt: string; updatedAt: string;
};

export async function saveSession(account: Account, tenantId?: string) {
  await sqlite.runAsync('INSERT OR REPLACE INTO local_session(id,user_json,tenant_id,last_online_at) VALUES(1,?,?,?)', JSON.stringify(account), tenantId ?? null, new Date().toISOString());
}
export async function loadSession(): Promise<{ account: Account; tenantId?: string } | null> {
  const row = await sqlite.getFirstAsync<{ user_json: string; tenant_id: string | null }>('SELECT user_json,tenant_id FROM local_session WHERE id=1');
  return row ? { account: JSON.parse(row.user_json), tenantId: row.tenant_id ?? undefined } : null;
}
export async function clearUserData() { await sqlite.execAsync('DELETE FROM attachments; DELETE FROM mutation_queue; DELETE FROM submissions; DELETE FROM forms; DELETE FROM sync_state; DELETE FROM tenants; DELETE FROM local_session;'); }
export async function saveTenants(items: Tenant[]) {
  await sqlite.withTransactionAsync(async () => {
    for (const item of items) await sqlite.runAsync(
      'INSERT OR REPLACE INTO tenants(tenant_id,membership_id,name,slug,membership_type,status) VALUES(?,?,?,?,?,?)',
      item.tenantId, item.membershipId, item.name, item.slug, item.membershipType, item.membershipStatus);
  });
}
export async function listTenants() {
  return sqlite.getAllAsync<Tenant>('SELECT tenant_id tenantId,membership_id membershipId,name,slug,membership_type membershipType,status membershipStatus FROM tenants ORDER BY name');
}
export async function savePulledForms(tenantId: string, items: PublishedForm[]) {
  await sqlite.withTransactionAsync(async () => {
    await sqlite.runAsync('UPDATE forms SET deleted=1 WHERE tenant_id=?', tenantId);
    for (const item of items) await sqlite.runAsync(
      'INSERT OR REPLACE INTO forms(form_id,tenant_id,name,description,version_id,version,definition_json,published_at,deleted) VALUES(?,?,?,?,?,?,?,?,0)',
      item.formId, tenantId, item.name, item.description ?? null, item.versionId, item.version, JSON.stringify(item.definition), item.publishedAt ?? null);
  });
}
export async function applyTombstones(items: { entityType: string; entityId: string }[]) {
  for (const item of items) if (item.entityType === 'FORM') await sqlite.runAsync('UPDATE forms SET deleted=1 WHERE form_id=?', item.entityId);
}
export async function listForms(tenantId: string): Promise<LocalForm[]> {
  const rows = await sqlite.getAllAsync<any>('SELECT * FROM forms WHERE tenant_id=? AND deleted=0 ORDER BY name', tenantId);
  return rows.map(row => ({ formId: row.form_id, tenantId: row.tenant_id, name: row.name, description: row.description, versionId: row.version_id, version: row.version, definition: JSON.parse(row.definition_json) as FormDefinition, publishedAt: row.published_at }));
}
export async function findForm(formId: string): Promise<LocalForm | null> {
  const row = await sqlite.getFirstAsync<any>('SELECT * FROM forms WHERE form_id=? AND deleted=0', formId);
  return row ? { formId: row.form_id, tenantId: row.tenant_id, name: row.name, description: row.description, versionId: row.version_id, version: row.version, definition: JSON.parse(row.definition_json), publishedAt: row.published_at } : null;
}
export async function findDraft(formId: string) {
  const row = await sqlite.getFirstAsync<any>("SELECT * FROM submissions WHERE form_id=? AND status='DRAFT' ORDER BY updated_at DESC LIMIT 1", formId);
  return row ? mapSubmission(row) : null;
}
export async function saveDraft(input: Omit<LocalSubmission, 'id' | 'createdAt' | 'updatedAt' | 'revision' | 'status'> & { id?: string; answers: Record<string, unknown> }) {
  const now = new Date().toISOString();
  const current = input.id ? await sqlite.getFirstAsync<any>('SELECT * FROM submissions WHERE id=?', input.id) : null;
  const id = input.id ?? Crypto.randomUUID();
  await sqlite.runAsync(
    'INSERT OR REPLACE INTO submissions(id,tenant_id,form_id,form_version_id,status,answers_json,revision,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)',
    id, input.tenantId, input.formId, input.formVersionId, 'DRAFT', JSON.stringify(input.answers), current?.revision ?? 0, current?.created_at ?? now, now);
  return id;
}
export async function queueSubmission(id: string) {
  const submission = await sqlite.getFirstAsync<any>('SELECT * FROM submissions WHERE id=?', id);
  if (!submission) throw new Error('Rascunho não encontrado.');
  const now = new Date().toISOString();
  await sqlite.runAsync("DELETE FROM mutation_queue WHERE submission_id=? AND status!='APPLIED'", id);
  await sqlite.runAsync("UPDATE submissions SET status='READY',updated_at=? WHERE id=?", now, id);
  if (submission.revision === 0) {
    const createId = Crypto.randomUUID();
    await sqlite.runAsync("INSERT INTO mutation_queue(id,mutation_id,tenant_id,submission_id,operation,status,attempts,error,created_at) VALUES(?,?,?,?,?,'PENDING',0,NULL,?)", createId, createId, submission.tenant_id, id, 'CREATE', now);
  }
  const completeId = Crypto.randomUUID();
  await sqlite.runAsync("INSERT INTO mutation_queue(id,mutation_id,tenant_id,submission_id,operation,status,attempts,error,created_at) VALUES(?,?,?,?,?,'PENDING',0,NULL,?)", completeId, completeId, submission.tenant_id, id, 'COMPLETE', now);
}
export async function pendingMutations(tenantId: string) {
  return sqlite.getAllAsync<any>("SELECT q.*,s.form_id,s.form_version_id,s.revision,s.answers_json,s.created_at submission_created_at FROM mutation_queue q JOIN submissions s ON s.id=q.submission_id WHERE q.tenant_id=? AND q.status IN ('PENDING','CONFLICT') ORDER BY q.created_at, CASE q.operation WHEN 'CREATE' THEN 0 WHEN 'UPDATE' THEN 1 ELSE 2 END LIMIT 100", tenantId);
}
export async function applyMutationResult(mutationId: string, status: string, message?: string) {
  const row = await sqlite.getFirstAsync<{ submission_id: string; operation: string }>('SELECT submission_id,operation FROM mutation_queue WHERE mutation_id=?', mutationId);
  if (!row) return;
  if (status === 'APPLIED' || status === 'ALREADY_APPLIED') {
    await sqlite.runAsync("UPDATE mutation_queue SET status='APPLIED',error=NULL WHERE mutation_id=?", mutationId);
    const finalStatus = row.operation === 'COMPLETE' ? 'SYNCED' : 'READY';
    const revisionIncrement = row.operation === 'CREATE' ? 0 : 1;
    await sqlite.runAsync('UPDATE submissions SET status=?,revision=revision+? WHERE id=?', finalStatus, revisionIncrement, row.submission_id);
  } else {
    await sqlite.runAsync("UPDATE mutation_queue SET status=?,attempts=attempts+1,error=? WHERE mutation_id=?", status, message ?? null, mutationId);
    await sqlite.runAsync("UPDATE submissions SET status='CONFLICT' WHERE id=?", row.submission_id);
  }
}
export async function setSyncCursor(tenantId: string, cursor: number) {
  await sqlite.runAsync('INSERT OR REPLACE INTO sync_state(tenant_id,cursor,last_synced_at) VALUES(?,?,?)', tenantId, String(cursor), new Date().toISOString());
}
export async function getSyncCursor(tenantId: string) {
  const row = await sqlite.getFirstAsync<{ cursor: string | null }>('SELECT cursor FROM sync_state WHERE tenant_id=?', tenantId);
  return Number(row?.cursor ?? 0);
}
export async function saveAttachment(submissionId: string, fieldId: string, uri: string, name: string, mime: string, size?: number) {
  const id = Crypto.randomUUID();
  await sqlite.runAsync("DELETE FROM attachments WHERE submission_id=? AND field_id=? AND status!='UPLOADED'", submissionId, fieldId);
  await sqlite.runAsync("INSERT INTO attachments(id,submission_id,field_id,uri,name,mime,size,status) VALUES(?,?,?,?,?,?,?,'PENDING')", id, submissionId, fieldId, uri, name, mime, size ?? null);
  return id;
}
export async function pendingAttachments(tenantId: string) {
  return sqlite.getAllAsync<any>("SELECT a.*,s.tenant_id,s.form_id,s.answers_json FROM attachments a JOIN submissions s ON s.id=a.submission_id WHERE s.tenant_id=? AND a.status='PENDING' AND s.status!='CONFLICT'", tenantId);
}
export async function completeAttachment(id: string, fieldId: string, submissionId: string, remote: { fileId: string; objectKey: string }) {
  const row = await sqlite.getFirstAsync<{ answers_json: string }>('SELECT answers_json FROM submissions WHERE id=?', submissionId);
  if (!row) return;
  const answers = JSON.parse(row.answers_json);
  answers[fieldId] = remote;
  await sqlite.runAsync('UPDATE submissions SET answers_json=?,updated_at=? WHERE id=?', JSON.stringify(answers), new Date().toISOString(), submissionId);
  await sqlite.runAsync("UPDATE attachments SET status='UPLOADED',remote_json=? WHERE id=?", JSON.stringify(remote), id);
}
export async function listPending(tenantId: string) {
  const rows = await sqlite.getAllAsync<any>("SELECT * FROM submissions WHERE tenant_id=? AND status!='SYNCED' ORDER BY updated_at DESC", tenantId);
  return rows.map(mapSubmission);
}
function mapSubmission(row: any): LocalSubmission {
  return { id: row.id, tenantId: row.tenant_id, formId: row.form_id, formVersionId: row.form_version_id, status: row.status, answers: JSON.parse(row.answers_json), revision: row.revision, createdAt: row.created_at, updatedAt: row.updated_at };
}
