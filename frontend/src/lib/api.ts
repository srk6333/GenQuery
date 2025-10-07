import axios from 'axios';

// API base configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// Types
export interface DatabaseConnection {
  type: 'MYSQL' | 'POSTGRESQL' | 'SQLITE' | 'H2';
  host?: string;
  port?: number;
  database: string;
  username?: string;
  password?: string;
  connectionString?: string;
}

export interface DatabaseSchema {
  databaseName: string;
  tables: TableInfo[];
  views: ViewInfo[];
  metadata: Record<string, any>;
}

export interface TableInfo {
  name: string;
  schema: string;
  type: string;
  columns: ColumnInfo[];
  indexes: IndexInfo[];
  foreignKeys: ForeignKeyInfo[];
  comment?: string;
}

export interface ColumnInfo {
  name: string;
  dataType: string;
  columnType: string;
  nullable: boolean;
  isPrimaryKey: boolean;
  isAutoIncrement: boolean;
  defaultValue?: any;
  comment?: string;
  maxLength?: number;
  precision?: number;
  scale?: number;
}

export interface IndexInfo {
  name: string;
  isUnique: boolean;
  columns: string[];
  type: string;
}

export interface ForeignKeyInfo {
  name: string;
  columnName: string;
  referencedTable: string;
  referencedColumn: string;
  onUpdate: string;
  onDelete: string;
}

export interface ViewInfo {
  name: string;
  schema: string;
  definition?: string;
  columns: ColumnInfo[];
  comment?: string;
}

export interface QueryRequest {
  naturalLanguageQuery: string;
  connectionId?: string;
  connectionDto: DatabaseConnection
  context?: Record<string, any>;
}

export interface QueryResponse {
  generatedSql: string;
  explanation: string;
  warnings: string[];
  isExecutable: boolean;
  metadata: QueryMetadata;
}

export interface QueryExecutionRequest {
  sql: string;
  connectionId?: string;
  limit?: number;
  offset?: number;
  dryRun?: boolean;
  connectionDto: DatabaseConnection;
}

export interface QueryExecutionResponse {
  results: Record<string, any>[];
  columnNames: string[];
  columnTypes: string[];
  rowCount: number;
  executionTimeMs: number;
  status: string;
  error?: string;
  metadata: QueryMetadata;
}

export interface QueryMetadata {
  queryType: string;
  tablesInvolved: string[];
  hasJoins: boolean;
  hasSubqueries: boolean;
  hasAggregations: boolean;
  complexity: string;
  timestamp: string;
}

export interface QueryValidationResponse {
  isValid: boolean;
  errors: string[];
  warnings: string[];
  suggestions: string[];
  sanitizedQuery?: string;
}

// API functions
export const databaseApi = {
  testConnection: (connection: DatabaseConnection) =>
    api.post<{connectionId: string; status: string; message: string}>('/database/test-connection', connection),
  
  getSchema: (connection: DatabaseConnection, connectionId?: string) =>
    api.post<{connectionId: string; schema: DatabaseSchema; status: string}>('/database/schema', connection, {
      params: connectionId ? { connectionId } : undefined
    }),
  
  getSupportedTypes: () =>
    api.get<{supportedTypes: string[]; status: string}>('/database/supported-types'),
  
  validateConnection: (connection: DatabaseConnection) =>
    api.post<{status: string; message: string}>('/database/validate-connection', connection),
};

export const queryApi = {
  generate: (request: QueryRequest, connectionId: string, connection: DatabaseConnection) =>
    api.post<{query: QueryResponse; status: string}>('/query/generate', request, {
      params: { connectionId },
      // Note: The backend expects connection info too, this might need adjustment
    }),
  
  validate: (sql: string) =>
    api.post<{validation: QueryValidationResponse; status: string}>('/query/validate', { sql }),
  
  execute: (request: QueryExecutionRequest, connectionId: string, connection: DatabaseConnection) =>
    api.post<{execution: QueryExecutionResponse; status: string}>('/query/execute', request, {
      params: { connectionId }
    }),
  
  explain: (sql: string, connectionId: string, connection: DatabaseConnection) =>
    api.post<{explanation: string; status: string}>('/query/explain', { sql }, {
      params: { connectionId }
    }),
  
  getHistory: (connectionId: string) =>
    api.get<{history: any[]; status: string; message: string}>('/query/history', {
      params: { connectionId }
    }),
  
  saveQuery: (query: Record<string, any>) =>
    api.post<{status: string; message: string}>('/query/save', query),
};