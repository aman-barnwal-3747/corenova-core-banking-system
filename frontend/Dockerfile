# ================================================================
#  CoreNova Bank – Frontend Dockerfile
#  PDF Phase 6: Docker & Deployment
#
#  Multi-stage build:
#    Stage 1 (builder) : Node 20 → npm install + vite build → /dist
#    Stage 2 (runtime) : Nginx (alpine) → serves static /dist
#
#  Nginx is configured to:
#   • Serve the React SPA with client-side routing fallback
#     (all routes → index.html, so /accounts, /loans etc. work on refresh)
#   • Proxy /api/* to the backend container (see nginx.conf)
# ================================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /build

# Leverage layer caching for node_modules
COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# ── Stage 2: Runtime (Nginx) ───────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove default Nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy built React app
COPY --from=builder /build/dist /usr/share/nginx/html

# Custom Nginx config (SPA routing + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
