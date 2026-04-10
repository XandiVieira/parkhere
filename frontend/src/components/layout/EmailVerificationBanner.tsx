"use client";

import { useState } from "react";
import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/lib/api";
import { t } from "@/lib/i18n";

export default function EmailVerificationBanner() {
  const { isAuthenticated, user } = useAuthStore();
  const [resent, setResent] = useState(false);
  const [sending, setSending] = useState(false);

  if (!isAuthenticated || !user || user.emailVerified) return null;

  const handleResend = async () => {
    setSending(true);
    try {
      await authApi.resendVerification(user.email);
      setResent(true);
    } catch {
      // silent
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="border-b border-yellow-300 bg-yellow-50 px-4 py-2 text-center text-sm text-yellow-800">
      <span>{t("verify.pending")} — {t("verify.sent")} {user.email}. </span>
      {resent ? (
        <span className="font-medium text-green-700">{t("verify.resend")} ✓</span>
      ) : (
        <button onClick={handleResend} disabled={sending} className="font-medium text-yellow-900 underline hover:text-yellow-700 disabled:opacity-50">
          {t("verify.resend")}
        </button>
      )}
    </div>
  );
}
