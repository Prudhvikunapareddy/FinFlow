FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/docker-entrypoint.sh /docker-entrypoint.d/40-finflow-config.sh
COPY --from=build /app/dist/finflow-frontend/browser /usr/share/nginx/html

RUN chmod +x /docker-entrypoint.d/40-finflow-config.sh

EXPOSE 80
