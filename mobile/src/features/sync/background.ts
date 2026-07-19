import * as BackgroundTask from 'expo-background-task';
import * as TaskManager from 'expo-task-manager';
import { initializeDatabase } from '@/core/database';
import { loadSession } from '@/core/database/repository';
import { restoreSession } from '@/core/api/client';
import { synchronize } from './sync';

const TASK_NAME = 'inspecteam-background-sync';

TaskManager.defineTask(TASK_NAME, async () => {
  try {
    await initializeDatabase();
    const session = await loadSession();
    if (!session?.tenantId || !(await restoreSession())) return BackgroundTask.BackgroundTaskResult.Failed;
    await synchronize(session.tenantId);
    return BackgroundTask.BackgroundTaskResult.Success;
  } catch {
    return BackgroundTask.BackgroundTaskResult.Failed;
  }
});

export async function registerBackgroundSync() {
  const status = await BackgroundTask.getStatusAsync();
  if (status !== BackgroundTask.BackgroundTaskStatus.Available) return false;
  await BackgroundTask.registerTaskAsync(TASK_NAME, { minimumInterval: 15 });
  return true;
}
