import React from 'react';

export default function TemperatureTimeline({ events = [], missingIntervals = [], profile }) {
  const chartEvents = [...(events || [])]
    .filter(event => event.dataQuality !== 'DUPLICATE' && Number.isFinite(Number(event.temperatureC)) && event.eventTime)
    .sort((a, b) => new Date(a.eventTime) - new Date(b.eventTime));
  if (chartEvents.length === 0 && missingIntervals.length === 0) {
    return <div className="empty-inline">No temperature readings have been recorded for this shipment.</div>;
  }

  const targetMin = Number(profile?.targetMinC);
  const targetMax = Number(profile?.targetMaxC);
  const minorMax = Number(profile?.minorMaxC);
  const criticalMax = Number(profile?.criticalMaxC);
  if (![targetMin, targetMax, minorMax, criticalMax].every(Number.isFinite)) {
    return <div className="alert-card error">The shipment storage range is unavailable, so the temperature chart cannot be drawn.</div>;
  }

  const timestamps = [
    ...chartEvents.map(event => new Date(event.eventTime).getTime()),
    ...missingIntervals.flatMap(interval => [new Date(interval.gapStartTime).getTime(), new Date(interval.gapEndTime).getTime()]),
  ].filter(Number.isFinite);
  const timeStart = Math.min(...timestamps);
  const timeEnd = Math.max(...timestamps);
  const timeRange = timeEnd - timeStart || 1;
  const temps = chartEvents.map(event => Number(event.temperatureC));
  const minTemp = Math.min(targetMin - 5, ...temps);
  const maxTemp = Math.max(criticalMax + 5, ...temps);
  const tempRange = maxTemp - minTemp || 1;
  const width = 760;
  const height = 220;
  const padding = { top: 25, right: 35, bottom: 35, left: 45 };
  const innerWidth = width - padding.left - padding.right;
  const innerHeight = height - padding.top - padding.bottom;
  const getX = time => timeEnd === timeStart ? padding.left + innerWidth / 2 : padding.left + ((time - timeStart) / timeRange) * innerWidth;
  const getY = temperature => padding.top + innerHeight - ((temperature - minTemp) / tempRange) * innerHeight;
  const targetYTop = getY(targetMax);
  const targetYBottom = getY(targetMin);
  const lines = chartEvents.slice(1).flatMap((event, index) => {
    const prior = chartEvents[index];
    const priorTime = new Date(prior.eventTime).getTime();
    const currentTime = new Date(event.eventTime).getTime();
    const crossesGap = missingIntervals.some(interval => new Date(interval.gapStartTime).getTime() < currentTime && new Date(interval.gapEndTime).getTime() > priorTime);
    if (crossesGap) return [];
    const outside = [Number(prior.temperatureC), Number(event.temperatureC)].some(value => value < targetMin || value > targetMax);
    return [{ points: `${getX(priorTime)},${getY(Number(prior.temperatureC))} ${getX(currentTime)},${getY(Number(event.temperatureC))}`, outside, duration: Math.max(0, Math.round((currentTime - priorTime) / 60000)), x: (getX(priorTime) + getX(currentTime)) / 2, y: (getY(Number(prior.temperatureC)) + getY(Number(event.temperatureC))) / 2 }];
  });

  return <div className="temperature-timeline">
    <div className="timeline-heading"><strong>Temperature timeline</strong><span>{chartEvents.length} readings</span></div>
    <div className="timeline-legend">
      <span>Target range: {targetMin}°C–{targetMax}°C</span><span>Minor limit: {minorMax}°C</span>
      <span>Critical limit: {criticalMax}°C</span>{missingIntervals.length > 0 && <span>Dashed box: no readings</span>}
    </div>
    <div className="overflow-x-auto">
      <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-auto max-h-64" role="img" aria-label="Temperature readings over time">
        <rect x={padding.left} y={targetYTop} width={innerWidth} height={Math.max(0, targetYBottom - targetYTop)} fill="#FFFFFF" />
        {missingIntervals.map((interval, index) => {
          const start = Math.max(timeStart, new Date(interval.gapStartTime).getTime());
          const end = Math.min(timeEnd, new Date(interval.gapEndTime).getTime());
          if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return null;
          return <g key={interval.intervalId || index}>
            <rect x={getX(start)} y={padding.top} width={Math.max(1, getX(end) - getX(start))} height={innerHeight} fill="#FFFFFF" stroke="var(--sev-critical)" strokeDasharray="4,3" />
            <text x={(getX(start) + getX(end)) / 2} y={padding.top + 13} textAnchor="middle" fill="var(--sev-critical)" style={{ fontSize: 9 }}>No readings</text>
            <title>{`Unknown temperature interval: ${interval.gapStartTime} to ${interval.gapEndTime}`}</title>
          </g>;
        })}
        {[targetMin, targetMax, minorMax, criticalMax].map((value, index) => <g key={index}>
          <line x1={padding.left} y1={getY(value)} x2={padding.left + innerWidth} y2={getY(value)} stroke={value >= criticalMax ? 'var(--sev-critical)' : value >= minorMax ? 'var(--sev-major)' : 'var(--border)'} strokeDasharray={index > 1 ? '4,4' : undefined} />
          <text x={padding.left - 8} y={getY(value) + 3} textAnchor="end" className="mono" style={{ fontSize: 10, fill: 'var(--text-muted)' }}>{value}°C</text>
        </g>)}
        <line x1={padding.left} y1={padding.top} x2={padding.left} y2={padding.top + innerHeight} stroke="var(--border)" />
        <line x1={padding.left + innerWidth} y1={padding.top} x2={padding.left + innerWidth} y2={padding.top + innerHeight} stroke="var(--border)" />
        {lines.map((line, index) => <g key={index}><polyline fill="none" stroke={line.outside ? 'var(--sev-critical)' : 'var(--text-muted)'} strokeWidth="1.2" points={line.points} />{line.outside && <text x={line.x} y={line.y - 8} textAnchor="middle" fill="var(--sev-critical)" style={{ fontSize: 9 }}>{line.duration} min</text>}</g>)}
        {chartEvents.map((event, index) => {
          const value = Number(event.temperatureC);
          const x = getX(new Date(event.eventTime).getTime());
          const y = getY(value);
          const outside = value < targetMin || value > targetMax;
          return <g key={event.eventId || index}>
            <rect x={x - 3} y={y - 3} width="6" height="6" fill={outside ? 'var(--sev-critical)' : 'var(--text)'} />
            <text x={x} y={y - 7} textAnchor="middle" fill={outside ? 'var(--sev-critical)' : 'var(--text-muted)'} style={{ fontSize: 9 }}>{value}°</text>
            <title>{`Reading: ${value}°C\nCondition: ${event.condition || 'UNKNOWN'}\nSource: ${event.source || 'SENSOR'}\nTime: ${event.eventTime}`}</title>
          </g>;
        })}
        <text x={padding.left} y={height - 10} className="mono" style={{ fontSize: 10, fill: 'var(--text-muted)' }}>{new Date(timeStart).toLocaleString()}</text>
        <text x={padding.left + innerWidth} y={height - 10} textAnchor="end" className="mono" style={{ fontSize: 10, fill: 'var(--text-muted)' }}>{new Date(timeEnd).toLocaleString()}</text>
      </svg>
    </div>
  </div>;
}
