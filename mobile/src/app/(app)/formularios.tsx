import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { useFocusEffect, router } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Card, Empty, Header, Screen, StatusPill } from '@/shared/components/ui';
import { listForms, type LocalForm } from '@/core/database/repository';
import { useAuth } from '@/features/auth/AuthContext';
import { synchronize } from '@/features/sync/sync';
import { useTheme } from '@/core/design-system/theme';

export default function FormsScreen() {
  const auth=useAuth(),theme=useTheme(),[forms,setForms]=useState<LocalForm[]>([]),[syncing,setSyncing]=useState(false),[error,setError]=useState('');
  const load=useCallback(async()=>{if(auth.tenantId)setForms(await listForms(auth.tenantId));},[auth.tenantId]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  async function sync(){if(!auth.tenantId)return;setSyncing(true);setError('');try{await synchronize(auth.tenantId);await load();}catch(reason){setError(reason instanceof Error?reason.message:'Não foi possível sincronizar.');}finally{setSyncing(false)}}
  return <Screen><Header eyebrow={auth.offline?'Modo offline':'Operação'} title="Formulários" description="Escolha um checklist publicado para iniciar uma nova inspeção."
    action={<Pressable accessibilityRole="button" accessibilityLabel="Sincronizar formulários" onPress={()=>void sync()} style={[styles.sync,{borderColor:theme.borderStrong}]}><MaterialIcons name="sync" size={22} color={theme.accent600}/></Pressable>}/>
    {error?<Card style={{backgroundColor:theme.dangerSoft}}><Text accessibilityRole="alert" style={{color:theme.danger}}>{error} Seus formulários salvos continuam disponíveis.</Text></Card>:null}
    {forms.length===0?<Empty icon="assignment" title="Nenhum formulário disponível" description={auth.offline?'Conecte-se à internet e sincronize para baixar os checklists liberados para você.':'Peça ao responsável para publicar e conceder acesso a um formulário.'}/>:forms.map(form=><Pressable key={form.formId} accessibilityRole="button" onPress={()=>router.push({pathname:'/(app)/formulario/[id]',params:{id:form.formId}})}><Card style={styles.formCard}><View style={[styles.formIcon,{backgroundColor:theme.accent50}]}><MaterialIcons name="fact-check" size={25} color={theme.accent700}/></View><View style={{flex:1,gap:5}}><Text style={[styles.formName,{color:theme.text}]}>{form.name}</Text>{form.description?<Text numberOfLines={2} style={{color:theme.textSecondary,lineHeight:19}}>{form.description}</Text>:null}<View style={styles.meta}><StatusPill label={'Versão '+form.version} tone="success"/><Text style={{color:theme.textSecondary,fontSize:12}}>{form.definition.sections.length} seções</Text></View></View><MaterialIcons name="chevron-right" size={24} color={theme.textSecondary}/></Card></Pressable>)}
    {syncing?<Text accessibilityRole="alert" style={{color:theme.textSecondary,textAlign:'center'}}>Sincronizando dados…</Text>:null}
  </Screen>
}
const styles=StyleSheet.create({sync:{width:48,height:48,borderWidth:1,borderRadius:12,alignItems:'center',justifyContent:'center'},formCard:{flexDirection:'row',alignItems:'center',gap:14},formIcon:{width:48,height:48,borderRadius:12,alignItems:'center',justifyContent:'center'},formName:{fontSize:17,fontWeight:'800'},meta:{flexDirection:'row',alignItems:'center',gap:10,marginTop:4}});
