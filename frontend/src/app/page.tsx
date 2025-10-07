'use client';

import { useState } from 'react';
import { DatabaseConnection } from '@/components/DatabaseConnection';
import { SchemaViewer } from '@/components/SchemaViewer';
import { QueryInterface } from '@/components/QueryInterface';
import { DatabaseConnection as DBConnection } from '@/lib/api';

export default function Home() {
  const [connectionId, setConnectionId] = useState<string | null>(null);
  const [connection, setConnection] = useState<DBConnection | null>(null);

  const handleConnectionSuccess = (id: string, conn: DBConnection) => {
    setConnectionId(id);
    setConnection(conn);
  };

  const resetConnection = () => {
    setConnectionId(null);
    setConnection(null);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">SQL</span>
              </div>
              <h1 className="text-2xl font-bold text-gray-900">SQL Assistant</h1>
              <span className="text-sm text-gray-500">AI-Powered Database Query Tool</span>
            </div>
            {connectionId && (
              <button
                onClick={resetConnection}
                className="text-sm text-gray-600 hover:text-gray-900 underline"
              >
                Change Connection
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {!connectionId || !connection ? (
          /* Connection Setup */
          <div className="max-w-md mx-auto">
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-gray-900 mb-4">
                Connect to Your Database
              </h2>
              <p className="text-gray-600">
                Start by connecting to your database. We support MySQL, PostgreSQL, SQLite, and H2.
              </p>
            </div>
            <DatabaseConnection onConnectionSuccess={handleConnectionSuccess} />
          </div>
        ) : (
          /* Main Application */
          <div className="space-y-8">
            {/* Connection Info */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  <span className="text-sm font-medium text-blue-900">
                    Connected to {connection.type} database: {connection.database}
                  </span>
                </div>
                <span className="text-xs text-blue-600">ID: {connectionId}</span>
              </div>
            </div>

            {/* Two Column Layout */}
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
              {/* Schema Sidebar */}
              <div className="xl:col-span-1">
                <SchemaViewer connectionId={connectionId} connection={connection} />
              </div>
              
              {/* Query Interface */}
              <div className="xl:col-span-2">
                <QueryInterface connectionId={connectionId} connection={connection} />
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center text-gray-500 text-sm">
            <p>Built with Next.js, Spring Boot, and OpenAI • Secure & Read-Only Query Execution</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
