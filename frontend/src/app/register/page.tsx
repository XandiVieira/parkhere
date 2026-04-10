"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import dynamic from "next/dynamic";
const GoogleLoginButton = dynamic(() => import("@/components/auth/GoogleLoginButton"), { ssr: false });
import { authApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { t } from "@/lib/i18n";
import { extractApiError } from "@/lib/utils";
import axios from "axios";

export default function RegisterPage() {
  const router = useRouter();
  const login = useAuthStore((s) => s.login);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (password.length < 8) {
      setError(t("auth.passwordMinError"));
      return;
    }
    if (password !== confirmPassword) {
      setError(t("auth.passwordMismatch"));
      return;
    }

    setLoading(true);
    try {
      const res = await authApi.register(name, email, password);
      login(res.data.token, res.data.user);
      router.push("/");
    } catch (err: unknown) {
      setError(extractApiError(err) || t("auth.registerFailed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-center text-2xl font-bold text-gray-900">{t("auth.createAccount")}</h1>

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
            <label htmlFor="name" className="mb-1 block text-sm font-medium text-gray-700">
              {t("auth.name")}
            </label>
            <input
              id="name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
              placeholder={t("auth.namePlaceholder")}
            />
          </div>

          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700">
              {t("auth.email")}
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
              placeholder={t("auth.emailPlaceholder")}
            />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-gray-700">
              {t("auth.password")}
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
              placeholder={t("auth.passwordMinLength")}
            />
          </div>

          <div>
            <label htmlFor="confirmPassword" className="mb-1 block text-sm font-medium text-gray-700">
              {t("auth.confirmPassword")}
            </label>
            <input
              id="confirmPassword"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
              placeholder={t("auth.passwordRepeatPlaceholder")}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? t("auth.creating") : t("auth.createAccount")}
          </button>
        </form>

        <div className="mt-4 text-center text-sm text-gray-600">
          {t("auth.hasAccount")}{" "}
          <Link href="/login" className="font-medium text-blue-600 hover:underline">
            {t("auth.signIn")}
          </Link>
        </div>
      </div>
    </div>
  );
}
