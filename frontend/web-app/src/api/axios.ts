import axios, { AxiosError } from 'axios';
import { tokenStorage } from '../utils/tokenStorage';
import { refreshAccessToken } from './authAxios';

const api = axios.create({
    baseURL: 'http://localhost:8081/api/v1',
    headers: {
        'Content-Type': 'application/json',
    }
});

// Flag to prevent infinite refresh loop
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

//add request interceptor to attach token
api.interceptors.request.use((config) => {
    const token = tokenStorage.getAccessToken();
    if(token){
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
},(error) => {
    return Promise.reject(error);
});

//add response interceptor to handle 403 and auto-refresh
api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as any;

        // If error is 403 and we haven't already tried to refresh
        if (error.response?.status === 403 && !originalRequest._retry) {
            if (isRefreshing) {
                // Queue the request while refresh is in progress
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then(token => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return api(originalRequest);
                    })
                    .catch(err => Promise.reject(err));
            }

            isRefreshing = true;
            originalRequest._retry = true;

            try {
                const refreshToken = tokenStorage.getRefreshToken();
                
                if (!refreshToken) {
                    // No refresh token, clear auth and redirect to login — the right one for
                    // whichever portal this tab belongs to (see tokenStorage.ts).
                    tokenStorage.clear();
                    window.location.href = (window.location.pathname === '/supplier' || window.location.pathname.startsWith('/supplier/')) ? '/supplier/login' : '/login';
                    return Promise.reject(error);
                }

                // Call refresh-token endpoint
                const response = await refreshAccessToken(refreshToken);
                const { token } = response;

                // Retry original request with new token
                originalRequest.headers.Authorization = `Bearer ${token}`;
                processQueue(null, token);
                isRefreshing = false;

                return api(originalRequest);
            } catch (err) {
                // Refresh failed, clear auth and redirect to login
                tokenStorage.clear();
                processQueue(err, null);
                isRefreshing = false;
                window.location.href = (window.location.pathname === '/supplier' || window.location.pathname.startsWith('/supplier/')) ? '/supplier/login' : '/login';
                return Promise.reject(err);
            }
        }

        return Promise.reject(error);
    }
);

export default api;