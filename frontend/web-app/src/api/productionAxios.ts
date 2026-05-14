import axios from 'axios';
import toast from 'react-hot-toast';

const productionApi = axios.create({
  baseURL: 'http://localhost:8085/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

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
