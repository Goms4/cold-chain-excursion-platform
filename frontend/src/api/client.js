// Lightweight fetch client for Cold-Chain Platform REST APIs

let activeRole = null;
let activeDisplayName = '';
export const getActiveRole = () => activeRole;
export const getActiveDisplayName = () => activeDisplayName;
export const setSessionIdentity = (role, displayName) => { activeRole = role; activeDisplayName = displayName || ''; };
export const clearSessionIdentity = () => { activeRole = null; activeDisplayName = ''; };
const csrfCookie = () => decodeURIComponent(document.cookie.split('; ').find(item => item.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=') || '');
const mutating = method => !['GET', 'HEAD', 'OPTIONS'].includes((method || 'GET').toUpperCase());

// Current API DTOs already expose business IDs. Keep references verbatim.
const shipmentBusinessId = id => id;
const lotBusinessId = id => id;
const mapLotIds = ids => ids;

function normalizeResponse(endpoint, data) {
  if (data == null) return data;
  endpoint = endpoint.split('?')[0];
  if (/\/api\/shipments(?:\/[^/]+)?$/.test(endpoint)) {
    const map = shipment => ({ ...shipment, shipmentId: shipment.shipmentId || shipment.id });
    return Array.isArray(data) ? data.map(map) : map(data);
  }
  if (/\/api\/shipments\/[^/]+\/lots$/.test(endpoint)) return (data || []).map(lot => ({ ...lot, lotId: lot.lotId || lotBusinessId(lot.id), shipmentId: shipmentBusinessId(lot.shipmentId) }));
  if (/\/api\/(?:excursions|shipments\/[^/]+\/excursions)(?:\/[^/]+)?$/.test(endpoint)) {
    const map = issue => ({ ...issue, shipmentId: shipmentBusinessId(issue.shipmentId), shipmentInternalId: issue.shipmentId, affectedLotInternalIds: issue.affectedLotIds, affectedLotIds: mapLotIds(issue.affectedLotIds) });
    return Array.isArray(data) ? data.map(map) : map(data);
  }
  if (/\/api\/investigations(?:\/excursion\/[^/]+|\/[^/]+)?$/.test(endpoint) && !endpoint.endsWith('/decisions')) {
    const map = investigation => ({ ...investigation, investigationId: investigation.investigationId || investigation.id, investigationInternalId: investigation.id, shipmentInternalId: investigation.shipmentId, shipmentId: shipmentBusinessId(investigation.shipmentId) });
    return data && !Array.isArray(data) ? map(data) : data;
  }
  if (/\/api\/shipments\/[^/]+\/sensor-events$/.test(endpoint)) return (data || []).map(event => ({
    ...event,
    eventId: event.eventId?.startsWith('EVT-') ? event.eventId : event.id != null ? `EVT-${String(event.id).padStart(6, '0')}` : event.eventId,
    shipmentId: shipmentBusinessId(event.shipmentId),
    lotId: lotBusinessId(event.lotId),
    dataQuality: event.dataQuality || (event.isDuplicate ? 'DUPLICATE' : event.qualityFlag === 'QUARANTINED' ? 'QUARANTINED' : ['LATE', 'OUT_OF_ORDER'].includes(event.qualityFlag) ? 'LATE' : 'VALID'),
    condition: event.condition || event.status || 'UNKNOWN',
  }));
  return data;
}

export async function apiRequest(endpoint, options = {}) {
  if (mutating(options.method)) {
    await fetch('/api/auth/csrf', { credentials: 'include' });
  }
  const headers = {
    'Content-Type': 'application/json',
    ...(mutating(options.method) && csrfCookie() ? { 'X-XSRF-TOKEN': csrfCookie() } : {}),
    ...(options.headers || {})
  };

  const config = {
    ...options,
    credentials: 'include',
    headers
  };

  const response = await fetch(endpoint, config);

  if (!response.ok) {
    let errorData;
    try {
      errorData = await response.json();
    } catch (e) {
      errorData = { message: `HTTP ${response.status}: ${response.statusText}` };
    }
    const error = new Error(errorData.message || (response.status === 403 ? "You don't have permission" : 'API request failed'));
    error.status = response.status;
    error.data = errorData;
    if (response.status === 401 && endpoint !== '/api/auth/login' && endpoint !== '/api/auth/me') {
      clearSessionIdentity();
      window.dispatchEvent(new CustomEvent('coldlatch:unauthorized'));
    }
    throw error;
  }

  // Handle empty responses (204 No Content)
  if (response.status === 204) {
    return null;
  }

  return normalizeResponse(endpoint, await response.json());
}
