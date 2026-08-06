const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';
const USER_KEY = 'auth_user';

const SUPPLIER_TOKEN_KEY = 'supplier_auth_token';
const SUPPLIER_REFRESH_TOKEN_KEY = 'supplier_auth_refresh_token';
const SUPPLIER_USER_KEY = 'supplier_auth_user';

// The staff portal and the supplier portal are two different identities that can be
// logged into simultaneously in different tabs of the same browser. localStorage is
// shared across every tab of the same origin (not per-tab), so if both used the same
// keys, logging into one would silently overwrite/clear the other's session. Picking
// the key set from the current URL (everything under /supplier/* vs everything else)
// keeps them fully isolated without needing a second AuthContext or a subdomain split.
// Exact path-segment match, not a plain prefix check — "/suppliers" (the staff
// vendor-management page) starts with the string "/supplier" too, which used to
// misclassify it as supplier-portal context and silently read the wrong (empty)
// token key, sending unauthenticated requests that the backend correctly 403'd.
const isSupplierContext = (): boolean =>
  window.location.pathname === '/supplier' || window.location.pathname.startsWith('/supplier/');

export const tokenStorage = {

   // Save both access token and refresh token
  setTokens: (accessToken: string, refreshToken: string) => {
    if (isSupplierContext()) {
      localStorage.setItem(SUPPLIER_TOKEN_KEY, accessToken);
      localStorage.setItem(SUPPLIER_REFRESH_TOKEN_KEY, refreshToken);
    } else {
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
  },

  // Get the access token
  getAccessToken: (): string | null => {
    return localStorage.getItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY);
  },

  // Get the refresh token
  getRefreshToken: (): string | null => {
    return localStorage.getItem(isSupplierContext() ? SUPPLIER_REFRESH_TOKEN_KEY : REFRESH_TOKEN_KEY);
  },

  // Update only the access token (used when refreshing)
  setAccessToken: (token: string) => {
    localStorage.setItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY, token);
  },

  // Update only the refresh token
  setRefreshToken: (token: string) => {
    localStorage.setItem(isSupplierContext() ? SUPPLIER_REFRESH_TOKEN_KEY : REFRESH_TOKEN_KEY, token);
  },

  //Save user info
  setUser: (user: any) => {
    localStorage.setItem(isSupplierContext() ? SUPPLIER_USER_KEY : USER_KEY, JSON.stringify(user));
  },

 // Get stored user info
  getUser: (): any | null => {
    const user = localStorage.getItem(isSupplierContext() ? SUPPLIER_USER_KEY : USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  //Clear all auth data (only the current context's — staff logout never touches a
  //supplier session in another tab, and vice versa)
  clear: () => {
    if (isSupplierContext()) {
      localStorage.removeItem(SUPPLIER_TOKEN_KEY);
      localStorage.removeItem(SUPPLIER_REFRESH_TOKEN_KEY);
      localStorage.removeItem(SUPPLIER_USER_KEY);
    } else {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  },

  //Check if user is authenticated
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY);
  },
};
