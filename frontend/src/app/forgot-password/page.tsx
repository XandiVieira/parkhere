"use client";

import { useState } from "react";
import Link from "next/link";
import { authApi } from "@/lib/api";
import { t } from "@/lib/i18n";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authApi.forgotPassword(email);
    } catch {
      // Ignore errors to prevent email enumeration
    } finally {
      setLoading(false);
      setSubmitted(true);
    }
  };

  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <h1 className="mb-2 text-center text-2xl font-bold text-gray-900">{t("forgot.title")}</h1>
        <p className="mb-6 text-center text-sm text-gray-500">
          {t("forgot.description")}
        </p>

        {submitted ? (
          <div className="rounded-md bg-green-50 p-4 text-center">
            <p className="text-sm font-medium text-green-700">
              {t("forgot.successMessage")}
            </p>
            <Link href="/login" className="mt-3 inline-block text-sm font-medium text-blue-600 hover:underline">
              {t("forgot.backToLogin")}
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
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

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-md bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? t("forgot.sending") : t("forgot.sendLink")}
            </button>

            <div className="text-center text-sm text-gray-600">
              <Link href="/login" className="text-blue-600 hover:underline">
                {t("forgot.backToLogin")}
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
