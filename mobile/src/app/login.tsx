import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';
import { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';
import { Button, Card, Field, Screen } from '@/shared/components/ui';
import { useTheme } from '@/core/design-system/theme';
import { useAuth } from '@/features/auth/AuthContext';

export default function Login() {
  const theme = useTheme(), auth = useAuth();
  const [email, setEmail] = useState(''), [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit() {
    if (!email || !password) { setError('Informe e-mail e senha.'); return; }
    setBusy(true); setError('');
    try { await auth.signIn(email, password); router.replace('/(app)/formularios'); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.'); }
    finally { setBusy(false); }
  }
  return <Screen><KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.center}>
    <View style={styles.brand}><View style={[styles.logo, { backgroundColor: theme.brand900 }]}><MaterialIcons name="fact-check" size={32} color="#fff"/></View>
      <Text style={[styles.name, { color: theme.text }]}>InspecTeam</Text><Text style={[styles.subtitle, { color: theme.textSecondary }]}>Checklists confiáveis, mesmo sem internet.</Text>
    </View>
    <Card style={styles.card}><Text accessibilityRole="header" style={[styles.title, { color: theme.text }]}>Acesse sua operação</Text>
      <Text style={{ color: theme.textSecondary, lineHeight: 20 }}>Use a conta cadastrada pelo responsável da empresa.</Text>
      {error ? <View accessibilityRole="alert" style={[styles.error, { backgroundColor: theme.dangerSoft }]}><MaterialIcons name="error-outline" size={20} color={theme.danger}/><Text style={{ color: theme.danger, flex: 1 }}>{error}</Text></View> : null}
      <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" autoComplete="email" keyboardType="email-address"/>
      <Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry autoComplete="current-password" onSubmitEditing={() => void submit()}/>
      <Button label={busy ? 'Entrando…' : 'Entrar'} icon="login" busy={busy} onPress={() => void submit()}/>
    </Card>
  </KeyboardAvoidingView></Screen>;
}
const styles = StyleSheet.create({ center:{flex:1,justifyContent:'center',gap:24,maxWidth:480,width:'100%',alignSelf:'center'},brand:{alignItems:'center',gap:8},logo:{width:64,height:64,borderRadius:18,alignItems:'center',justifyContent:'center'},name:{fontSize:28,fontWeight:'900'},subtitle:{fontSize:14,textAlign:'center'},card:{gap:16},title:{fontSize:22,fontWeight:'800'},error:{padding:12,borderRadius:8,flexDirection:'row',gap:8} });
