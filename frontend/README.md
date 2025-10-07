# SQL Assistant Frontend

React/Next.js frontend application for the SQL Assistant.

## 🚀 Features

- **Modern UI**: Clean, responsive design with Tailwind CSS
- **Database Connection**: Visual form for connecting to various database types
- **Schema Visualization**: Interactive tree view of database structure
- **AI Chat Interface**: Natural language to SQL query generation
- **SQL Editor**: Monaco editor with syntax highlighting
- **Query Execution**: Safe query execution with results display
- **Real-time Updates**: React Query for efficient data management

## 🛠️ Prerequisites

- Node.js 18 or higher
- npm or yarn
- Backend API running (see backend README)

## 📦 Installation

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your configuration
   ```

4. **Start development server**
   ```bash
   npm run dev
   ```

5. **Open in browser**
   ```
   http://localhost:3000
   ```

## 🔧 Configuration

### Environment Variables

Create a `.env.local` file with:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

### API Configuration

The frontend expects the backend API to be running on `http://localhost:8080/api` by default. Ensure the backend is started before using the frontend.

## 📋 Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm start` | Start production server |
| `npm run lint` | Run ESLint |

## 🏗️ Project Structure

```
src/
├── app/                    # Next.js app directory
│   ├── layout.tsx         # Root layout
│   ├── page.tsx           # Main page
│   └── globals.css        # Global styles
├── components/            # React components
│   ├── DatabaseConnection.tsx  # Connection form
│   ├── SchemaViewer.tsx        # Schema tree view
│   └── QueryInterface.tsx      # Chat and SQL editor
└── lib/                   # Utilities and configurations
    ├── api.ts             # API client and types
    └── query-client.tsx   # React Query provider
```

## 🎨 Key Components

### DatabaseConnection
- Form for entering database connection details
- Supports MySQL, PostgreSQL, SQLite, H2
- Real-time connection testing
- Validation and error handling

### SchemaViewer
- Interactive tree view of database schema
- Expandable tables and columns
- Search functionality
- Primary keys and foreign key indicators

### QueryInterface
- Split view with chat interface and SQL editor
- Natural language input for query generation
- Monaco editor with SQL syntax highlighting
- Query execution with results table

## 📄 License

MIT License - see the main project README for details.
