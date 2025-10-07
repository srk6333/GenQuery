'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Database,
  Table,
  Key,
  ChevronRight,
  ChevronDown,
  Loader,
  AlertCircle,
  Search,
} from 'lucide-react';
import { databaseApi, DatabaseConnection, TableInfo, ColumnInfo } from '@/lib/api';

interface SchemaViewerProps {
  connectionId: string;
  connection: DatabaseConnection;
}

interface TreeItemProps {
  children: React.ReactNode;
  label: string;
  icon: React.ReactNode;
  isExpanded: boolean;
  onToggle: () => void;
  level?: number;
}

function TreeItem({ children, label, icon, isExpanded, onToggle, level = 0 }: TreeItemProps) {
  return (
    <div>
      <div
        className={`flex items-center gap-2 py-2 px-2 hover:bg-gray-50 cursor-pointer rounded-md ${
          level > 0 ? 'ml-4' : ''
        }`}
        onClick={onToggle}
      >
        {isExpanded ? (
          <ChevronDown className="w-4 h-4 text-gray-400" />
        ) : (
          <ChevronRight className="w-4 h-4 text-gray-400" />
        )}
        {icon}
        <span className="text-sm font-medium text-gray-700 truncate">{label}</span>
      </div>
      {isExpanded && (
        <div className="ml-6">
          {children}
        </div>
      )}
    </div>
  );
}

function ColumnItem({ column }: { column: ColumnInfo }) {
  const getColumnIcon = () => {
    if (column.isPrimaryKey) return <Key className="w-4 h-4 text-yellow-600" />;
    return <div className="w-4 h-4" />;
  };

  return (
    <div className="flex items-center gap-2 py-1 px-2 text-sm">
      {getColumnIcon()}
      <span className={`font-mono ${column.isPrimaryKey ? 'font-bold' : ''}`}>
        {column.name}
      </span>
      <span className="text-gray-500">({column.columnType})</span>
      {!column.nullable && (
        <span className="text-xs text-red-600 bg-red-100 px-1 rounded">NOT NULL</span>
      )}
      {column.isAutoIncrement && (
        <span className="text-xs text-blue-600 bg-blue-100 px-1 rounded">AUTO</span>
      )}
    </div>
  );
}

function TableItem({ table }: { table: TableInfo }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [showColumns, setShowColumns] = useState(false);

  return (
    <TreeItem
      label={`${table.name} (${table.columns.length} columns)`}
      icon={<Table className="w-4 h-4 text-green-600" />}
      isExpanded={isExpanded}
      onToggle={() => setIsExpanded(!isExpanded)}
      level={1}
    >
      <TreeItem
        label={`Columns (${table.columns.length})`}
        icon={<Database className="w-4 h-4 text-gray-600" />}
        isExpanded={showColumns}
        onToggle={() => setShowColumns(!showColumns)}
        level={2}
      >
        <div className="space-y-1">
          {table.columns.map((column, idx) => (
            <ColumnItem key={idx} column={column} />
          ))}
        </div>
      </TreeItem>
    </TreeItem>
  );
}

export function SchemaViewer({ connectionId, connection }: SchemaViewerProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [showTables, setShowTables] = useState(true);

  const { data, isLoading, error } = useQuery({
    queryKey: ['database', 'schema', connectionId],
    queryFn: () => databaseApi.getSchema(connection, connectionId).then(res => res.data),
  });

  const schema = data?.schema;

  const filteredTables = schema?.tables.filter(table =>
    table.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    table.columns.some(col => col.name.toLowerCase().includes(searchTerm.toLowerCase()))
  ) || [];

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <Database className="w-6 h-6 text-blue-600" />
          <h2 className="text-xl font-semibold">Database Schema</h2>
        </div>
        <div className="flex items-center justify-center py-8">
          <Loader className="w-6 h-6 animate-spin text-blue-600" />
          <span className="ml-2 text-gray-600">Loading schema...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <Database className="w-6 h-6 text-blue-600" />
          <h2 className="text-xl font-semibold">Database Schema</h2>
        </div>
        <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-md">
          <AlertCircle className="w-4 h-4" />
          <span className="text-sm">
            Failed to load schema
          </span>
        </div>
      </div>
    );
  }

  if (!schema) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <Database className="w-6 h-6 text-blue-600" />
          <h2 className="text-xl font-semibold">Database Schema</h2>
        </div>
        <div className="text-center py-8 text-gray-600">
          No schema data available
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center gap-3 mb-6">
        <Database className="w-6 h-6 text-blue-600" />
        <h2 className="text-xl font-semibold">Database Schema</h2>
        <span className="text-sm text-gray-600">({schema.databaseName})</span>
      </div>

      <div className="relative mb-4">
        <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
        <input
          type="text"
          placeholder="Search tables and columns..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="max-h-96 overflow-y-auto">
        <TreeItem
          label={`Tables (${filteredTables.length})`}
          icon={<Table className="w-4 h-4 text-green-600" />}
          isExpanded={showTables}
          onToggle={() => setShowTables(!showTables)}
        >
          {filteredTables.map((table, idx) => (
            <TableItem key={idx} table={table} />
          ))}
        </TreeItem>
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200 text-xs text-gray-600">
        <div className="flex flex-wrap gap-4">
          <span>Product: {schema.metadata?.databaseProductName}</span>
          <span>Version: {schema.metadata?.databaseProductVersion}</span>
        </div>
      </div>
    </div>
  );
}