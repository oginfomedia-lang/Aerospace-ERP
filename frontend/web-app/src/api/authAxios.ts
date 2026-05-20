import axios from 'axios';

const AUTH_BASE_URL = 'http://localhost:8087/api/auth';

export const authApi = axios.create({
    baseURL: AUTH_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

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
    return response.data;
};

//validate token
export const validateToken = async (token: string) => {
    const response = await authApi.post('/validate', { token });
    return response.data;
};