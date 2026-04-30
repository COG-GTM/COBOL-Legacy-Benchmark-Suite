import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

async function main() {
  // Seed portfolios matching TSTGEN00.cbl test data patterns
  const portfolios = [
    {
      id: "PORT0001",
      accountNo: "1234567890",
      clientName: "John Smith",
      clientType: "INDIVIDUAL" as const,
    },
    {
      id: "PORT0002",
      accountNo: "2345678901",
      clientName: "Acme Corporation",
      clientType: "CORPORATE" as const,
    },
    {
      id: "PORT0003",
      accountNo: "3456789012",
      clientName: "Smith Family Trust",
      clientType: "TRUST" as const,
    },
  ];

  for (const p of portfolios) {
    await prisma.portfolio.upsert({
      where: { id: p.id },
      update: {},
      create: p,
    });
  }

  // Seed positions
  await prisma.position.upsert({
    where: {
      portfolioId_investmentId: {
        portfolioId: "PORT0001",
        investmentId: "AAPL",
      },
    },
    update: {},
    create: {
      portfolioId: "PORT0001",
      investmentId: "AAPL",
      quantity: 100,
      costBasis: 15000,
      marketValue: 17500,
    },
  });

  await prisma.position.upsert({
    where: {
      portfolioId_investmentId: {
        portfolioId: "PORT0001",
        investmentId: "GOOGL",
      },
    },
    update: {},
    create: {
      portfolioId: "PORT0001",
      investmentId: "GOOGL",
      quantity: 50,
      costBasis: 70000,
      marketValue: 72000,
    },
  });

  console.log("Seed data created successfully");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
