const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';
const USER_KEY = 'auth_user';

export const tokenStorage = {
  
   // Save both access token and refresh token
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  // Get the access token
  getAccessToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY);
  },

  // Get the refresh token
  getRefreshToken: (): string | null => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  // Update only the access token (used when refreshing)
  setAccessToken: (token: string) => {
    localStorage.setItem(TOKEN_KEY, token);
  },

  // Update only the refresh token
  setRefreshToken: (token: string) => {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
  },

  //Save user info
  setUser: (user: any) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

 // Get stored user info
  getUser: (): any | null => {
    const user = localStorage.getItem(USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  //Clear all auth data
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  //Check if user is authenticated
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem(TOKEN_KEY);
  },
};
