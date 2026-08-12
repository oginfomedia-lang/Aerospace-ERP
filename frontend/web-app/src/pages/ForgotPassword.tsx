import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail } from 'lucide-react';
import { forgotPassword } from '../api/authAxios';

const ForgotPassword: React.FC = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      await forgotPassword(email);
    } finally {
      // Always show the same "check your email" outcome, whether or not the
      // account exists — matches the backend's anti-enumeration behavior.
      setSent(true);
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-xl w-full max-w-md p-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
        <div className="flex justify-center mb-6">
          <div className="bg-blue-600 p-3 rounded-2xl">
            <Mail className="h-8 w-8 text-white" />
          </div>
        </div>
        <h2 className="text-3xl font-black text-center text-gray-800 mb-2">Forgot Password</h2>

        {sent ? (
          <p className="text-center text-gray-600 mt-4">
            If an account exists for that email, we've sent a reset link. Check your inbox.
          </p>
        ) : (
          <>
            <p className="text-center text-gray-500 mb-8">Enter your email and we'll send you a reset link</p>
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-2 rounded-xl border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                  placeholder="you@example.com"
                  required
                />
              </div>
              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 rounded-xl transition-all transform hover:scale-[1.02] disabled:opacity-50"
              >
                {isLoading ? 'Sending...' : 'Send Reset Link'}
              </button>
            </form>
          </>
        )}

        <p className="text-center text-sm text-gray-600 mt-6">
          <Link to="/login" className="text-blue-600 font-bold hover:underline">
            Back to Sign In
          </Link>
        </p>
      </div>
    </div>
  );
};

export default ForgotPassword;
