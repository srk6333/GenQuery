'use client';

import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Database, Loader, CheckCircle, AlertCircle } from 'lucide-react';
import { databaseApi, DatabaseConnection as DBConnection } from '@/lib/api';

interface DatabaseConnectionProps {
  onConnectionSuccess: (connectionId: string, connection: DBConnection) => void;
}

export function DatabaseConnection({ onConnectionSuccess }: DatabaseConnectionProps) {
  const [formData, setFormData] = useState<DBConnection>({
    type: 'MYSQL',
    host: 'localhost',
    port: 3306,
    database: '',
    username: '',
    password: '',
  });

  const { data: supportedTypes } = useQuery({
    queryKey: ['database', 'supported-types'],
    queryFn: () => databaseApi.getSupportedTypes().then(res => res.data),
  });

  const testConnectionMutation = useMutation({
    mutationFn: (connection: DBConnection) => databaseApi.testConnection(connection),
    onSuccess: (response) => {
      onConnectionSuccess(response.data.connectionId, formData);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    testConnectionMutation.mutate(formData);
  };

  const handleInputChange = (field: keyof DBConnection, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));
  };

  const showHostPort = formData.type === 'MYSQL' || formData.type === 'POSTGRESQL';

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center gap-3 mb-6">
        <Database className="w-6 h-6 text-blue-600" />
        <h2 className="text-xl font-semibold">Database Connection</h2>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Database Type
          </label>
          <select
            value={formData.type}
            onChange={(e) => handleInputChange('type', e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
          >
            <option value="MYSQL">MySQL</option>
            <option value="POSTGRESQL">PostgreSQL</option>
            <option value="SQLITE">SQLite</option>
            <option value="H2">H2 (In-Memory)</option>
          </select>
        </div>

        {showHostPort && (
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Host
              </label>
              <input
                type="text"
                value={formData.host || ''}
                onChange={(e) => handleInputChange('host', e.target.value)}
                placeholder="localhost"
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Port
              </label>
              <input
                type="number"
                value={formData.port || ''}
                onChange={(e) => handleInputChange('port', parseInt(e.target.value) || undefined)}
                placeholder={formData.type === 'MYSQL' ? '3306' : '5432'}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
              />
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Database Name
          </label>
          <input
            type="text"
            value={formData.database}
            onChange={(e) => handleInputChange('database', e.target.value)}
            placeholder={formData.type === 'SQLITE' ? '/path/to/database.db' : 'database_name'}
            required
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
          />
        </div>

        {showHostPort && (
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Username
              </label>
              <input
                type="text"
                value={formData.username || ''}
                onChange={(e) => handleInputChange('username', e.target.value)}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                type="password"
                value={formData.password || ''}
                onChange={(e) => handleInputChange('password', e.target.value)}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
              />
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Connection String (Optional)
          </label>
          <input
            type="text"
            value={formData.connectionString || ''}
            onChange={(e) => handleInputChange('connectionString', e.target.value)}
            placeholder="jdbc:mysql://localhost:3306/mydb"
            className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
          />
          <p className="text-xs text-gray-500 mt-1">
            Override the generated connection URL with a custom string
          </p>
        </div>

        <button
          type="submit"
          disabled={testConnectionMutation.isPending}
          className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {testConnectionMutation.isPending && <Loader className="w-4 h-4 animate-spin" />}
          Test Connection
        </button>

        {testConnectionMutation.isError && (
          <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-md">
            <AlertCircle className="w-4 h-4" />
            <span className="text-sm">
              {testConnectionMutation.error?.response?.data?.message || 'Connection failed'}
            </span>
          </div>
        )}

        {testConnectionMutation.isSuccess && (
          <div className="flex items-center gap-2 text-green-600 bg-green-50 p-3 rounded-md">
            <CheckCircle className="w-4 h-4" />
            <span className="text-sm">Connection successful!</span>
          </div>
        )}
      </form>
    </div>
  );
}