import axios from 'axios';
import { tokenStorage } from '../utils/tokenStorage';
import { API_URLS } from '../config/apiConfig';

const AUTH_BASE_URL = API_URLS.auth;

export const authApi = axios.create({
    baseURL: AUTH_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Attaches the JWT to authenticated auth-service calls (e.g. /suppliers/invite). Harmless
// for the public calls below (login/register/refresh/logout/validate) — no token exists yet
// at that point, so the header is simply omitted.
authApi.interceptors.request.use(
    (config) => {
        const token = tokenStorage.getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

//register user
export const registerUser = async (userData: {
    username: string,
    email:string,
    password:string,
    roles?:string[],
}) => {
    const response = await authApi.post('/register', userData);
    return response.data;
};

//login user
export const loginUser = async (credentials:{
    username:string,
    password:string
}) => {
    const response = await authApi.post('/login', credentials);
    const { token, refreshToken, username, email, roles } = response.data;
    
    // Store both tokens
    tokenStorage.setTokens(token, refreshToken);
    tokenStorage.setUser({ username, email, roles });
    
    return response.data;
};

//refresh token
export const refreshAccessToken = async (refreshToken: string) => {
    const response = await authApi.post('/refresh-token', { refreshToken });
    const { token, refreshToken: newRefreshToken } = response.data;
    
    // Update both tokens
    tokenStorage.setAccessToken(token);
    tokenStorage.setRefreshToken(newRefreshToken);
    
    return response.data;
};

//logout user
export const logoutUser = async (refreshToken: string) => {
    try {
        await authApi.post('/logout', { refreshToken });
    } catch (error) {
        console.error('Logout failed:', error);
    } finally {
        // Clear tokens regardless of API response
        tokenStorage.clear();
    }
};

//validate token
export const validateToken = async (token: string) => {
    const response = await authApi.post('/validate', { token });
    return response.data;
};

//request a password reset email
export const forgotPassword = async (email: string) => {
    const response = await authApi.post('/forgot-password', { email });
    return response.data;
};

//reset password using the token from the emailed link
export const resetPassword = async (token: string, newPassword: string) => {
    const response = await authApi.post('/reset-password', { token, newPassword });
    return response.data;
};