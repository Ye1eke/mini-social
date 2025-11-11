# Docker Compose Setup Guide

This guide explains how to run the MiniSocial application using Docker Compose for local development.

## Prerequisites

- Docker Desktop or Docker Engine (20.10+)
- Docker Compose (v2.0+)

## Quick Start

1. **Clone the repository and navigate to the project root**

2. **Configure environment variables (optional)**

   ```bash
   cp .env.example .env
   # Edit .env and add your Backblaze B2 credentials if you want to test image uploads
   ```

3. **Start all services**

   ```bash
   docker-compose up -d
   ```

4. **View logs**

   ```bash
   # All services
   docker-compose logs -f

   # Specific service
   docker-compose logs -f backend
   docker-compose logs -f frontend
   ```

5. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - PostgreSQL: localhost:5432
   - Redis: localhost:6379

## Services

### Frontend (Next.js)

- **Port**: 3000
- **Development mode**: Hot reload enabled
- **Volume mounts**: Source code is mounted for live updates

### Backend (Spring Boot)

- **Port**: 8080
- **Database**: Connects to PostgreSQL service
- **Cache**: Connects to Redis service
- **Health check**: Waits for database to be ready

### PostgreSQL

- **Port**: 5432
- **Database**: minisocial
- **User**: postgres
- **Password**: postgres
- **Initialization**: Runs init.sql on first start
- **Persistence**: Data stored in Docker volume

### Redis

- **Port**: 6379
- **Persistence**: AOF (Append Only File) enabled
- **Data**: Stored in Docker volume

## Common Commands

### Start services

```bash
docker-compose up -d
```

### Stop services

```bash
docker-compose down
```

### Stop and remove volumes (clean slate)

```bash
docker-compose down -v
```

### Rebuild services

```bash
docker-compose up -d --build
```

### View service status

```bash
docker-compose ps
```

### Execute commands in containers

```bash
# Backend shell
docker-compose exec backend sh

# PostgreSQL CLI
docker-compose exec postgres psql -U postgres -d minisocial

# Redis CLI
docker-compose exec redis redis-cli
```

## Troubleshooting

### Backend fails to start

- Check if PostgreSQL is healthy: `docker-compose ps`
- View backend logs: `docker-compose logs backend`
- Ensure port 8080 is not in use

### Frontend fails to start

- Check if backend is running: `docker-compose ps`
- View frontend logs: `docker-compose logs frontend`
- Ensure port 3000 is not in use

### Database connection issues

- Wait for PostgreSQL health check to pass
- Check database logs: `docker-compose logs postgres`
- Verify connection string in docker-compose.yml

### Redis connection issues

- Check Redis is running: `docker-compose ps redis`
- Test connection: `docker-compose exec redis redis-cli ping`

### Clean restart

```bash
docker-compose down -v
docker-compose up -d --build
```

## Development Workflow

1. **Make code changes**

   - Frontend: Changes are hot-reloaded automatically
   - Backend: Rebuild required (`docker-compose up -d --build backend`)

2. **View logs in real-time**

   ```bash
   docker-compose logs -f backend frontend
   ```

3. **Access database**

   ```bash
   docker-compose exec postgres psql -U postgres -d minisocial
   ```

4. **Check Redis cache**
   ```bash
   docker-compose exec redis redis-cli
   # Example: KEYS feed:*
   # Example: ZRANGE feed:1 0 -1 WITHSCORES
   ```

## Network Configuration

All services are connected via the `minisocial-network` bridge network, allowing them to communicate using service names as hostnames:

- Backend connects to `postgres:5432`
- Backend connects to `redis:6379`
- Frontend connects to `backend:8080` (via localhost:8080 from host)

## Volume Persistence

Data is persisted in Docker volumes:

- `postgres_data`: Database files
- `redis_data`: Redis AOF files

To remove all data:

```bash
docker-compose down -v
```

## Environment Variables

Key environment variables configured in docker-compose.yml:

**Backend:**

- `SPRING_DATASOURCE_URL`: PostgreSQL connection string
- `DB_USERNAME`, `DB_PASSWORD`: Database credentials
- `REDIS_HOST`, `REDIS_PORT`: Redis connection
- `JWT_SECRET`: JWT signing key (change in production!)
- `B2_*`: Backblaze B2 storage credentials

**Frontend:**

- `NEXT_PUBLIC_API_URL`: Backend API URL

## Production Deployment

This Docker Compose setup is for **local development only**. For production:

- Frontend: Deploy to Vercel (see .github/workflows/frontend.yml)
- Backend: Deploy to Fly.io (see .github/workflows/backend.yml)
- Use managed PostgreSQL and Redis services
- Configure proper secrets and environment variables
