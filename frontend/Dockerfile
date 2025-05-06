# 1단계: Node 빌드 스테이지
FROM node:20 AS build
WORKDIR /app

ENV GENERATE_SOURCEMAP=false

ENV GENERATE_SOURCEMAP=false

COPY package*.json ./
RUN npm ci --omit=dev

COPY . .

RUN npm run build

# 2단계: Nginx
FROM nginx:alpine

# nginx.conf 파일 복사 추가
COPY ./docker/nginx/nginx.conf /etc/nginx/nginx.conf

# 빌드 결과 복사
COPY --from=build /app/build /usr/share/nginx/html