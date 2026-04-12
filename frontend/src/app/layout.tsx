import type { Metadata } from "next";
import "./globals.css";
import { Montserrat, Lato } from "next/font/google";
import { cn } from "@/lib/utils";

const montserrat = Montserrat({ subsets: ["latin"], variable: "--font-heading" });
const lato = Lato({ subsets: ["latin"], weight: ["400", "700"], variable: "--font-body" });

export const metadata: Metadata = {
  title: "Octobird",
  description: "GitHub Bot Configuration Dashboard",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={cn(montserrat.variable, lato.variable)}>
      <body>{children}</body>
    </html>
  );
}
