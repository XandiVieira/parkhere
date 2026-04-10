"use client";

import Link from "next/link";
import { t } from "@/lib/i18n";

export default function SpotError({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-orange-100">
          <svg className="h-8 w-8 text-orange-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01" />
          </svg>
        </div>
        <h1 className="mb-2 text-xl font-bold text-gray-900">{t("common.error")}</h1>
        <p className="mb-6 text-sm text-gray-600">{t("common.errorMessage")}</p>
        <div className="flex justify-center gap-3">
          <button
            onClick={reset}
            className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            {t("common.retry")}
          </button>
          <Link
            href="/"
            className="rounded-md bg-gray-100 px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
          >
            {t("nav.map")}
          </Link>
        </div>
      </div>
    </div>
  );
}
