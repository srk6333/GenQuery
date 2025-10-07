# SQL Assistant Backend

Spring Boot API server for the SQL Assistant application.

## 🚀 Features

- **Multi-Database Support**: MySQL, PostgreSQL, SQLite, H2
- **Schema Introspection**: Automatic database structure analysis
- **AI-Powered Query Generation**: OpenAI GPT-4 integration
- **Query Validation**: SQL parsing and security checks
- **Safe Execution**: Read-only query enforcement
- **CORS Support**: Configured for frontend integration

## 🛠️ Prerequisites

- Java 17 or higher
- Maven 3.6+
- OpenAI API key

## 📦 Installation

1. **Clone and navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Install dependencies**
   ```bash
   ./mvnw clean install
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

## 🔧 Configuration

### Environment Variables

Create a `.env` file with:

```env
OPENAI_API_KEY=your-openai-api-key-here
```

### Application Properties

The application uses `application.yml` for configuration:

- **Server**: Runs on port 8080 with `/api` context path
- **CORS**: Allows requests from frontend (localhost:3000, 3001)
- **Query Limits**: 30s timeout, 1000 max rows
- **OpenAI**: Uses GPT-4 model

## 📋 API Endpoints

### Database Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/database/test-connection` | POST | Test database connection |
| `/api/database/schema` | POST | Get database schema |
| `/api/database/supported-types` | GET | Get supported database types |
| `/api/database/validate-connection` | POST | Validate connection parameters |

### Query Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/query/generate` | POST | Generate SQL from natural language |
| `/api/query/validate` | POST | Validate SQL query |
| `/api/query/execute` | POST | Execute SQL query |
| `/api/query/explain` | POST | Explain SQL query |
| `/api/query/history` | GET | Get query history |
| `/api/query/save` | POST | Save query |

## 🗄️ Database Support

### MySQL
```json
{
  "type": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "database": "mydb",
  "username": "user",
  "password": "pass"
}
```

### PostgreSQL
```json
{
  "type": "POSTGRESQL",
  "host": "localhost",
  "port": 5432,
  "database": "mydb",
  "username": "user",
  "password": "pass"
}
```

### SQLite
```json
{
  "type": "SQLITE",
  "database": "/path/to/database.db"
}
```

## 🔒 Security Features

- **Query Validation**: SQL parsing and syntax checking
- **Read-Only Enforcement**: Only SELECT queries allowed
- **Injection Prevention**: Pattern matching and sanitization
- **Connection Timeouts**: Prevents hanging connections
- **Row Limits**: Prevents excessive data retrieval

## 🚀 Development

### Project Structure
```
src/main/java/com/sqlassistant/backend/
├── controller/          # REST controllers
├── service/            # Business logic
├── dto/               # Data transfer objects
├── model/             # Domain models
└── config/            # Configuration classes
```

### Key Components

- **DatabaseService**: Manages database connections and schema introspection
- **AiService**: Integrates with OpenAI for query generation
- **QueryValidationService**: Validates and sanitizes SQL queries
- **DatabaseController**: REST endpoints for database operations
- **QueryController**: REST endpoints for query operations

## 🧪 Testing

Run tests with:
```bash
./mvnw test
```

The project includes:
- Unit tests for services
- Integration tests with TestContainers
- API endpoint tests

## 📊 Monitoring

The application includes:
- Spring Boot Actuator endpoints
- Health checks at `/api/actuator/health`
- Metrics at `/api/actuator/metrics`

## 🐛 Troubleshooting

### Common Issues

1. **Database Driver Not Found**
   - Ensure the database driver dependency is included
   - Check the connection type matches the driver

2. **OpenAI API Errors**
   - Verify the API key is set correctly
   - Check network connectivity
   - Ensure sufficient API credits

3. **Connection Timeout**
   - Verify database is running and accessible
   - Check firewall settings
   - Validate connection parameters

### Logs

Check application logs for detailed error information:
```bash
tail -f logs/sql-assistant.log
```

## 📄 License

MIT License - see the main project README for details.