import axios from 'axios';
import toast from 'react-hot-toast';

const procurementApi = axios.create({
  baseURL: 'http://localhost:8086/api',
  headers: { 'Content-Type': 'application/json' },
});

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
