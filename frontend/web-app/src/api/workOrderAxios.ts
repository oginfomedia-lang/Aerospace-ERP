// api/workOrderAxios.ts
// Points to work-order-service on port 8084
// Mirrors the pattern of notificationAxios.ts and qaAxios.ts

import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';
import { API_URLS } from '../config/apiConfig';

const workOrderApi = axios.create({
  baseURL: API_URLS.workOrder,
  headers: { 'Content-Type': 'application/json' },
});

// ✅ Request interceptor – attach JWT token
workOrderApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

workOrderApi.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Work order service unreachable.';
    toast.error(msg);
    return Promise.reject(err);
  }
);

export default workOrderApi;