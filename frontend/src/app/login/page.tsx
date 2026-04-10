"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import dynamic from "next/dynamic";
const GoogleLoginButton = dynamic(() => import("@/components/auth/GoogleLoginButton"), { ssr: false });
import { authApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import { extractApiError } from "@/lib/utils";
import axios from "axios";

export default function LoginPage() {
  const router = useRouter();
  const { login, logout } = useAuthStore();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // Clear any stale auth state when visiting the login page
  useEffect(() => { logout(); }, [logout]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await authApi.login(email, password);
      login(res.data.token, res.data.user);
      router.push("/");
    } catch (err: unknown) {
      setError(extractApiError(err) || t("auth.invalidCredentials"));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    if (!credentialResponse.credential) return;
    setError("");
    setLoading(true);
    try {
      const res = await axios.post(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1"}/auth/google`,
        { credential: credentialResponse.credential }
      );
      login(res.data.token, res.data.user);
      router.push("/");
    } catch {
      setError(t("auth.googleError"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-center text-2xl font-bold text-gray-900 dark:text-white">{t("auth.login")}</h1>

        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
        )}

        {/* Google Sign-In */}
        <div className="mb-4 flex justify-center">
          <GoogleLoginButton
            onSuccess={(credential) => handleGoogleSuccess({ credential })}
            onError={() => setError(t("auth.googleError"))}
          />
        </div>

        <div className="mb-4 flex items-center gap-3">
          <div className="h-px flex-1 bg-gray-200" />
          <span className="text-xs text-gray-400">{t("common.or")}</span>
          <div className="h-px flex-1 bg-gray-200" />
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700">{t("auth.email")}</label>
            <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400"
              placeholder={t("auth.emailPlaceholder")} />
          </div>
          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-gray-700">{t("auth.password")}</label>
            <input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
              disabled={loading}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:text-gray-400"
              placeholder="********" />
          </div>
          <button type="submit" disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded-md bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
            {loading && (
              <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
            )}
            {loading ? t("auth.logging") : t("auth.login")}
          </button>
        </form>

        <div className="mt-4 text-center text-sm text-gray-600">
          <Link href="/forgot-password" className="text-blue-600 hover:underline">{t("auth.forgotPassword")}</Link>
        </div>
        <div className="mt-2 text-center text-sm text-gray-600">
          {t("auth.noAccount")}{" "}
          <Link href="/register" className="font-medium text-blue-600 hover:underline">{t("auth.registerHere")}</Link>
        </div>
      </div>
    </div>
  );
}
