import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker';
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { captureRef } from 'react-native-view-shot';
import Svg, { Path } from 'react-native-svg';
import { useMemo, useRef, useState } from 'react';
import { Image, PanResponder, Pressable, StyleSheet, Text, View } from 'react-native';
import type { FormField } from '@/core/api/contracts';
import { Card, Field } from '@/shared/components/ui';
import { useTheme } from '@/core/design-system/theme';

type Props={field:FormField;value:unknown;error?:string;onChange(value:unknown):void;onFile?(uri:string,name:string,mime:string,size?:number):Promise<void>};

export function DynamicField({field,value,error,onChange,onFile}:Props){
 const theme=useTheme();
 if(field.type==='heading')return <Text accessibilityRole="header" style={[styles.heading,{color:theme.text}]}>{field.label}</Text>;
 if(field.type==='instructions')return <View style={[styles.instructions,{backgroundColor:theme.infoSoft}]}><MaterialIcons name="info-outline" size={22} color={theme.info}/><Text style={{color:theme.text,flex:1,lineHeight:20}}>{field.description||field.label}</Text></View>;
 if(field.type==='checkbox')return <Pressable accessibilityRole="checkbox" accessibilityState={{checked:Boolean(value)}} onPress={()=>onChange(!value)} style={[styles.choice,{borderColor:error?theme.danger:theme.borderStrong,backgroundColor:value?theme.accent50:theme.surface}]}><MaterialIcons name={value?'check-box':'check-box-outline-blank'} size={26} color={value?theme.accent700:theme.textSecondary}/><Text style={{color:theme.text,flex:1,fontWeight:'600'}}>{field.label}{field.required?' *':''}</Text></Pressable>;
 if(field.type==='select'||field.type==='multiselect'){
  const selected=field.type==='multiselect'?(Array.isArray(value)?value:[]):[value];
  return <Card style={{gap:10}}><FieldTitle field={field}/>{field.options?.map(option=>{const active=selected.includes(option);return <Pressable key={option} accessibilityRole={field.type==='select'?'radio':'checkbox'} accessibilityState={{checked:active}} onPress={()=>{if(field.type==='select')onChange(option);else onChange(active?selected.filter(item=>item!==option):[...selected,option])}} style={[styles.option,{borderColor:active?theme.accent600:theme.border,backgroundColor:active?theme.accent50:theme.surface}]}><MaterialIcons name={active?(field.type==='select'?'radio-button-checked':'check-box'):(field.type==='select'?'radio-button-unchecked':'check-box-outline-blank')} size={23} color={active?theme.accent700:theme.textSecondary}/><Text style={{color:theme.text,flex:1}}>{option}</Text></Pressable>})}{error?<Text accessibilityRole="alert" style={{color:theme.danger}}>{error}</Text>:null}</Card>
 }
 if(field.type==='date'||field.type==='time')return <DateField field={field} value={value} error={error} onChange={onChange}/>;
 if(field.type==='photo')return <MediaField field={field} value={value} error={error} onChange={onChange} onFile={onFile}/>;
 if(field.type==='signature')return <SignatureField field={field} value={value} error={error} onChange={onChange} onFile={onFile}/>;
 return <Field label={field.label+(field.required?' *':'')} error={error} value={typeof value==='string'?value:''} onChangeText={onChange} multiline={field.type==='textarea'} numberOfLines={field.type==='textarea'?5:1} keyboardType={field.type==='number'?'decimal-pad':'default'} placeholder={field.description||undefined} style={field.type==='textarea'?{minHeight:120,textAlignVertical:'top'}:undefined}/>;
}

function FieldTitle({field}:{field:FormField}){const theme=useTheme();return <View style={{gap:3}}><Text style={{color:theme.text,fontWeight:'800'}}>{field.label}{field.required?' *':''}</Text>{field.description?<Text style={{color:theme.textSecondary,fontSize:13}}>{field.description}</Text>:null}</View>}

function DateField({field,value,error,onChange}:Props){
 const theme=useTheme(),[open,setOpen]=useState(false),text=typeof value==='string'?value:'';
 const selected=text?(field.type==='date'?new Date(text+'T12:00:00'):new Date('1970-01-01T'+text+':00')):new Date();
 function changed(event:DateTimePickerEvent,date?:Date){setOpen(false);if(event.type!=='set'||!date)return;onChange(field.type==='date'?date.toISOString().slice(0,10):date.toTimeString().slice(0,5))}
 const display=text?(field.type==='date'?selected.toLocaleDateString('pt-BR'):text):'Selecionar '+(field.type==='date'?'data':'horário');
 return <View style={{gap:6}}><FieldTitle field={field}/><Pressable accessibilityRole="button" onPress={()=>setOpen(true)} style={[styles.dateButton,{backgroundColor:theme.surface,borderColor:error?theme.danger:theme.borderStrong}]}><MaterialIcons name={field.type==='date'?'calendar-today':'schedule'} size={22} color={theme.accent700}/><Text style={{color:text?theme.text:theme.textSecondary,flex:1,fontSize:16}}>{display}</Text><MaterialIcons name="expand-more" size={22} color={theme.textSecondary}/></Pressable>{open?<DateTimePicker value={selected} mode={field.type==='date'?'date':'time'} is24Hour onChange={changed}/>:null}{error?<Text accessibilityRole="alert" style={{color:theme.danger}}>{error}</Text>:null}</View>
}
async function persistFile(uri:string,name:string){
 const directory=FileSystem.documentDirectory+'evidencias/';
 await FileSystem.makeDirectoryAsync(directory,{intermediates:true});
 const target=directory+Date.now()+'-'+name.replace(/[^a-zA-Z0-9._-]/g,'_');
 await FileSystem.copyAsync({from:uri,to:target});
 return target;
}

