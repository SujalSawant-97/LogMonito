import React, { useState, useEffect, useCallback } from 'react';
import { Navbar } from '../components/Navbar';
import { MetricsPanel } from '../components/MetricsPanel';
import { ErrorFeed } from '../components/ErrorFeed';
import { ApiKeyModal } from '../components/ApiKeyModal';
import { AiDiagnosticModal } from '../components/AiDiagnosticModal';
import { apiClient } from '../api/client';
import { PlusCircle, ShieldCheck } from 'lucide-react';

export const DashboardPage = () => {
  const [projects, setProjects] = useState(() => {
    const saved = localStorage.getItem('logmonito_projects');
    try {
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [selectedProject, setSelectedProject] = useState(() => {
    return localStorage.getItem('logmonito_selected_project') || '';
  });

  const [metrics, setMetrics] = useState([]);
  const [errors, setErrors] = useState([]);
  const [refreshing, setRefreshing] = useState(false);

  // Modals
  const [isKeyModalOpen, setIsKeyModalOpen] = useState(false);
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);
  const [activeLogForAi, setActiveLogForAi] = useState(null);
  const [aiInsight, setAiInsight] = useState('');
  const [aiDiagnosis, setAiDiagnosis] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');
  const [scanningWindow, setScanningWindow] = useState(false);

  // Keep projects list in sync with localStorage
  useEffect(() => {
    localStorage.setItem('logmonito_projects', JSON.stringify(projects));
  }, [projects]);

  useEffect(() => {
    if (selectedProject) {
      localStorage.setItem('logmonito_selected_project', selectedProject);
    }
  }, [selectedProject]);

  // Set default selected project if none selected
  useEffect(() => {
    if (!selectedProject && projects.length > 0) {
      setSelectedProject(projects[0]);
    }
  }, [projects, selectedProject]);

  // Telemetry Fetcher
  const fetchTelemetry = useCallback(async () => {
    if (!selectedProject) return;

    setRefreshing(true);
    try {
      const [metricsData, errorsData] = await Promise.allSettled([
        apiClient.getMetrics(selectedProject),
        apiClient.getErrors(selectedProject),
      ]);

      if (metricsData.status === 'fulfilled' && Array.isArray(metricsData.value)) {
        setMetrics(metricsData.value);
      }
      if (errorsData.status === 'fulfilled' && Array.isArray(errorsData.value)) {
        setErrors(errorsData.value);
      }
    } catch (err) {
      console.error('Failed to poll telemetry:', err);
    } finally {
      setRefreshing(false);
    }
  }, [selectedProject]);

  // Initial load and periodic polling every 15 seconds
  useEffect(() => {
    fetchTelemetry();
    const timer = setInterval(() => {
      fetchTelemetry();
    }, 15000);
    return () => clearInterval(timer);
  }, [fetchTelemetry]);

  // New Project Created Callback
  const handleProjectCreated = (newAppName) => {
    if (!projects.includes(newAppName)) {
      const updated = [newAppName, ...projects];
      setProjects(updated);
      setSelectedProject(newAppName);
    }
  };

  // AI Diagnostic 1: Single Error Analysis
  const handleDiagnoseError = async (log) => {
    setActiveLogForAi(log);
    setAiInsight('');
    setAiDiagnosis(null);
    setAiError('');
    setAiLoading(true);
    setIsAiModalOpen(true);

    try {
      const res = await apiClient.analyzeError(log.id, selectedProject);
      if (res && typeof res === 'object') {
        setAiDiagnosis(res);
        setAiInsight(res.insight || '');
      } else {
        setAiInsight(res || 'No insight returned by AI.');
      }
    } catch (err) {
      setAiError(err.message || 'Could not connect to AI Debugger. Ensure Python service is running.');
    } finally {
      setAiLoading(false);
    }
  };

  // AI Diagnostic 2: System Time Window Analysis (Last 15 minutes)
  const handleScanWindow = async () => {
    setActiveLogForAi(null);
    setAiInsight('');
    setAiError('');
    setAiLoading(true);
    setScanningWindow(true);
    setIsAiModalOpen(true);

    try {
      const res = await apiClient.analyzeWindow(selectedProject, 15);
      setAiInsight(res.insight || 'System scan complete.');
    } catch (err) {
      setAiError(err.message || 'Could not connect to the Local LLM. Ensure Ollama is running.');
    } finally {
      setAiLoading(false);
      setScanningWindow(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar
        projects={projects}
        selectedProject={selectedProject}
        onSelectProject={setSelectedProject}
        onOpenNewProject={() => setIsKeyModalOpen(true)}
        onRefresh={fetchTelemetry}
        refreshing={refreshing}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        {/* Welcome Empty State if no project exists */}
        {projects.length === 0 ? (
          <div className="py-20 flex flex-col items-center justify-center text-center max-w-md mx-auto">
            <div className="p-4 rounded-2xl bg-blue-500/10 text-blue-400 mb-4 border border-blue-500/20">
              <ShieldCheck size={36} />
            </div>
            <h2 className="text-xl font-bold text-white">Welcome to LogMonito</h2>
            <p className="text-xs text-slate-400 mt-2 mb-6">
              Create your first project to generate an API key and begin monitoring your Spring Boot microservices with AI-powered telemetry.
            </p>
            <button
              onClick={() => setIsKeyModalOpen(true)}
              className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-medium transition-all shadow-xl shadow-blue-500/25"
            >
              <PlusCircle size={17} /> Create Your First Project
            </button>
          </div>
        ) : (
          <>
            {/* Live Metrics Cards */}
            <MetricsPanel metrics={metrics} appName={selectedProject} />

            {/* Error Incident Feed */}
            <ErrorFeed
              errors={errors}
              onDiagnoseError={handleDiagnoseError}
              onScanWindow={handleScanWindow}
              scanningWindow={scanningWindow}
            />
          </>
        )}
      </main>

      {/* Project / API Key Generator Modal */}
      <ApiKeyModal
        isOpen={isKeyModalOpen}
        onClose={() => setIsKeyModalOpen(false)}
        onProjectCreated={handleProjectCreated}
      />

      {/* AI Diagnostic Modal */}
      <AiDiagnosticModal
        isOpen={isAiModalOpen}
        onClose={() => setIsAiModalOpen(false)}
        log={activeLogForAi}
        insight={aiInsight}
        diagnosis={aiDiagnosis}
        loading={aiLoading}
        error={aiError}
      />
    </div>
  );
};
