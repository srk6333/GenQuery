# SQL Assistant - AI-Powered Database Query Tool

A modern web application that connects to databases, understands natural language questions, and generates SQL queries using AI.

## 🚀 Features

- **Multi-Database Support**: Connect to MySQL, PostgreSQL, SQLite
- **AI-Powered Queries**: Natural language to SQL conversion using OpenAI
- **Schema Visualization**: Interactive database schema explorer
- **Query Editor**: Monaco editor with SQL syntax highlighting
- **Safe Execution**: Query validation and sanitization
- **Results Display**: Clean tabular interface for query results

## 🏗️ Architecture

- **Frontend**: React/Next.js with TypeScript
- **Backend**: Spring Boot with Java 17
- **AI Integration**: OpenAI GPT-4 for text-to-SQL
- **Database**: Support for MySQL, PostgreSQL, SQLite

## 📁 Project Structure

```
sql-assistant/
├── frontend/          # React/Next.js application
├── backend/           # Spring Boot API server
├── docs/             # Documentation
└── README.md         # This file
```

## 🛠️ Quick Start

### Method 1: Docker Compose (Recommended)

1. **Prerequisites**
   - Docker and Docker Compose installed
   - OpenAI API key

2. **Setup**
   ```bash
   # Clone the repository
   git clone <repository-url>
   cd sql-assistant
   
   # Create environment file
   echo "OPENAI_API_KEY=your-openai-api-key" > .env
   
   # Start all services
   docker-compose up -d
   ```

3. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - MySQL: localhost:3306 (demo_user/demo_password)
   - PostgreSQL: localhost:5432 (demo_user/demo_password)

### Method 2: Manual Setup

#### Backend (Spring Boot)
```bash
cd backend
# Set environment variable
export OPENAI_API_KEY=your-openai-api-key
./mvnw spring-boot:run
```

#### Frontend (Next.js)
```bash
cd frontend
npm install
npm run dev
```

## 🚀 Usage Guide

### 1. Connect to Database
- Choose database type (MySQL, PostgreSQL, SQLite, H2)
- Enter connection details
- Test connection

### 2. Explore Schema
- View database tables and columns
- See relationships and constraints
- Search for specific tables/columns

### 3. Generate Queries
- Ask questions in natural language:
  - "Show me all users who placed orders this month"
  - "What are the best selling products?"
  - "Find customers who haven't ordered recently"
  - "Show me revenue by category"

### 4. Review and Execute
- Review generated SQL query
- Edit if needed using the SQL editor
- Execute query safely (read-only)
- View results in a clean table format

## 📊 Demo Database

Use the included sample database for testing:

```bash
# Using Docker (includes sample data)
docker-compose up -d mysql

# Or manually import sample data
mysql -u root -p < docs/sample-data.sql
```

**Connection Details:**
- Host: localhost
- Port: 3306
- Database: sql_assistant_demo  
- Username: demo_user
- Password: demo_password

**Sample Questions to Try:**
- "Show me all users and their total orders"
- "What products are running low on stock?"
- "Find the most expensive orders"
- "Show me sales by category"
- "Which users have never placed an order?"

## 🔧 Environment Variables

Create `.env` files with your configuration:

**Backend (.env)**
```
OPENAI_API_KEY=your-openai-api-key
```

**Frontend (.env.local)**
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

See individual README files in each directory for detailed setup instructions.

## 📝 License

MIT License - see LICENSE file for details.