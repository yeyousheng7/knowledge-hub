# KnowledgeHub Frontend

This application is the React frontend for KnowledgeHub. The current F3 baseline includes authentication, the application shell, and the read-only notes workspace.

## Prerequisites

- Node.js `^20.19.0` or `>=22.12.0`
- npm 10 or newer

## Local development

```bash
npm install
npm run dev
```

The application is served at the URL printed by Vite. Browser requests use the same-origin `/api/v1` path by default, and Vite proxies `/api` to `http://localhost:8080` during local development. Copy `.env.example` to `.env.local` and set `API_PROXY_TARGET` when the backend is running at another host or port. An explicit `VITE_API_BASE_URL` must include the `/api/v1` prefix.

Authentication is persisted under one browser `localStorage` key so a session can be validated with `/auth/me` after a reload. This is required by the current bearer-token contract and means the token is exposed to JavaScript; do not add duplicate token caches or log it.

## Verification

```bash
npm run build
npm run typecheck
npm run lint
npm run test
```
