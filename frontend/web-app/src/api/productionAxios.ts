import axios from 'axios';
import toast from 'react-hot-toast';

const productionApi = axios.create({
  baseURL: 'http://localhost:8085/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// ✅ Request interceptor – attach JWT token
productionApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

productionApi.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Production service unreachable.';
    toast.error(msg);
    return Promise.reject(err);
  }
);

export default productionApi;
