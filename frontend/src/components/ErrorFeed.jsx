import React, { useState } from 'react';
import { AlertTriangle, Bot, ChevronDown, ChevronRight, Sparkles, Clock, ShieldAlert } from 'lucide-react';

export const ErrorFeed = ({ errors = [], onDiagnoseError, onScanWindow, scanningWindow }) => {
  const [expandedRow, setExpandedRow] = useState(null);

  const toggleRow = (id) => {
    setExpandedRow(expandedRow === id ? null : id);
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-lg overflow-hidden">
      {/* Header with System Scan Button */}
      <div className="p-5 border-b border-slate-800 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-base font-semibold text-white">Incident Error Logs</h3>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-red-500/10 text-red-400 border border-red-500/20">
              {errors.length} Active Events
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">Real-time application runtime exceptions and stack traces</p>
        </div>

        <button
          onClick={onScanWindow}
          disabled={scanningWindow}
          className="flex items-center gap-2 px-3.5 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-medium transition-colors shadow-lg shadow-purple-500/20 disabled:opacity-50"
        >
          <Sparkles size={14} className={scanningWindow ? 'animate-spin' : ''} />
          <span>{scanningWindow ? 'Scanning...' : 'AI System Health Scan (15m)'}</span>
        </button>
      </div>

      {/* Errors Table / List */}
      {errors.length === 0 ? (
        <div className="py-16 flex flex-col items-center justify-center text-center text-slate-500">
          <div className="p-3.5 rounded-full bg-slate-800/60 mb-2.5 text-emerald-400">
            <ShieldAlert size={26} />
          </div>
          <p className="text-sm font-medium text-slate-300">All Systems Nominal</p>
          <p className="text-xs text-slate-500 max-w-sm mt-1">
            No runtime errors captured for this service. As errors occur, your SDK will stream them here.
          </p>
        </div>
      ) : (
        <div className="divide-y divide-slate-800/80">
          {errors.map((err) => {
            const isExpanded = expandedRow === err.id;
            const timeStr = err.timestamp ? new Date(err.timestamp).toLocaleString() : 'Unknown';

            return (
              <div key={err.id || Math.random()} className="hover:bg-slate-850/50 transition-colors">
                <div
                  className="p-4 flex items-start justify-between gap-4 cursor-pointer"
                  onClick={() => toggleRow(err.id)}
                >
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <button className="text-slate-500 hover:text-slate-300 mt-0.5">
                      {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </button>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-red-500/20 text-red-400 border border-red-500/30">
                          {err.logLevel || 'ERROR'}
                        </span>
                        <span className="text-xs text-slate-400 flex items-center gap-1 font-mono">
                          <Clock size={12} /> {timeStr}
                        </span>
                        {err.metadata?.serviceName && (
                          <span className="text-[11px] text-slate-500 bg-slate-800 px-2 py-0.2 rounded font-mono">
                            {err.metadata.serviceName}
                          </span>
                        )}
                        {err.diagnosis && (
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 flex items-center gap-1">
                            <Sparkles size={10} /> Diagnosed
                          </span>
                        )}
                      </div>

                      <p className="text-xs font-mono text-slate-200 truncate pr-2">
                        {err.message}
                      </p>
                    </div>
                  </div>

                  {/* AI Diagnose Button */}
                  <div className="flex-shrink-0" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={() => onDiagnoseError(err)}
                      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all border ${
                        err.diagnosis
                          ? 'bg-cyan-950/40 border-cyan-500/40 text-cyan-300 hover:bg-cyan-900/60'
                          : 'bg-slate-800 hover:bg-cyan-600/90 text-slate-300 hover:text-white border-slate-700/60 hover:border-cyan-500'
                      }`}
                    >
                      <Bot size={14} className={err.diagnosis ? 'text-cyan-400' : 'text-cyan-400 group-hover:text-white'} />
                      <span>{err.diagnosis ? 'View Diagnosis' : 'Diagnose'}</span>
                    </button>
                  </div>
                </div>

                {/* Expanded Stack Trace */}
                {isExpanded && (
                  <div className="px-12 pb-4 pt-1 bg-slate-950/40">
                    <div className="p-3 bg-slate-950 rounded-lg border border-slate-800/80">
                      <div className="flex items-center justify-between text-[11px] text-slate-500 mb-1.5 font-mono">
                        <span>MongoDB Document ID: {err.id}</span>
                        <span>Level: {err.logLevel}</span>
                      </div>
                      <pre className="text-xs font-mono text-slate-300 whitespace-pre-wrap overflow-x-auto leading-relaxed">
                        {err.message}
                      </pre>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
