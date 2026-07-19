import { useColorScheme } from "react-native";
export const palette={brand950:"#071d2b",brand900:"#0b2637",brand700:"#15546a",accent700:"#08775f",accent600:"#0c8e70",accent100:"#d9f5eb",accent50:"#edf9f5",danger:"#a12a3e",dangerSoft:"#f9dfe4",warning:"#805b12",warningSoft:"#fff0c7",success:"#086947",successSoft:"#d9f4e8",info:"#175e89",infoSoft:"#dbeefa",white:"#ffffff"};
export const light={canvas:"#f3f6f7",surface:"#ffffff",surfaceSubtle:"#f6f8f9",text:"#101c24",textSecondary:"#5e6f78",border:"#dce3e6",borderStrong:"#c8d1d5",...palette};
export const dark={canvas:"#0c1d27",surface:"#132733",surfaceSubtle:"#10232e",text:"#f5f8f9",textSecondary:"#a7b7be",border:"#28424f",borderStrong:"#3a5764",...palette,accent50:"#13392f",accent100:"#164c3e"};
export const spacing={xs:4,sm:8,md:12,lg:16,xl:24,xxl:32};
export const radius={sm:8,md:12,lg:18};
export function useTheme(){return useColorScheme()==="dark"?dark:light}
