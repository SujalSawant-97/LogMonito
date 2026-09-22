import React, { useState } from 'react';
import { X, Bot, Sparkles, Copy, Check, AlertTriangle, Lightbulb, AlertCircle, Search, Wrench } from 'lucide-react';

export const AiDiagnosticModal = ({ isOpen, onClose, log, insight, diagnosis, loading, error }) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const handleCopy = () => {
    const textToCopy = diagnosis
      ? `Summary:\n${diagnosis.summary}\n\nCause:\n${diagnosis.cause}\n\nSolution:\n${diagnosis.solution}`
      : (insight || '');
    if (textToCopy) {
      navigator.clipboard.writeText(textToCopy);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-2xl w-full p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150 max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
              <Bot size={22} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-semibold text-white">AI Incident Diagnosis</h3>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 flex items-center gap-1">
                  <Sparkles size={11} /> Antigravity SRE
                </span>
              </div>
              <p className="text-xs text-slate-400">Automated root cause analysis & recommended resolution</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 transition-colors p-1"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content Body */}
        <div className="py-4 overflow-y-auto flex-1 space-y-4">
          {/* Target Error Summary */}
          {log && (
            <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
              <div className="flex items-center gap-2 mb-1">
                <AlertTriangle size={14} className="text-red-400" />
                <span className="text-[11px] font-semibold text-red-400">Analyzed Exception</span>
                <span className="text-[10px] text-slate-500 font-mono">ID: {log.id}</span>
              </div>
              <pre className="text-xs font-mono text-slate-300 whitespace-pre-wrap line-clamp-3">
                {log.message}
              </pre>
            </div>
          )}

          {/* Loading State */}
          {loading && (
            <div className="py-12 flex flex-col items-center justify-center gap-3 text-center">
              <div className="p-4 rounded-full bg-purple-500/10 text-purple-400 animate-pulse">
                <Bot size={32} />
              </div>
              <div>
                <p className="text-sm font-medium text-white">Synthesizing Incident Diagnostics...</p>
                <p className="text-xs text-slate-500">Querying local LLM with error context & system telemetry</p>
              </div>
            </div>
          )}

          {/* Error State */}
          {error && !loading && (
            <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-xs flex items-center gap-2">
              <AlertTriangle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* Structured Diagnosis Content */}
          {!loading && (diagnosis || insight) && (
            <div className="space-y-3">
              {/* Copy Toolbar */}
              <div className="flex items-center justify-between px-1">
                <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  Automated Root Cause Triage
                </span>
                <button
                  onClick={handleCopy}
                  className="flex items-center gap-1.5 text-xs text-slate-300 hover:text-white transition-colors px-2.5 py-1 bg-slate-800/80 hover:bg-slate-700/80 rounded border border-slate-700/60"
                >
                  {copied ? <Check size={13} className="text-emerald-400" /> : <Copy size={13} />}
                  <span>{copied ? 'Copied' : 'Copy All'}</span>
                </button>
              </div>

              {diagnosis ? (
                <>
                  {/* Card 1: Summary */}
                  {diagnosis.summary && (
                    <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-1.5">
                      <div className="flex items-center gap-2 text-xs font-semibold text-rose-400">
                        <AlertCircle size={15} />
                        <span>1. Incident Summary</span>
                      </div>
                      <p className="text-xs text-slate-200 leading-relaxed pl-5 font-sans">
                        {diagnosis.summary}
                      </p>
                    </div>
                  )}

                  {/* Card 2: Root Cause */}
                  {diagnosis.cause && (
                    <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-1.5">
                      <div className="flex items-center gap-2 text-xs font-semibold text-amber-400">
                        <Search size={15} />
                        <span>2. Root Cause Analysis</span>
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed pl-5 font-mono whitespace-pre-wrap">
                        {diagnosis.cause}
                      </p>
                    </div>
                  )}

                  {/* Card 3: Solution & Code Patch */}
                  {diagnosis.solution && (
                    <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-2">
                      <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
                        <Wrench size={15} />
                        <span>3. Recommended Resolution & Code Fix</span>
                      </div>
                      <div className="p-3.5 bg-slate-900 rounded-lg text-xs font-mono text-emerald-200 border border-emerald-500/20 whitespace-pre-wrap leading-relaxed">
                        {diagnosis.solution}
                      </div>
                    </div>
                  )}
                </>
              ) : (
                /* Fallback Plain Insight */
                <div className="p-4 bg-slate-950/70 border border-slate-800 rounded-xl">
                  <div className="text-sm text-slate-200 leading-relaxed whitespace-pre-wrap font-sans bg-slate-900/50 p-4 rounded-lg border border-slate-800/80">
                    {insight}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
