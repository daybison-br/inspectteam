import { Stack } from 'expo-router';
import { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider } from '@/features/auth/AuthContext';
import { registerBackgroundSync } from '@/features/sync/background';

export default function RootLayout() {
  useEffect(() => { void registerBackgroundSync(); }, []);
  return <AuthProvider><StatusBar style="auto"/><Stack screenOptions={{ headerShown: false, animation: 'fade' }}/></AuthProvider>;
}
