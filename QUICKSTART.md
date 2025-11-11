# MiniSocial Quick Start Guide

## 🚀 Your Application is Running!

All services are up and running with Docker Compose. Here's what you have:

### 📍 Access Points

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### ✅ Services Status

All four services are running:

- ✅ Frontend (Next.js) - Port 3001
- ✅ Backend (Spring Boot) - Port 8080
- ✅ PostgreSQL - Port 5432
- ✅ Redis - Port 6379

### 🎯 What You Can Do Now

1. **Open the application**: Visit http://localhost:3001 in your browser
2. **Register a new account**: Create your first user
3. **Create posts**: Share text and images (image uploads require B2 credentials)
4. **Follow users**: Build your social network
5. **View your feed**: See posts from users you follow

### 🔧 Useful Commands

```bash
# View all service logs
docker compose logs -f

# View specific service logs
docker compose logs -f backend
docker compose logs -f frontend

# Check service status
docker compose ps

# Stop all services
docker compose down

# Stop and remove all data
docker compose down -v

# Restart a specific service
docker compose restart backend
docker compose restart frontend

# Rebuild and restart
docker compose up -d --build
```

### 🗄️ Database Access

Connect to PostgreSQL:

```bash
docker compose exec postgres psql -U postgres -d minisocial
```

Useful SQL commands:

```sql
-- View all users
SELECT id, email, follower_count, following_count FROM users;

-- View all posts
SELECT p.id, u.email, p.content, p.created_at
FROM posts p
JOIN users u ON p.user_id = u.id
ORDER BY p.created_at DESC;

-- View follow relationships
SELECT
  follower.email as follower,
  following.email as following
FROM follows f
JOIN users follower ON f.follower_id = follower.id
JOIN users following ON f.following_id = following.id;
```

### 💾 Redis Cache Access

Connect to Redis:

```bash
docker compose exec redis redis-cli
```

Useful Redis commands:

```bash
# View all feed keys
KEYS feed:*

# View a user's feed (sorted set)
ZRANGE feed:1 0 -1 WITHSCORES

# Check cache stats
INFO stats
```

### 🔐 Environment Variables

The application uses these environment variables (configured in docker-compose.yml):

**Backend:**

- Database connection to PostgreSQL
- Redis connection for caching
- JWT secret for authentication
- B2 credentials for image storage (optional)

**Frontend:**

- API URL pointing to backend

### 📝 Testing the API

You can test the backend API directly:

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Create a post (replace TOKEN with JWT from login)
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"content":"Hello from MiniSocial!"}'

# Get feed (replace TOKEN with JWT from login)
curl http://localhost:8080/api/feed \
  -H "Authorization: Bearer TOKEN"
```

### 🖼️ Image Upload Setup (Optional)

To enable image uploads, you need Backblaze B2 credentials:

1. Sign up at https://www.backblaze.com/b2/cloud-storage.html
2. Create a bucket
3. Generate application keys
4. Update the `.env` file with your credentials:
   ```
   B2_ENDPOINT=https://s3.us-west-000.backblazeb2.com
   B2_ACCESS_KEY_ID=your-actual-key-id
   B2_SECRET_ACCESS_KEY=your-actual-secret-key
   B2_BUCKET_NAME=your-bucket-name
   ```
5. Restart the backend: `docker compose restart backend`

### 🐛 Troubleshooting

**Frontend not loading?**

- Check logs: `docker compose logs frontend`
- Ensure port 3001 is not in use

**Backend errors?**

- Check logs: `docker compose logs backend`
- Verify PostgreSQL is healthy: `docker compose ps`
- Check database connection: `docker compose exec postgres psql -U postgres -d minisocial`

**Database issues?**

- Reset database: `docker compose down -v && docker compose up -d`
- This will delete all data and start fresh

**Redis connection issues?**

- Check Redis: `docker compose exec redis redis-cli ping`
- Should return "PONG"

### 🎉 Next Steps

1. Explore the application at http://localhost:3001
2. Create multiple users to test the social features
3. Check out the code in `backend/` and `frontend/` directories
4. Review the API documentation in the backend controllers
5. Customize the application to your needs

### 📚 Additional Resources

- Full setup guide: See `DOCKER_SETUP.md`
- Requirements: See `.kiro/specs/mini-social-mvp/requirements.md`
- Design: See `.kiro/specs/mini-social-mvp/design.md`
- Tasks: See `.kiro/specs/mini-social-mvp/tasks.md`

---

**Enjoy building with MiniSocial! 🚀**
