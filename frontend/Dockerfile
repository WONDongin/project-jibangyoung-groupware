FROM node:20 AS builder
WORKDIR /app
COPY . .
RUN npm install && npm run build

FROM node:20
WORKDIR /app
COPY --from=builder /app .
EXPOSE 3000
CMD ["npm", "run", "start"]
