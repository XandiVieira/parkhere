import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import "leaflet/dist/leaflet.css";
import Navbar from "@/components/layout/Navbar";
import AuthHydrator from "@/components/auth/AuthHydrator";
import ThemeHydrator from "@/components/layout/ThemeHydrator";
import EmailVerificationBanner from "@/components/layout/EmailVerificationBanner";
import ServiceWorkerRegister from "@/components/layout/ServiceWorkerRegister";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "ParkHere - Encontre Vagas de Estacionamento",
  description: "Aplicativo colaborativo de descoberta de vagas",
  manifest: "/manifest.json",
  themeColor: "#2563eb",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "ParkHere",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="pt-BR"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <head>
        <link rel="apple-touch-icon" href="/icons/icon-192.png" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
      </head>
      <body className="flex min-h-screen flex-col bg-gray-50 dark:bg-gray-900">
        <ThemeHydrator />
        <ServiceWorkerRegister />
        <AuthHydrator />
        <Navbar />
        <EmailVerificationBanner />
        <main className="flex flex-1 flex-col dark:text-gray-100">{children}</main>
      </body>
    </html>
  );
}
