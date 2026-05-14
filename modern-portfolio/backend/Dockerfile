FROM node:20-alpine AS builder
WORKDIR /app
COPY backend/package.json backend/tsconfig.json ./backend/
COPY database/prisma ./database/prisma/
COPY database/package.json ./database/
WORKDIR /app/database
RUN npm install
RUN npx prisma generate --schema=prisma/schema.prisma
WORKDIR /app/backend
RUN npm install
COPY backend/src ./src/
RUN npx tsc

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/backend/dist ./dist
COPY --from=builder /app/backend/package.json ./
COPY --from=builder /app/backend/node_modules ./node_modules
COPY --from=builder /app/database/prisma ./prisma
COPY --from=builder /app/database/node_modules/.prisma ./node_modules/.prisma
EXPOSE 3001
CMD ["node", "dist/index.js"]
