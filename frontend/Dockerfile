# Imagem do front do módulo, servida sob /modulos/produtos/ (Contrato §12.8).
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV PORTA=3003
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html/modulos/produtos
EXPOSE 3003
