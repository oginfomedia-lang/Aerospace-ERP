import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';
import { API_URLS } from '../config/apiConfig';

// Separate axios instance for qa-service (port 8082)
// Your existing api/axios.ts points to inventory-service (port 8081)
// QA service runs on a different port so needs its own instance

const qaApi = axios.create({
  baseURL: API_URLS.qa,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ✅ Request interceptor – attach JWT token
qaApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handles errors globally
qaApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.data ||
      'QA Service error. Check if qa-service is running on port 8082.';

    if (error.response?.status !== 401) {
      toast.error(message);
    }

    return Promise.reject(error);
  }
);

export default qaApi;