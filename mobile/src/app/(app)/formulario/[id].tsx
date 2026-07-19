import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router, useLocalSearchParams } from 'expo-router';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Button, Card, Empty, Header, Screen } from '@/shared/components/ui';
import { findDraft, findForm, queueSubmission, saveAttachment, saveDraft, type LocalForm } from '@/core/database/repository';
import { DynamicField } from '@/features/form/DynamicField';
import { useTheme } from '@/core/design-system/theme';
import { useAuth } from '@/features/auth/AuthContext';
import { synchronize } from '@/features/sync/sync';
import { hasValue } from '@/features/form/validation';

export default function FormScreen(){
 const {id}=useLocalSearchParams<{id:string}>(),auth=useAuth(),theme=useTheme();
 const [form,setForm]=useState<LocalForm|null>(null),[answers,setAnswers]=useState<Record<string,unknown>>({}),[draftId,setDraftId]=useState<string>();
 const [section,setSection]=useState(0),[errors,setErrors]=useState<Record<string,string>>({}),[status,setStatus]=useState('Carregando…'),[sending,setSending]=useState(false);
 const save=useCallback(async()=>{if(!form||!auth.tenantId)return;const saved=await saveDraft({id:draftId,tenantId:auth.tenantId,formId:form.formId,formVersionId:form.versionId,answers});if(!draftId)setDraftId(saved);return saved},[form,auth.tenantId,draftId,answers]);
 useEffect(()=>{void(async()=>{const formId=Array.isArray(id)?id[0]:id;if(!formId)return;const loaded=await findForm(formId);setForm(loaded);const draft=await findDraft(formId);if(draft){setDraftId(draft.id);setAnswers(draft.answers);setStatus('Rascunho recuperado')}else setStatus('Novo preenchimento')})()},[id]);
 useEffect(()=>{if(!form||!Object.keys(answers).length)return;const timer=setTimeout(()=>{void save().then(()=>setStatus('Salvo neste aparelho'))},500);return()=>clearTimeout(timer)},[form,answers,save]);
 const current=form?.definition.sections[section];
 const progress=useMemo(()=>{if(!form)return 0;const fields=form.definition.sections.flatMap(item=>item.fields).filter(item=>!['heading','instructions'].includes(item.type));if(!fields.length)return 100;return Math.round(fields.filter(item=>hasValue(answers[item.id])).length/fields.length*100)},[form,answers]);
 function validate(all=false){if(!form)return false;const fields=(all?form.definition.sections:[current]).filter(Boolean).flatMap(item=>item!.fields),next:Record<string,string>={};for(const field of fields)if(field.required&&!hasValue(answers[field.id]))next[field.id]='Este campo é obrigatório.';setErrors(previous=>all?next:{...previous,...next});return Object.keys(next).length===0}
 async function addFile(fieldId:string,uri:string,name:string,mime:string,size?:number){const submissionId=await save();if(!submissionId)return;const attachmentId=await saveAttachment(submissionId,fieldId,uri,name,mime,size);setAnswers(currentAnswers=>({...currentAnswers,[fieldId]:{uri,name,mime,localAttachmentId:attachmentId}}))}
 async function finish(){if(!validate(true)||!form)return;setSending(true);setStatus('Preparando envio…');try{const submissionId=await save();if(!submissionId)return;await queueSubmission(submissionId);setStatus('Salvo na fila do aparelho');if(auth.tenantId){try{await synchronize(auth.tenantId);setStatus('Resposta sincronizada')}catch{setStatus('Sem conexão — envio ficará pendente')}}router.replace('/(app)/pendencias')}finally{setSending(false)}}
 if(!form)return <Screen><Header title="Formulário"/><Empty icon="assignment-late" title="Formulário indisponível" description="Sincronize novamente ou solicite acesso ao responsável."/></Screen>;
 return <Screen><Pressable accessibilityRole="button" onPress={()=>router.back()} style={styles.back}><MaterialIcons name="arrow-back" size={22} color={theme.text}/><Text style={{color:theme.text,fontWeight:'700'}}>Formulários</Text></Pressable>
  <Header eyebrow={'Versão '+form.version} title={form.name} description={form.description}/>
  <Card style={styles.progress}><View style={styles.progressHead}><Text style={{color:theme.textSecondary}}>Seção {section+1} de {form.definition.sections.length}</Text><Text style={{color:theme.accent700,fontWeight:'900'}}>{progress}% preenchido</Text></View><View accessibilityRole="progressbar" accessibilityValue={{min:0,max:100,now:progress}} style={[styles.track,{backgroundColor:theme.border}]}><View style={[styles.bar,{backgroundColor:theme.accent600,width:(progress+'%') as any}]}/></View><View style={styles.steps}>{form.definition.sections.map((item,index)=><Pressable key={item.id} accessibilityRole="button" accessibilityLabel={'Ir para seção '+(index+1)+': '+item.title} onPress={()=>setSection(index)} style={[styles.step,{backgroundColor:index===section?theme.brand900:theme.surfaceSubtle}]}><Text style={{color:index===section?'#fff':theme.textSecondary,fontWeight:'800'}}>{index+1}</Text></Pressable>)}</View></Card>
  <View style={{gap:14}}><View><Text accessibilityRole="header" style={[styles.sectionTitle,{color:theme.text}]}>{current?.title}</Text>{current?.description?<Text style={{color:theme.textSecondary}}>{current.description}</Text>:null}</View>
   {current?.fields.map(field=><DynamicField key={field.id} field={field} value={answers[field.id]} error={errors[field.id]} onChange={value=>{setAnswers(items=>({...items,[field.id]:value}));setErrors(items=>({...items,[field.id]:''}));setStatus('Salvando…')}} onFile={(uri,name,mime,size)=>addFile(field.id,uri,name,mime,size)}/>)}
  </View>
  <Text accessibilityLiveRegion="polite" style={{color:theme.textSecondary,textAlign:'center'}}>{status}</Text>
  <View style={styles.actions}>{section>0?<Button label="Voltar" icon="arrow-back" variant="secondary" onPress={()=>setSection(section-1)}/>:<View/>}{section<form.definition.sections.length-1?<Button label="Continuar" icon="arrow-forward" onPress={()=>{if(validate())setSection(section+1)}}/>:<Button label="Concluir checklist" icon="send" busy={sending} onPress={()=>void finish()}/>}</View>
 </Screen>
}
const styles=StyleSheet.create({back:{minHeight:44,alignSelf:'flex-start',flexDirection:'row',alignItems:'center',gap:7},progress:{gap:12},progressHead:{flexDirection:'row',justifyContent:'space-between'},track:{height:7,borderRadius:99,overflow:'hidden'},bar:{height:'100%',borderRadius:99},steps:{flexDirection:'row',gap:7,flexWrap:'wrap'},step:{width:36,height:36,borderRadius:9,alignItems:'center',justifyContent:'center'},sectionTitle:{fontSize:22,fontWeight:'900'},actions:{flexDirection:'row',justifyContent:'space-between',gap:12}});
