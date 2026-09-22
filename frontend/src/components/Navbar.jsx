import React from 'react';
import { Activity, Plus, LogOut, User as UserIcon, RefreshCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Navbar = ({
  projects = [],
  selectedProject,
  onSelectProject,
  onOpenNewProject,
  onRefresh,
  refreshing,
}) => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-40 px-6 flex items-center justify-between">
      {/* Brand & Active App Selector */}
      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-blue-600 text-white shadow-lg shadow-blue-500/20">
            <Activity size={18} />
          </div>
          <div>
            <span className="font-bold text-base tracking-tight text-white flex items-center gap-1.5">
              LogMonito <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/30">AI Live</span>
            </span>
          </div>
        </div>

        {/* Project Selector */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500">Service:</span>
          {projects.length > 0 ? (
            <select
              value={selectedProject || ''}
              onChange={(e) => onSelectProject(e.target.value)}
              className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs font-medium text-white focus:outline-none focus:border-blue-500 transition-colors"
            >
              {projects.map((proj) => (
                <option key={proj} value={proj}>
                  {proj}
                </option>
              ))}
            </select>
          ) : (
            <span className="text-xs text-slate-400 italic">No services yet</span>
          )}

          <button
            onClick={onOpenNewProject}
            className="flex items-center gap-1 px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-medium transition-colors border border-slate-700/60"
            title="Create New Project"
          >
            <Plus size={14} /> New Project
          </button>
        </div>
      </div>

      {/* Actions: Refresh & User Menu */}
      <div className="flex items-center gap-4">
        <button
          onClick={onRefresh}
          disabled={refreshing}
          className={`p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 text-slate-300 transition-all ${
            refreshing ? 'opacity-50 cursor-not-allowed' : ''
          }`}
          title="Refresh Telemetry"
        >
          <RefreshCw size={15} className={refreshing ? 'animate-spin text-blue-400' : ''} />
        </button>

        <div className="h-4 w-px bg-slate-800" />

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-xs text-slate-300">
            <div className="w-7 h-7 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-blue-400 font-medium">
              <UserIcon size={14} />
            </div>
            <span className="hidden sm:inline font-medium">{user?.name || user?.email}</span>
          </div>

          <button
            onClick={logout}
            className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
            title="Sign Out"
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>
    </header>
  );
};
