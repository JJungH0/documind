import type { Metadata } from "next";
import { IBM_Plex_Sans_KR } from "next/font/google";
import "./globals.css";

const plex = IBM_Plex_Sans_KR({
  weight: ["400", "600"],
  preload: false,
  display: "swap",
  variable: "--font-plex",
});

export const metadata: Metadata = {
  title: "DocuMind",
  description: "업로드한 문서를 근거로 답하는 질의응답",
};

export default function RootLayout({
                                     children,
                                   }: Readonly<{ children: React.ReactNode }>) {
  return (
      <html lang="ko" className={plex.variable}>
      <body className="font-sans antialiased">{children}</body>
      </html>
  );
}