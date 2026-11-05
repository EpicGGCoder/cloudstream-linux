import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";
import { ThemeProvider } from "@/components/layout/theme-provider";
import { QueryProvider } from "@/components/layout/query-provider";

const inter = Inter({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "CloudStream — Linux Port",
  description:
    "CloudStream ported to Linux — a media center that prioritizes freedom and flexibility. Browse, search, and play from any provider.",
  keywords: [
    "CloudStream",
    "Linux",
    "media center",
    "streaming",
    "AppImage",
    "extensions",
  ],
  authors: [{ name: "CloudStream Linux Port" }],
  manifest: "/manifest.json",
  openGraph: {
    title: "CloudStream — Linux Port",
    description: "A faithful Linux port of CloudStream, the extension-based media player.",
    type: "website",
  },
};

export const viewport: Viewport = {
  themeColor: "#1a0d1f",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning className="dark">
      <body
        className={`${inter.variable} font-sans antialiased bg-background text-foreground`}
      >
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem={false}
          disableTransitionOnChange
        >
          <QueryProvider>
            {children}
            <Toaster />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
