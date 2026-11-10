import React, { useState } from 'react';
import { Play, CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { apiRequest } from '../api/client';

export default function ScenarioRunnerBar() {
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState(null);

  const runScenario = async (scenarioId) => {
    setRunning(true);
    setResult(null);
    try {
      const res = await apiRequest(`/api/scenarios/run/${scenarioId}`, { method: 'POST' });
      setResult(res);
      // Trigger a soft refresh event so open views reload their data
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) {
      setResult({ scenarioName: scenarioId, success: false, verificationNotes: [e.message] });
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="mb-6 bg-white border border-clinical-border rounded-lg p-3.5 clinical-card">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center space-x-2">
          <Play className="w-4 h-4 text-pharma-700" />
          <span className="text-xs font-semibold text-slate-800 uppercase tracking-wider">Deterministic Scenario Replay:</span>
        </div>

        <div className="flex items-center flex-wrap gap-1.5">
          <button
            onClick={() => runScenario('1')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded font-medium text-slate-700 transition"
          >
            1. Normal
          </button>
          <button
            onClick={() => runScenario('2')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-amber-50 hover:bg-amber-100 border border-amber-200 rounded font-medium text-amber-800 transition"
          >
            2. Minor Excursion
          </button>
          <button
            onClick={() => runScenario('3')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded font-medium text-rose-800 transition"
          >
            3. Major / Critical
          </button>
          <button
            onClick={() => runScenario('4')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 rounded font-medium text-indigo-800 transition"
          >
            4. Duplicate Event
          </button>
          <button
            onClick={() => runScenario('5')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-purple-50 hover:bg-purple-100 border border-purple-200 rounded font-medium text-purple-800 transition"
          >
            5. Out-of-Order
          </button>
          <button
            onClick={() => runScenario('6')}
            disabled={running}
            className="text-xs px-2.5 py-1 bg-teal-50 hover:bg-teal-100 border border-teal-200 rounded font-medium text-teal-800 transition"
          >
            6. Missing & Contradictory
          </button>
          <button
            onClick={() => runScenario('all')}
            disabled={running}
            className="text-xs px-3 py-1 bg-pharma-700 hover:bg-pharma-800 text-white rounded font-medium transition flex items-center space-x-1"
          >
            {running && <RefreshCw className="w-3 h-3 animate-spin mr-1" />}
            <span>Run All</span>
          </button>
        </div>
      </div>

      {result && (
        <div className={`mt-2.5 p-2.5 rounded text-xs border ${
          result.success ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-rose-50 border-rose-200 text-rose-900'
        }`}>
          <div className="font-semibold flex items-center space-x-1.5">
            {result.success ? <CheckCircle className="w-3.5 h-3.5 text-emerald-600" /> : <AlertCircle className="w-3.5 h-3.5 text-rose-600" />}
            <span>{result.scenarioName}: {result.success ? 'Verified Successfully' : 'Verification Issue'}</span>
          </div>
          {result.verificationNotes && result.verificationNotes.length > 0 && (
            <ul className="mt-1 list-disc list-inside space-y-0.5 text-[11px] opacity-90 font-mono">
              {result.verificationNotes.map((n, idx) => <li key={idx}>{n}</li>)}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
