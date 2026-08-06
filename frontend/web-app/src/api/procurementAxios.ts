import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';

const procurementApi = axios.create({
  baseURL: 'http://localhost:8086/api',
  headers: { 'Content-Type': 'application/json' },
});

// ✅ Request interceptor – attach JWT token
procurementApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

procurementApi.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Procurement service unreachable.';
    toast.error(msg);
    return Promise.reject(err);
  }
);

export default procurementApi;
