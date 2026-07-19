import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';
import { useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Button, Card, Field } from '@/shared/components/ui';
import { useTheme } from '@/core/design-system/theme';
import { useAuth } from '@/features/auth/AuthContext';

export default function Login() {
  const theme = useTheme(), auth = useAuth(), passwordInput = useRef<TextInput>(null);
  const [email, setEmail] = useState(''), [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit() {
    if (!email || !password) { setError('Informe e-mail e senha.'); return; }
    setBusy(true); setError('');
    try { await auth.signIn(email, password); router.replace('/(app)/formularios'); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.'); }
    finally { setBusy(false); }
  }
  return <SafeAreaView style={[styles.safe, { backgroundColor: theme.canvas }]}>
    <KeyboardAvoidingView style={styles.safe} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled" keyboardDismissMode="interactive" automaticallyAdjustKeyboardInsets={Platform.OS === 'ios'}>
        <View style={styles.center}>
          <View style={styles.brand}><View style={[styles.logo, { backgroundColor: theme.brand900 }]}><MaterialIcons name="fact-check" size={32} color="#fff"/></View>
            <Text style={[styles.name, { color: theme.text }]}>InspecTeam</Text><Text style={[styles.subtitle, { color: theme.textSecondary }]}>Checklists confiáveis, mesmo sem internet.</Text>
          </View>
          <Card style={styles.card}><Text accessibilityRole="header" style={[styles.title, { color: theme.text }]}>Acesse sua operação</Text>
            <Text style={{ color: theme.textSecondary, lineHeight: 20 }}>Use a conta cadastrada pelo responsável da empresa.</Text>
            {error ? <View accessibilityRole="alert" style={[styles.error, { backgroundColor: theme.dangerSoft }]}><MaterialIcons name="error-outline" size={20} color={theme.danger}/><Text style={{ color: theme.danger, flex: 1 }}>{error}</Text></View> : null}
            <Field label="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" autoCorrect={false} autoComplete="email" textContentType="emailAddress" keyboardType="email-address" returnKeyType="next" blurOnSubmit={false} onSubmitEditing={() => passwordInput.current?.focus()}/>
            <Field ref={passwordInput} label="Senha" value={password} onChangeText={setPassword} secureTextEntry autoComplete="current-password" textContentType="password" returnKeyType="done" onSubmitEditing={() => void submit()}/>
            <Button label={busy ? 'Entrando…' : 'Entrar'} icon="login" busy={busy} onPress={() => void submit()}/>
          </Card>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  </SafeAreaView>;
}
const styles = StyleSheet.create({ safe:{flex:1},scroll:{flexGrow:1,justifyContent:'center',padding:16},center:{gap:24,maxWidth:480,width:'100%',alignSelf:'center',paddingVertical:16},brand:{alignItems:'center',gap:8},logo:{width:64,height:64,borderRadius:18,alignItems:'center',justifyContent:'center'},name:{fontSize:28,fontWeight:'900'},subtitle:{fontSize:14,textAlign:'center'},card:{gap:16},title:{fontSize:22,fontWeight:'800'},error:{padding:12,borderRadius:8,flexDirection:'row',gap:8} });
