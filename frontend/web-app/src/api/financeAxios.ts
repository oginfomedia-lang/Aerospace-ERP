import axios from 'axios';
import { tokenStorage } from '../utils/tokenStorage';

const financeApi = axios.create({
  baseURL: 'http://localhost:8092/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

financeApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

// No response-side toast here on purpose: every call site already shows its own
// contextual message (toast.promise on writes, .catch(() => toast.error(...)) on
// reads). A second generic toast here just duplicated whatever the call site showed.
financeApi.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);

export default financeApi;
