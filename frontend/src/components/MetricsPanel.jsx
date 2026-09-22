import React from 'react';
import { Cpu, HardDrive, TrendingUp, Clock } from 'lucide-react';

export const MetricsPanel = ({ metrics = [], appName }) => {
  const latest = metrics.length > 0 ? metrics[0] : null;

  const cpuPercent = latest ? Math.min(100, Math.max(0, (latest.cpuUsage || 0) * 100)) : 0;
  // Convert bytes to MB
  const memoryMb = latest ? Math.round((latest.memoryUsed || 0) / (1024 * 1024)) : 0;

  // Format historical trend points for SVG sparkline (chronological order)
  const chartData = [...metrics].reverse();
  const maxCpu = Math.max(...chartData.map((m) => (m.cpuUsage || 0) * 100), 10);
  const maxMem = Math.max(...chartData.map((m) => Math.round((m.memoryUsed || 0) / (1024 * 1024))), 50);

  const getCpuColor = (pct) => {
    if (pct >= 85) return 'text-red-400 bg-red-500';
    if (pct >= 70) return 'text-amber-400 bg-amber-500';
    return 'text-emerald-400 bg-emerald-500';
  };

  const formattedTime = latest?.timestamp
    ? new Date(latest.timestamp).toLocaleTimeString()
    : 'No data yet';

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
      {/* 1. CPU Metric Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg relative overflow-hidden">
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
            <Cpu size={16} className="text-blue-400" /> Process CPU Usage
          </span>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${getCpuColor(cpuPercent).split(' ')[0]} bg-slate-800`}>
            {cpuPercent.toFixed(1)}%
          </span>
        </div>

        <div className="text-2xl font-bold text-white tracking-tight mb-3">
          {cpuPercent.toFixed(1)}%
        </div>

        <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
          <div
            className={`h-full transition-all duration-500 rounded-full ${getCpuColor(cpuPercent).split(' ')[1]}`}
            style={{ width: `${cpuPercent}%` }}
          />
        </div>

        <div className="mt-3 flex items-center justify-between text-[11px] text-slate-500">
          <span>Target: &lt; 70%</span>
          <span className="flex items-center gap-1">
            <Clock size={11} /> {formattedTime}
          </span>
        </div>
      </div>

      {/* 2. Memory Metric Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg relative overflow-hidden">
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
            <HardDrive size={16} className="text-indigo-400" /> JVM Memory Used
          </span>
          <span className="text-xs font-semibold px-2 py-0.5 rounded-full text-indigo-400 bg-slate-800">
            {memoryMb} MB
          </span>
        </div>

        <div className="text-2xl font-bold text-white tracking-tight mb-3">
          {memoryMb} <span className="text-sm font-normal text-slate-400">MB</span>
        </div>

        <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
          <div
            className="h-full transition-all duration-500 rounded-full bg-indigo-500"
            style={{ width: `${Math.min(100, (memoryMb / Math.max(memoryMb, 512)) * 100)}%` }}
          />
        </div>

        <div className="mt-3 flex items-center justify-between text-[11px] text-slate-500">
          <span>Active heap utilization</span>
          <span className="flex items-center gap-1">
            <Clock size={11} /> {formattedTime}
          </span>
        </div>
      </div>

      {/* 3. Historical Sparkline Trend */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg flex flex-col justify-between">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-medium text-slate-400 flex items-center gap-1.5">
            <TrendingUp size={16} className="text-emerald-400" /> Telemetry Trend
          </span>
          <span className="text-[11px] text-slate-500">Last {chartData.length} pulses</span>
        </div>

        {chartData.length > 1 ? (
          <div className="h-16 w-full flex items-end gap-1.5 pt-2">
            {chartData.map((item, idx) => {
              const val = (item.cpuUsage || 0) * 100;
              const heightPct = Math.max(10, Math.min(100, (val / maxCpu) * 100));
              return (
                <div key={idx} className="flex-1 flex flex-col items-center gap-1 h-full justify-end group relative">
                  <div
                    className="w-full rounded-t bg-blue-500/60 hover:bg-blue-400 transition-all"
                    style={{ height: `${heightPct}%` }}
                  />
                  {/* Tooltip on hover */}
                  <div className="absolute -top-7 hidden group-hover:block bg-slate-950 text-white text-[10px] px-1.5 py-0.5 rounded border border-slate-700 whitespace-nowrap z-10">
                    {val.toFixed(1)}%
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="h-16 flex items-center justify-center text-xs text-slate-500 italic">
            Waiting for more telemetry data points...
          </div>
        )}

        <div className="mt-2 text-[11px] text-slate-500 flex justify-between">
          <span>Oldest</span>
          <span>Latest</span>
        </div>
      </div>
    </div>
  );
};
