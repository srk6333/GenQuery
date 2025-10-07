'use client';

import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Send, Loader, Play, CheckCircle, AlertCircle, MessageSquare } from 'lucide-react';
import { Editor } from '@monaco-editor/react';
import { queryApi, DatabaseConnection, QueryResponse, QueryExecutionResponse } from '@/lib/api';

interface QueryInterfaceProps {
  connectionId: string;
  connection: DatabaseConnection;
}

interface ChatMessage {
  id: string;
  type: 'user' | 'assistant' | 'system';
  content: string;
  sql?: string;
  timestamp: Date;
}

export function QueryInterface({ connectionId, connection }: QueryInterfaceProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      type: 'system',
      content: 'Connected to database! Ask me anything in natural language and I\'ll generate SQL queries for you.',
      timestamp: new Date(),
    }
  ]);
  const [input, setInput] = useState('');
  const [currentSql, setCurrentSql] = useState('');
  const [executionResults, setExecutionResults] = useState<QueryExecutionResponse | null>(null);

  const generateQueryMutation = useMutation({
    mutationFn: (query: string) => queryApi.generate(
      { naturalLanguageQuery: query, connectionDto : connection },
      connectionId,
      connection
    ),
    onSuccess: (response) => {
      const queryResponse: QueryResponse = response.data.query;
      
      // Add user message
      const userMessage: ChatMessage = {
        id: Date.now() + '_user',
        type: 'user',
        content: input,
        timestamp: new Date(),
      };

      // Add assistant message
      const assistantMessage: ChatMessage = {
        id: Date.now() + '_assistant',
        type: 'assistant',
        content: queryResponse.explanation || 'Here\'s the SQL query I generated:',
        sql: queryResponse.generatedSql,
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, userMessage, assistantMessage]);
      setCurrentSql(queryResponse.generatedSql);
      setInput('');
    },
    onError: (error) => {
      const errorMessage: ChatMessage = {
        id: Date.now() + '_error',
        type: 'system',
        content: 'Failed to generate SQL query. Please try again.',
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, errorMessage]);
    },
  });

  const executeQueryMutation = useMutation({
    mutationFn: (sql: string) => queryApi.execute(
      { sql, dryRun: false, limit: 100, connectionDto: connection },
      connectionId,
      connection
    ),
    onSuccess: (response) => {
      setExecutionResults(response.data.execution);
    },
  });

  const handleSendMessage = () => {
    if (!input.trim()) return;
    generateQueryMutation.mutate(input);
  };

  const handleExecuteQuery = () => {
    if (!currentSql.trim()) return;
    executeQueryMutation.mutate(currentSql);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center gap-3 mb-6">
        <MessageSquare className="w-6 h-6 text-blue-600" />
        <h2 className="text-xl font-semibold">Query Assistant</h2>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chat Interface */}
        <div className="space-y-4">
          <div className="border border-gray-200 rounded-lg">
            <div className="p-4 border-b border-gray-200">
              <h3 className="font-medium text-gray-900">Conversation</h3>
            </div>
            
            <div className="h-64 overflow-y-auto p-4 space-y-3">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                      message.type === 'user'
                        ? 'bg-blue-600 text-white'
                        : message.type === 'system'
                        ? 'bg-yellow-50 text-yellow-800 border border-yellow-200'
                        : 'bg-gray-100 text-gray-900'
                    }`}
                  >
                    <div className="text-sm">
                      {message.content}
                    </div>
                    {message.sql && (
                      <div className="mt-2 p-2 bg-white bg-opacity-10 rounded text-xs font-mono">
                        {message.sql.slice(0, 100)}
                        {message.sql.length > 100 && '...'}
                      </div>
                    )}
                    <div className="text-xs opacity-70 mt-1">
                      {message.timestamp.toLocaleTimeString()}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="p-4 border-t border-gray-200">
              <div className="flex gap-2">
                <textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="Ask me to generate a SQL query..."
                  className="flex-1 p-2 border border-gray-300 rounded-md resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 text-black"
                  rows={2}
                />
                <button
                  onClick={handleSendMessage}
                  disabled={!input.trim() || generateQueryMutation.isPending}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  {generateQueryMutation.isPending ? (
                    <Loader className="w-4 h-4 animate-spin" />
                  ) : (
                    <Send className="w-4 h-4" />
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* SQL Editor */}
        <div className="space-y-4">
          <div className="border border-gray-200 rounded-lg">
            <div className="p-4 border-b border-gray-200 flex items-center justify-between">
              <h3 className="font-medium text-gray-900">SQL Query</h3>
              <button
                onClick={handleExecuteQuery}
                disabled={!currentSql.trim() || executeQueryMutation.isPending}
                className="px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                {executeQueryMutation.isPending ? (
                  <Loader className="w-3 h-3 animate-spin" />
                ) : (
                  <Play className="w-3 h-3" />
                )}
                Execute
              </button>
            </div>
            
            <div className="h-48">
              <Editor
                height="100%"
                defaultLanguage="sql"
                value={currentSql}
                onChange={(value) => setCurrentSql(value || '')}
                theme="vs-light"
                options={{
                  minimap: { enabled: false },
                  scrollBeyondLastLine: false,
                  fontSize: 13,
                  wordWrap: 'on',
                  automaticLayout: true,
                }}
              />
            </div>
          </div>

          {/* Execution Results */}
          {(executionResults || executeQueryMutation.error) && (
            <div className="border border-gray-200 rounded-lg">
              <div className="p-4 border-b border-gray-200">
                <h3 className="font-medium text-gray-900">Results</h3>
              </div>
              
              <div className="p-4">
                {executeQueryMutation.error && (
                  <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-md mb-4">
                    <AlertCircle className="w-4 h-4" />
                    <span className="text-sm">
                      Query execution failed
                    </span>
                  </div>
                )}

                {executionResults && executionResults.status === 'SUCCESS' && (
                  <>
                    <div className="flex items-center gap-2 text-green-600 bg-green-50 p-3 rounded-md mb-4">
                      <CheckCircle className="w-4 h-4" />
                      <span className="text-sm">
                        Query executed successfully ({executionResults.rowCount} rows, {executionResults.executionTimeMs}ms)
                      </span>
                    </div>

                    {executionResults.results.length > 0 && (
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                          <thead>
                            <tr className="border-b border-gray-200">
                              {executionResults.columnNames.map((col, idx) => (
                                <th key={idx} className="text-left py-2 px-3 font-medium text-gray-900">
                                  {col}
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {executionResults.results.slice(0, 10).map((row, idx) => (
                              <tr key={idx} className="border-b border-gray-100">
                                {executionResults.columnNames.map((col, colIdx) => (
                                  <td key={colIdx} className="py-2 px-3 text-gray-700 max-w-xs truncate">
                                    {String(row[col] ?? '')}
                                  </td>
                                ))}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {executionResults.results.length > 10 && (
                          <p className="text-xs text-gray-500 mt-2">
                            Showing first 10 rows of {executionResults.results.length}
                          </p>
                        )}
                      </div>
                    )}
                  </>
                )}

                {executionResults && executionResults.status === 'ERROR' && (
                  <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-md">
                    <AlertCircle className="w-4 h-4" />
                    <span className="text-sm">
                      {executionResults.error}
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}