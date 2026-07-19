import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Button, Card, Empty, Header, Screen, StatusPill } from '@/shared/components/ui';
import { listPending, type LocalSubmission } from '@/core/database/repository';
import { useAuth } from '@/features/auth/AuthContext';
import { synchronize } from '@/features/sync/sync';
import { useTheme } from '@/core/design-system/theme';

export default function PendingScreen(){
 const auth=useAuth(),theme=useTheme(),[items,setItems]=useState<LocalSubmission[]>([]),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const load=useCallback(async()=>{if(auth.tenantId)setItems(await listPending(auth.tenantId));},[auth.tenantId]);
 useFocusEffect(useCallback(()=>{void load()},[load]));
 async function sync(){if(!auth.tenantId)return;setBusy(true);setError('');try{await synchronize(auth.tenantId);await load()}catch(e){setError(e instanceof Error?e.message:'Falha na sincronização.')}finally{setBusy(false)}}
 return <Screen><Header eyebrow="Fila offline" title="Pendências" description="Itens salvos no aparelho aguardando envio ou revisão."/>
  <Button label="Sincronizar agora" icon="sync" busy={busy} onPress={()=>void sync()}/>
  {error?<Text accessibilityRole="alert" style={{color:theme.danger}}>{error}</Text>:null}
  {!items.length?<Empty icon="cloud-done" title="Tudo sincronizado" description="Não há respostas pendentes neste aparelho."/>:items.map(item=><Card key={item.id} style={styles.item}><View style={{flex:1}}><Text style={{color:theme.text,fontWeight:'800'}}>Resposta {item.id.slice(0,8)}</Text><Text style={{color:theme.textSecondary,fontSize:12}}>Atualizada em {new Date(item.updatedAt).toLocaleString('pt-BR')}</Text></View><StatusPill label={item.status==='CONFLICT'?'Requer atenção':item.status==='READY'?'Aguardando envio':'Rascunho'} tone={item.status==='CONFLICT'?'danger':item.status==='READY'?'warning':'neutral'}/></Card>)}
 </Screen>
}
const styles=StyleSheet.create({item:{flexDirection:'row',alignItems:'center',gap:12}});
