# Build stage
FROM node:25-alpine AS build

WORKDIR /app

# Accept build argument for API URL
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy all other files
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Upgrade libpng to fix CVE-2026-25646
RUN apk upgrade --no-cache libpng

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY ./nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]