function MediaField({field,value,error,onChange,onFile}:Props){
 const theme=useTheme(),uri=typeof value==='object'&&value&&'uri'in value?String((value as any).uri):'';
 async function capture(){
  const permission=await ImagePicker.requestCameraPermissionsAsync();if(!permission.granted)return;
  const result=await ImagePicker.launchCameraAsync({mediaTypes:['images'],quality:.82,exif:false});
  if(result.canceled)return;const asset=result.assets[0],name=asset.fileName||('foto-'+Date.now()+'.jpg'),saved=await persistFile(asset.uri,name);
  const info=await FileSystem.getInfoAsync(saved);onChange({uri:saved,name,mime:asset.mimeType||'image/jpeg'});await onFile?.(saved,name,asset.mimeType||'image/jpeg',info.exists?info.size:undefined);
 }
 return <Card style={{gap:12}}><FieldTitle field={field}/>{uri?<Image source={{uri}} accessibilityLabel={'Foto de '+field.label} style={styles.photo}/>:<View style={[styles.photoEmpty,{backgroundColor:theme.surfaceSubtle}]}><MaterialIcons name="photo-camera" size={38} color={theme.textSecondary}/><Text style={{color:theme.textSecondary}}>Nenhuma foto registrada</Text></View>}<Pressable accessibilityRole="button" onPress={()=>void capture()} style={[styles.capture,{backgroundColor:theme.brand900}]}><MaterialIcons name="photo-camera" size={22} color="#fff"/><Text style={{color:'#fff',fontWeight:'800'}}>{uri?'Refazer foto':'Abrir câmera'}</Text></Pressable>{error?<Text style={{color:theme.danger}}>{error}</Text>:null}</Card>
}

function SignatureField({field,value,error,onChange,onFile}:Props){
 const theme=useTheme(),[paths,setPaths]=useState<string[]>([]),canvas=useRef<View>(null);
 const pan=useMemo(()=>PanResponder.create({onStartShouldSetPanResponder:()=>true,onPanResponderGrant:e=>{const start='M '+e.nativeEvent.locationX+' '+e.nativeEvent.locationY;setPaths(items=>[...items,start])},onPanResponderMove:e=>{const point=' L '+e.nativeEvent.locationX+' '+e.nativeEvent.locationY;setPaths(items=>[...items.slice(0,-1),(items.at(-1)||'')+point])}}),[]);
 async function finish(){if(!paths.some(Boolean)||!canvas.current)return;const temporary=await captureRef(canvas,{format:'png',quality:1});const name='assinatura-'+Date.now()+'.png',saved=await persistFile(temporary,name);const info=await FileSystem.getInfoAsync(saved);onChange({uri:saved,name,mime:'image/png'});await onFile?.(saved,name,'image/png',info.exists?info.size:undefined)}
 return <Card style={{gap:12}}><FieldTitle field={field}/><View ref={canvas} collapsable={false} style={[styles.signature,{backgroundColor:'#fff',borderColor:error?theme.danger:theme.borderStrong}]} {...pan.panHandlers}>{typeof value==='object'&&value&&'uri'in value?<Image source={{uri:String((value as any).uri)}} style={StyleSheet.absoluteFill}/>:<Svg width="100%" height="100%">{paths.filter(Boolean).map((path,index)=><Path key={index} d={path} stroke="#102832" strokeWidth={2.4} fill="none" strokeLinecap="round"/>)}</Svg>}</View><View style={styles.signatureActions}><Pressable accessibilityRole="button" onPress={()=>{setPaths([]);onChange(undefined)}} style={[styles.smallButton,{borderColor:theme.borderStrong}]}><Text style={{color:theme.text}}>Limpar</Text></Pressable><Pressable accessibilityRole="button" onPress={()=>void finish()} style={[styles.smallButton,{backgroundColor:theme.brand900}]}><Text style={{color:'#fff',fontWeight:'700'}}>Confirmar assinatura</Text></Pressable></View>{error?<Text style={{color:theme.danger}}>{error}</Text>:null}</Card>
}
const styles=StyleSheet.create({heading:{fontSize:20,fontWeight:'900',marginTop:8},instructions:{padding:14,borderRadius:10,flexDirection:'row',gap:10},dateButton:{minHeight:52,borderWidth:1,borderRadius:10,paddingHorizontal:12,flexDirection:'row',alignItems:'center',gap:10},choice:{minHeight:52,borderWidth:1,borderRadius:10,padding:12,flexDirection:'row',alignItems:'center',gap:10},option:{minHeight:48,borderWidth:1,borderRadius:9,paddingHorizontal:12,flexDirection:'row',alignItems:'center',gap:10},photo:{height:220,borderRadius:10,width:'100%'},photoEmpty:{height:170,borderRadius:10,alignItems:'center',justifyContent:'center',gap:8},capture:{minHeight:52,borderRadius:10,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:8},signature:{height:190,borderWidth:1,borderRadius:10,overflow:'hidden'},signatureActions:{flexDirection:'row',gap:8,justifyContent:'flex-end'},smallButton:{minHeight:44,borderWidth:1,borderRadius:8,paddingHorizontal:14,alignItems:'center',justifyContent:'center'}});
