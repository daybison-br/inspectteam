import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
const REFRESH_TOKEN = 'inspecteam.refresh-token';
const DEVICE_ID = 'inspecteam.device-id';
const DATABASE_KEY = 'inspecteam.database-key';
export const sessionStore = {
  getRefreshToken: () => SecureStore.getItemAsync(REFRESH_TOKEN),
  setRefreshToken: (value: string) => SecureStore.setItemAsync(REFRESH_TOKEN, value, { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY }),
  clearRefreshToken: () => SecureStore.deleteItemAsync(REFRESH_TOKEN),
  async getDeviceId() {
    const current = await SecureStore.getItemAsync(DEVICE_ID);
    if (current) return current;
    const value = Crypto.randomUUID();
    await SecureStore.setItemAsync(DEVICE_ID, value);
    return value;
  },
  async getDatabaseKey() {
    const current = await SecureStore.getItemAsync(DATABASE_KEY);
    if (current) return current;
    const bytes = await Crypto.getRandomBytesAsync(32);
    const value = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
    await SecureStore.setItemAsync(DATABASE_KEY, value, { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY });
    return value;
  },
};
