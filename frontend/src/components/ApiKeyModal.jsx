import React, { useState } from 'react';
import { X, Copy, Check, KeyRound, Terminal, AlertCircle } from 'lucide-react';
import { apiClient } from '../api/client';

export const ApiKeyModal = ({ isOpen, onClose, onProjectCreated }) => {
  const [appName, setAppName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [createdApp, setCreatedApp] = useState(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [copiedYaml, setCopiedYaml] = useState(false);

  if (!isOpen) return null;

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (!appName.trim()) return;

    setLoading(true);
    setError('');
    try {
      const app = await apiClient.generateApiKey(appName.trim());
      setCreatedApp(app);
      if (onProjectCreated) {
        onProjectCreated(app.appName);
      }
    } catch (err) {
      setError(err.message || 'Failed to generate API Key');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text, setCopied) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const yamlSnippet = createdApp
    ? `spring:
  application:
    name: ${createdApp.appName}

monitor:
  server-url: http://localhost:8080
  api-key: ${createdApp.apiKey}
  log-level: WARN`
    : '';

  const handleClose = () => {
    setAppName('');
    setCreatedApp(null);
    setError('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-xl w-full p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        <button
          onClick={handleClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-200 transition-colors"
        >
          <X size={20} />
        </button>

        {!createdApp ? (
          <div>
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20">
                <KeyRound size={22} />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Create New Project</h3>
                <p className="text-xs text-slate-400">Generate an API Key for your Spring Boot application</p>
              </div>
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center gap-2">
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleGenerate} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Application Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. order-service, payment-api"
                  value={appName}
                  onChange={(e) => setAppName(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-white placeholder:text-slate-600 focus:outline-none focus:border-blue-500 transition-colors"
                  required
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  Must match the <code>spring.application.name</code> in your client application.
                </p>
              </div>

              <div className="flex justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-slate-200 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading || !appName.trim()}
                  className="px-4 py-2 text-xs font-medium bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition-colors disabled:opacity-50 flex items-center gap-2"
                >
                  {loading ? 'Generating...' : 'Generate API Key'}
                </button>
              </div>
            </form>
          </div>
        ) : (
          <div className="space-y-5">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-lg bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <Check size={22} />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Project Ready: {createdApp.appName}</h3>
                <p className="text-xs text-slate-400">Your SDK credentials have been generated</p>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1.5">Your Secret API Key</label>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  readOnly
                  value={createdApp.apiKey}
                  className="w-full font-mono text-xs px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-emerald-400 selection:bg-emerald-950 select-all"
                />
                <button
                  onClick={() => copyToClipboard(createdApp.apiKey, setCopiedKey)}
                  className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg transition-colors flex-shrink-0"
                  title="Copy API Key"
                >
                  {copiedKey ? <Check size={16} className="text-emerald-400" /> : <Copy size={16} />}
                </button>
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
                  <Terminal size={14} /> application.yml Configuration
                </span>
                <button
                  onClick={() => copyToClipboard(yamlSnippet, setCopiedYaml)}
                  className="text-[11px] text-blue-400 hover:text-blue-300 flex items-center gap-1"
                >
                  {copiedYaml ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
                  {copiedYaml ? 'Copied' : 'Copy Snippet'}
                </button>
              </div>
              <pre className="p-3 bg-slate-950 border border-slate-800 rounded-lg font-mono text-[11px] text-slate-300 overflow-x-auto">
                {yamlSnippet}
              </pre>
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={handleClose}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium rounded-lg transition-colors"
              >
                Go to Dashboard
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
