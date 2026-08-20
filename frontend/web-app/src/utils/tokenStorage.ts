const TOKEN_KEY = 'auth_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';
const USER_KEY = 'auth_user';

const SUPPLIER_TOKEN_KEY = 'supplier_auth_token';
const SUPPLIER_REFRESH_TOKEN_KEY = 'supplier_auth_refresh_token';
const SUPPLIER_USER_KEY = 'supplier_auth_user';

// The staff portal and the supplier portal are two different identities that can be
// logged into simultaneously in different tabs of the same browser. sessionStorage is
// scoped per tab (unlike localStorage, which is shared across every tab of the same
// origin), so this split matters less for cross-tab isolation than it used to — but it's
// kept so the staff and supplier identities never collide within the SAME tab either
// (e.g. a staff user opening a supplier-portal link in the same tab).
// Exact path-segment match, not a plain prefix check — "/suppliers" (the staff
// vendor-management page) starts with the string "/supplier" too, which used to
// misclassify it as supplier-portal context and silently read the wrong (empty)
// token key, sending unauthenticated requests that the backend correctly 403'd.
const isSupplierContext = (): boolean =>
  window.location.pathname === '/supplier' || window.location.pathname.startsWith('/supplier/');

// sessionStorage (not localStorage): each browser tab gets its own independent login
// that ends when that tab (or the whole browser) closes, and logging into a different
// account in a new tab no longer disturbs any other open tab — sessionStorage is never
// shared across tabs, only inherited by a tab that duplicates an existing one.
export const tokenStorage = {

   // Save both access token and refresh token
  setTokens: (accessToken: string, refreshToken: string) => {
    if (isSupplierContext()) {
      sessionStorage.setItem(SUPPLIER_TOKEN_KEY, accessToken);
      sessionStorage.setItem(SUPPLIER_REFRESH_TOKEN_KEY, refreshToken);
    } else {
      sessionStorage.setItem(TOKEN_KEY, accessToken);
      sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
  },

  // Get the access token
  getAccessToken: (): string | null => {
    return sessionStorage.getItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY);
  },

  // Get the refresh token
  getRefreshToken: (): string | null => {
    return sessionStorage.getItem(isSupplierContext() ? SUPPLIER_REFRESH_TOKEN_KEY : REFRESH_TOKEN_KEY);
  },

  // Update only the access token (used when refreshing)
  setAccessToken: (token: string) => {
    sessionStorage.setItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY, token);
  },

  // Update only the refresh token
  setRefreshToken: (token: string) => {
    sessionStorage.setItem(isSupplierContext() ? SUPPLIER_REFRESH_TOKEN_KEY : REFRESH_TOKEN_KEY, token);
  },

  //Save user info
  setUser: (user: any) => {
    sessionStorage.setItem(isSupplierContext() ? SUPPLIER_USER_KEY : USER_KEY, JSON.stringify(user));
  },

 // Get stored user info
  getUser: (): any | null => {
    const user = sessionStorage.getItem(isSupplierContext() ? SUPPLIER_USER_KEY : USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  //Clear all auth data (only the current context's — staff logout never touches a
  //supplier session in another tab, and vice versa)
  clear: () => {
    if (isSupplierContext()) {
      sessionStorage.removeItem(SUPPLIER_TOKEN_KEY);
      sessionStorage.removeItem(SUPPLIER_REFRESH_TOKEN_KEY);
      sessionStorage.removeItem(SUPPLIER_USER_KEY);
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
    }
  },

  //Check if user is authenticated
  isAuthenticated: (): boolean => {
    return !!sessionStorage.getItem(isSupplierContext() ? SUPPLIER_TOKEN_KEY : TOKEN_KEY);
  },
};
