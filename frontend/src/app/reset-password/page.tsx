"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { authApi } from "@/lib/api";
import { t } from "@/lib/i18n";

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (password.length < 8) {
      setError(t("reset.passwordMinLength"));
      return;
    }
    if (password !== confirmPassword) {
      setError(t("reset.passwordMismatch"));
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword(token, password);
      setSuccess(true);
    } catch {
      setError(t("reset.invalidToken"));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm text-center">
          <p className="text-red-600">{t("reset.invalidToken")}</p>
          <Link href="/login" className="mt-3 inline-block text-sm font-medium text-blue-600 hover:underline">
            {t("forgot.backToLogin")}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="mb-6 text-center text-2xl font-bold text-gray-900">{t("reset.title")}</h1>

        {success ? (
          <div className="rounded-md bg-green-50 p-4 text-center">
            <p className="text-sm font-medium text-green-700">{t("reset.success")}</p>
            <Link href="/login" className="mt-3 inline-block text-sm font-medium text-blue-600 hover:underline">
              {t("auth.signIn")}
            </Link>
          </div>
        ) : (
          <>
            {error && (
              <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
            )}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="password" className="mb-1 block text-sm font-medium text-gray-700">
                  {t("reset.newPassword")}
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
                  {t("reset.confirmPassword")}
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
                {loading ? t("reset.submitting") : t("reset.submit")}
              </button>
            </form>
            <div className="mt-4 text-center text-sm text-gray-600">
              <Link href="/login" className="text-blue-600 hover:underline">{t("forgot.backToLogin")}</Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="flex flex-1 items-center justify-center"><span className="text-gray-500">{t("common.loading")}</span></div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
