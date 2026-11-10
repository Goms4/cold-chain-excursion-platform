// Lightweight fetch client for Cold-Chain Platform REST APIs

const getAuthToken = () => {
  return localStorage.getItem('cold_chain_token') || '';
};

export const setAuthToken = (token) => {
  if (token) {
    localStorage.setItem('cold_chain_token', token);
  } else {
    localStorage.removeItem('cold_chain_token');
  }
};

export const getActiveRole = () => {
  return localStorage.getItem('cold_chain_role') || 'OPERATIONS';
};

export const setActiveRole = (role) => {
  localStorage.setItem('cold_chain_role', role);
};

export async function apiRequest(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {})
  };

  const config = {
    ...options,
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
    const error = new Error(errorData.message || 'API request failed');
    error.status = response.status;
    error.data = errorData;
    throw error;
  }

  // Handle empty responses (204 No Content)
  if (response.status === 204) {
    return null;
  }

  return response.json();
}
