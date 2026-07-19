import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';
import { useAuth } from '@/features/auth/AuthContext';
import { useTheme } from '@/core/design-system/theme';

export default function Index() {
  const auth = useAuth(); const theme = useTheme();
  if (auth.loading) return <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: theme.canvas }}><ActivityIndicator color={theme.accent600}/></View>;
  return <Redirect href={auth.account ? '/(app)/formularios' : '/login'}/>;
}
