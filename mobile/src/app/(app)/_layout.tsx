import { Redirect, Slot } from 'expo-router';
import { ActivityIndicator, Modal, StyleSheet, Text, View } from 'react-native';
import { useState } from 'react';
import { BottomNav, Button, Card, Field } from '@/shared/components/ui';
import { useAuth } from '@/features/auth/AuthContext';
import { useTheme } from '@/core/design-system/theme';

export default function AppLayout() {
  const auth = useAuth(), theme = useTheme();
  const [current, setCurrent] = useState(''), [next, setNext] = useState(''), [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  if (auth.loading) return <View style={[styles.loading, { backgroundColor: theme.canvas }]}><ActivityIndicator color={theme.accent600}/></View>;
  if (!auth.account) return <Redirect href="/login"/>;
  async function change() {
    if (next.length < 10 || next !== confirm) { setError('A nova senha deve ter ao menos 10 caracteres e a confirmação deve ser igual.'); return; }
    setBusy(true); setError('');
    try { await auth.changeTemporaryPassword(current, next); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível alterar a senha.'); }
    finally { setBusy(false); }
  }
  return <View style={{ flex: 1, backgroundColor: theme.canvas }}><View style={{ flex: 1 }}><Slot/></View><BottomNav/>
    <Modal visible={auth.account.mustChangePassword} transparent animationType="fade" onRequestClose={() => {}}>
      <View style={styles.backdrop}><Card style={styles.modal}><Text accessibilityRole="header" style={[styles.title, { color: theme.text }]}>Crie uma senha definitiva</Text>
        <Text style={{ color: theme.textSecondary, lineHeight: 20 }}>Sua senha atual é temporária. Por segurança, a troca é obrigatória antes de usar o aplicativo.</Text>
        {error ? <Text accessibilityRole="alert" style={{ color: theme.danger }}>{error}</Text> : null}
        <Field label="Senha temporária" value={current} onChangeText={setCurrent} secureTextEntry/>
        <Field label="Nova senha" value={next} onChangeText={setNext} secureTextEntry/>
        <Field label="Confirme a nova senha" value={confirm} onChangeText={setConfirm} secureTextEntry/>
        <Button label="Salvar nova senha" busy={busy} onPress={() => void change()}/>
      </Card></View>
    </Modal>
  </View>;
}
const styles=StyleSheet.create({loading:{flex:1,alignItems:'center',justifyContent:'center'},backdrop:{flex:1,backgroundColor:'rgba(0,20,30,.72)',justifyContent:'center',padding:20},modal:{gap:16,maxWidth:480,width:'100%',alignSelf:'center'},title:{fontSize:22,fontWeight:'800'}});
