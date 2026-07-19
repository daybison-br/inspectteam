import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Button, Card, Header, Screen, StatusPill } from '@/shared/components/ui';
import { useAuth } from '@/features/auth/AuthContext';
import { useTheme } from '@/core/design-system/theme';

export default function SettingsScreen(){
 const auth=useAuth(),theme=useTheme();
 return <Screen><Header eyebrow="Conta e dispositivo" title="Ajustes" description="Gerencie a empresa ativa e a sessão neste aparelho."/>
  <Card style={styles.profile}><View style={[styles.avatar,{backgroundColor:theme.brand900}]}><Text style={{color:'#fff',fontWeight:'900',fontSize:20}}>{auth.account?.displayName?.slice(0,1).toUpperCase()}</Text></View><View style={{flex:1}}><Text style={{color:theme.text,fontWeight:'800',fontSize:17}}>{auth.account?.displayName}</Text><Text style={{color:theme.textSecondary}}>{auth.account?.email}</Text></View>{auth.offline?<StatusPill label="Offline" tone="warning"/>:<StatusPill label="Online" tone="success"/>}</Card>
  <Text style={[styles.section,{color:theme.text}]}>Empresa ativa</Text>
  {auth.tenants.map(tenant=><Pressable key={tenant.tenantId} accessibilityRole="radio" accessibilityState={{checked:tenant.tenantId===auth.tenantId}} onPress={()=>void auth.selectTenant(tenant.tenantId)}><Card style={styles.tenant}><MaterialIcons name={tenant.tenantId===auth.tenantId?'radio-button-checked':'radio-button-unchecked'} size={24} color={tenant.tenantId===auth.tenantId?theme.accent600:theme.textSecondary}/><View style={{flex:1}}><Text style={{color:theme.text,fontWeight:'700'}}>{tenant.name}</Text><Text style={{color:theme.textSecondary,fontSize:12}}>{tenant.membershipType}</Text></View></Card></Pressable>)}
  <Button label="Sair deste aparelho" icon="logout" variant="danger" onPress={()=>void auth.signOut().then(()=>router.replace('/login'))}/>
 </Screen>
}
const styles=StyleSheet.create({profile:{flexDirection:'row',alignItems:'center',gap:12},avatar:{width:48,height:48,borderRadius:14,alignItems:'center',justifyContent:'center'},section:{fontSize:14,fontWeight:'800',marginTop:6},tenant:{flexDirection:'row',alignItems:'center',gap:12}});
