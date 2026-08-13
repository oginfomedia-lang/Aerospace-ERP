import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';

const salesApi = axios.create({
  baseURL: 'http://localhost:8091/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

salesApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

salesApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.data ||
      'Sales service error. Check if sales-service is running on port 8091.';
    if (error.response?.status !== 401) toast.error(message);
    return Promise.reject(error);
  }
);

export default salesApi;
