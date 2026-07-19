import { Platform } from 'react-native';
import { apiRequest } from '@/core/api/client';
import type { MutationResult } from '@/core/api/contracts';
import { applyMutationResult, applyTombstones, completeAttachment, getSyncCursor, pendingAttachments, pendingMutations, savePulledForms, setSyncCursor } from '@/core/database/repository';
import { sessionStore } from '@/core/security/session';

type ServerPull = { serverTime: string; nextCursor: number; forms: any[]; tombstones: { cursor: number; entityType: string; entityId: string; deletedAt: string }[] };

async function pushMutations(tenantId: string, deviceId: string, queue: any[]) {
  if (!queue.length) return;
  const results = await apiRequest<MutationResult[]>('/tenants/' + tenantId + '/sync/push', {
    method: 'POST',
    body: JSON.stringify({ deviceId, mutations: queue.map(item => ({
      mutationId: item.mutation_id, operation: item.operation, submissionId: item.submission_id,
      formId: item.form_id, formVersionId: item.form_version_id, revision: item.revision,
      answers: JSON.parse(item.answers_json), clientCreatedAt: item.submission_created_at,
    })) }),
  });
  for (const result of results) await applyMutationResult(result.mutationId, result.status, result.message);
}

export async function synchronize(tenantId: string) {
  const deviceId = await sessionStore.getDeviceId();
  await apiRequest<void>('/tenants/' + tenantId + '/sync/devices', {
    method: 'POST', body: JSON.stringify({ deviceId, name: 'InspecTeam Mobile', platform: Platform.OS.toUpperCase() }),
  });

  const cursor = await getSyncCursor(tenantId);
  const pull = await apiRequest<ServerPull>('/tenants/' + tenantId + '/sync/pull?cursor=' + cursor + '&deviceId=' + deviceId);
  await savePulledForms(tenantId, pull.forms.map(form => ({ ...form, tenantId })));
  await applyTombstones(pull.tombstones);
  await setSyncCursor(tenantId, pull.nextCursor);

  const initialQueue = await pendingMutations(tenantId);
  await pushMutations(tenantId, deviceId, initialQueue.filter(item => item.operation === 'CREATE' || item.operation === 'UPDATE'));

  const files = await pendingAttachments(tenantId);
  for (const file of files) {
    const localResponse = await fetch(file.uri);
    const body = await localResponse.blob();
    const upload = await apiRequest<{ fileId: string; objectKey: string; uploadUrl: string }>('/tenants/' + tenantId + '/files/upload-sessions', {
      method: 'POST', body: JSON.stringify({ formId: file.form_id, submissionId: file.submission_id, fieldId: file.field_id, originalName: file.name, contentType: file.mime, sizeBytes: file.size || body.size }),
    });
    const uploaded = await fetch(upload.uploadUrl, { method: 'PUT', headers: { 'Content-Type': file.mime }, body });
    if (!uploaded.ok) throw new Error('Falha ao enviar uma evidência.');
    await apiRequest<void>('/tenants/' + tenantId + '/files/' + upload.fileId + '/complete', { method: 'POST', body: JSON.stringify({ formId: file.form_id }) });
    await completeAttachment(file.id, file.field_id, file.submission_id, { fileId: upload.fileId, objectKey: upload.objectKey });
  }

  const finalQueue = (await pendingMutations(tenantId)).filter(item => item.operation === 'COMPLETE');
  await pushMutations(tenantId, deviceId, finalQueue);
  return { downloaded: pull.forms.length, sent: initialQueue.length, synchronizedAt: pull.serverTime };
}
