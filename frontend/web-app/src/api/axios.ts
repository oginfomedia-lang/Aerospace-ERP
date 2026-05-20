import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8081/api/v1',
    headers: {
        'Content-Type': 'application/json',
    }
});

//add request interceptor to attach token
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('auth_token');
    if(token){
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
},(error) => {
    return Promise.reject(error);
})

export default api;