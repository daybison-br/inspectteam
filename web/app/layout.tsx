import type {Metadata} from "next";import "./globals.css";
export const metadata:Metadata={title:{default:"InspecTeam",template:"%s · InspecTeam"},description:"Gestão de inspeções, checklists e equipes de campo."};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="pt-BR"><body>{children}</body></html>}
