import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "InspecTeam — Formulários de campo",
  description: "Crie, distribua e acompanhe checklists operacionais em uma única plataforma.",
  icons: { icon: "/favicon.svg", shortcut: "/favicon.svg" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
