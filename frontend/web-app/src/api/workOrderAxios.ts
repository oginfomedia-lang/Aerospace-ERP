// api/workOrderAxios.ts
// Points to work-order-service on port 8084
// Mirrors the pattern of notificationAxios.ts and qaAxios.ts

import axios from 'axios';
import toast from 'react-hot-toast';

const workOrderApi = axios.create({
  baseURL: 'http://localhost:8084/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

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