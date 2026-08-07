import axios from 'axios';
import toast from 'react-hot-toast';
import { tokenStorage } from '../utils/tokenStorage';

const productionResourceApi = axios.create({
  baseURL: 'http://localhost:8090/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

productionResourceApi.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

productionResourceApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.response?.data?.data ||
      'Production Resource service error. Check if production-resource-service is running on port 8090.';
    if (error.response?.status !== 401) toast.error(message);
    return Promise.reject(error);
  }
);

export default productionResourceApi;