export const metadata = {
  title: "Investment Portfolio Management System",
  description: "Modernized from COBOL Legacy Benchmark Suite",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
