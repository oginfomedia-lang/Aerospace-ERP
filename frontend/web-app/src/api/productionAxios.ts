import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';
import { API_URLS } from '../config/apiConfig';

const productionApi = axios.create({
  baseURL: API_URLS.production,
  headers: { 'Content-Type': 'application/json' },
});

// ✅ Request interceptor – attach JWT token
productionApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
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
