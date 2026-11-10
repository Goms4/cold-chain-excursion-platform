import React from 'react';

export default function TemperatureTimeline({ events = [], missingIntervals = [], profile }) {
  if (!events || events.length === 0) {
    return (
      <div className="p-8 text-center text-slate-400 text-xs border border-dashed border-slate-200 rounded-lg">
        No temperature telemetry recorded yet for this shipment.
      </div>
    );
  }

  // Target bounds from storage profile
  const targetMin = profile?.targetMinC != null ? Number(profile.targetMinC) : 15.0;
  const targetMax = profile?.targetMaxC != null ? Number(profile.targetMaxC) : 25.0;
  const minorMax = profile?.minorMaxC != null ? Number(profile.minorMaxC) : 30.0;
  const criticalMax = profile?.criticalMaxC != null ? Number(profile.criticalMaxC) : 40.0;

  // Find min and max for chart scale
  const temps = events.map(e => Number(e.temperatureC || 20));
  const minTemp = Math.min(targetMin - 5, ...temps);
  const maxTemp = Math.max(criticalMax + 5, ...temps);
  const tempRange = maxTemp - minTemp || 1;

  const height = 220;
  const width = 760;
  const padding = { top: 25, right: 35, bottom: 35, left: 45 };

  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;

  const getY = (temp) => {
    return padding.top + innerHeight - ((temp - minTemp) / tempRange) * innerHeight;
  };

  const getX = (index) => {
    if (events.length === 1) return padding.left + innerWidth / 2;
    return padding.left + (index / (events.length - 1)) * innerWidth;
  };

  // Safe target zone rectangle coordinates
  const targetYTop = getY(targetMax);
  const targetYBottom = getY(targetMin);
  const targetHeight = Math.max(0, targetYBottom - targetYTop);

  // Line path
  const linePoints = events.map((e, idx) => `${getX(idx)},${getY(Number(e.temperatureC || 20))}`).join(' ');

  return (
    <div className="w-full bg-white p-4 border border-clinical-border rounded-lg clinical-card">
      <div className="flex items-center justify-between mb-3 text-xs">
        <div className="flex items-center space-x-3">
          <span className="font-semibold text-slate-800">Event-Time Temperature Chronology</span>
          <span className="font-mono text-slate-500">[{events.length} telemetry readings]</span>
        </div>
        <div className="flex items-center space-x-4 text-[11px] text-slate-600">
          <span className="flex items-center space-x-1">
            <span className="w-2.5 h-2.5 rounded-sm bg-emerald-100 border border-emerald-400"></span>
            <span>Target ({targetMin}°C - {targetMax}°C)</span>
          </span>
          <span className="flex items-center space-x-1">
            <span className="w-2.5 h-1 border-t-2 border-dashed border-amber-500"></span>
            <span>Minor Limit ({minorMax}°C)</span>
          </span>
          <span className="flex items-center space-x-1">
            <span className="w-2.5 h-1 border-t-2 border-dashed border-rose-500"></span>
            <span>Critical Limit ({criticalMax}°C)</span>
          </span>
          {missingIntervals && missingIntervals.length > 0 && (
            <span className="flex items-center space-x-1">
              <span className="w-2.5 h-2.5 rounded-sm bg-purple-100 border border-purple-400"></span>
              <span className="font-mono text-purple-700">Missing (UNKNOWN)</span>
            </span>
          )}
        </div>
      </div>

      <div className="overflow-x-auto">
        <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto max-h-64">
          {/* Target Compliant Range Band */}
          <rect
            x={padding.left}
            y={targetYTop}
            width={innerWidth}
            height={targetHeight}
            fill="#ECFDF5"
            stroke="#A7F3D0"
            strokeWidth="0.5"
            opacity="0.8"
          />

          {/* Grid lines and Y labels */}
          {[minTemp, targetMin, targetMax, minorMax, criticalMax].map((t, idx) => {
            const y = getY(t);
            return (
              <g key={idx}>
                <line
                  x1={padding.left}
                  y1={y}
                  x2={padding.left + innerWidth}
                  y2={y}
                  stroke={t === criticalMax ? '#FDA4AF' : t === minorMax ? '#FDE68A' : '#F1F5F9'}
                  strokeDasharray={t > targetMax ? '4,4' : 'none'}
                  strokeWidth="1"
                />
                <text
                  x={padding.left - 8}
                  y={y + 3}
                  textAnchor="end"
                  className="text-[10px] fill-slate-400 font-mono"
                >
                  {t.toFixed(0)}°C
                </text>
              </g>
            );
          })}

          {/* Data Line */}
          {events.length > 1 && (
            <polyline
              fill="none"
              stroke="#0F4C5C"
              strokeWidth="2"
              strokeLinejoin="round"
              strokeLinecap="round"
              points={linePoints}
            />
          )}

          {/* Points */}
          {events.map((e, idx) => {
            const x = getX(idx);
            const temp = Number(e.temperatureC || 20);
            const y = getY(temp);
            const isExcursion = temp < targetMin || temp > targetMax;
            const isCritical = temp >= criticalMax;
            const fill = isCritical ? '#E11D48' : isExcursion ? '#D97706' : '#059669';

            return (
              <g key={e.id || idx} className="cursor-pointer group">
                <circle
                  cx={x}
                  cy={y}
                  r="4"
                  fill={fill}
                  stroke="#FFFFFF"
                  strokeWidth="1.5"
                />
                <title>{`Event: ${e.eventId}\nTime: ${e.eventTime}\nTemp: ${temp}°C\nStatus: ${e.status || 'NORMAL'}`}</title>
              </g>
            );
          })}

          {/* X axis labels (Start, Mid, End) */}
          <text x={padding.left} y={height - 10} className="text-[10px] fill-slate-400 font-mono">
            {events[0]?.eventTime ? new Date(events[0].eventTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
          </text>
          <text x={padding.left + innerWidth} y={height - 10} textAnchor="end" className="text-[10px] fill-slate-400 font-mono">
            {events[events.length - 1]?.eventTime ? new Date(events[events.length - 1].eventTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
          </text>
        </svg>
      </div>
    </div>
  );
}
