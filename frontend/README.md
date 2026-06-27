# KnowledgeHub Frontend

F0 provides the buildable frontend foundation for KnowledgeHub. It intentionally contains only a placeholder screen and does not call the backend yet.

## Prerequisites

- Node.js `^20.19.0` or `>=22.12.0`
- npm 10 or newer

## Local development

```bash
npm install
npm run dev
```

The application is served at the URL printed by Vite. Copy `.env.example` to `.env.local` when local API configuration is needed. `VITE_API_BASE_URL` is reserved for the API client introduced in a later phase; F0 does not read it.

## Verification

```bash
npm run build
npm run typecheck
npm run lint
```
