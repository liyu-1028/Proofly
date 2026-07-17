# GEMINI.md - Proofly (审稿宝) Project Instructions

This document provides essential context and guidelines for developing the Proofly project. Adhere to these standards to maintain consistency and quality.

## Project Overview

**Proofly (审稿宝)** is an online design proofing system tailored for printing shops, advertisement agencies, and design studios. It solves communication friction between designers and clients by providing a centralized platform for:
- **Project Management**: Organizing design tasks for clients.
- **Version Control**: Maintaining a history of design drafts (never overwritten).
- **Online Annotation**: Allowing clients to mark specific areas on a design with feedback.
- **Formal Confirmation**: Tracking "final approval" with detailed logs for legal and production safety.

### Architecture
- **Monorepo**: Root contains `backend/` and `frontend/`.
- **Multi-Tenancy**: Every entity must include a `store_id`. Systems must ensure strict data isolation based on `store_id` in a native SaaS architecture.
- **Backend Layers**: `Controller -> Service -> DAO -> MySQL`.
- **Frontend**: Vue 3 with Composition API and TypeScript.

## Technology Stack

| Layer | Technology |
| --- | --- |
| **Backend** | JDK 17, Spring Boot 3.5.x, MyBatis-Plus |
| **Frontend** | Vue 3, Vite, TypeScript, Pinia |
| **Database** | MySQL 8.0 |
| **Cache** | Redis 7.4.8 (`redis:7.4.8-alpine`) |
| **Storage** | MinIO (`RELEASE.2024-07-16T23-46-41Z`) |
| **Docs** | OpenAPI (SpringDoc), Markdown in `docs/` |

## Building and Running

### Prerequisites
- **Docker**: Used for local dependencies (MySQL, Redis, MinIO).
- **JDK 17**: Mandatory for the backend.
- **Node.js 20+**: Recommended for the frontend.

### Commands

#### Backend
Refer to `backend/MAVEN.md` for Maven setup details.
```bash
cd backend
mvn spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

#### Infrastructure (Local Dev)
```bash
# Start MySQL, Redis, MinIO
docker compose -f docker/docker-compose.dev.yml up -d
```
*(Note: If the docker-compose file is missing, refer to `docs/api-m12-deployment-ops-openapi.md` for requirements.)*

## Development Conventions

### Backend
- **Package Structure**: `com.lyllink.proofly.{module}.{controller|service|dao|dto|domain}`.
- **DAO Layer**: Use MyBatis-Plus Mappers for standard CRUD. Use MyBatis XML for complex queries.
- **Data Transfer**: Use `DTO` for requests/responses; do not expose `Entity` objects to the API.
- **API Response**: Always use `ApiResponse<T>` wrapper.
- **Validation**: Use `@Valid` and JSR-303 annotations in DTOs.
- **Immutability**: Design versions and confirmation records should never be physically deleted or overwritten. Use logical flags or audit trails.
- **Comments**: All code comments MUST be in Chinese.

### Frontend
- **API Client**: Centralized in `src/api/`. Use the `request<T>` utility in `src/api/http.ts`.
- **Types**: Define interfaces for API responses and business entities in `src/types/`.
- **State Management**: Use Pinia stores (e.g., `src/stores/session.ts`).
- **Styling**: Prefer clean, functional layouts for professional use.
- **Comments**: All code comments MUST be in Chinese.

### Documentation
- **API Specs**: Update or create `docs/api-mXX-{module}.md` for every new or modified API.
- **Module Tracking**: Update `docs/module-completion.md` when a milestone is reached.

## Core Rules & Constraints
1. **JDK 17 Only**: Do not use lower versions.
2. **Spring Boot 3.5.x**: Stay on this version line; do not upgrade to 4.x.
3. **Store Isolation**: Every business query MUST filter by `store_id`.
4. **Version Safety**: New design uploads must create a new version record in `project_version`.
5. **No Blind Overwrites**: Before modifying code, check if the user has made custom changes.
6. **Environment Variables**: Prefer `${VAR:default}` for configurations in `application.yml` and frontend `.env`.

## Key Files
- `README.md`: Project summary and goals.
- `AGENT.md`: Detailed developer collaboration rules and tech constraints.
- `docs/system-module-list.md`: Roadmap and feature specifications.
- `backend/MAVEN.md`: Local build environment specifics.
