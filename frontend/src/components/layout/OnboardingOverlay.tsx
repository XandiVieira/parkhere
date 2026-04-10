"use client";

import { useState, useEffect } from "react";
import { t } from "@/lib/i18n";

const ONBOARDING_KEY = "parkhere-onboarding-done";

const STEPS = [
  {
    icon: "📍",
    titleKey: "onboarding.step1Title" as const,
    textKey: "onboarding.step1" as const,
  },
  {
    icon: "🛡",
    titleKey: "onboarding.step2Title" as const,
    textKey: "onboarding.step2" as const,
  },
  {
    icon: "🏆",
    titleKey: "onboarding.step3Title" as const,
    textKey: "onboarding.step3" as const,
  },
];

export default function OnboardingOverlay() {
  const [visible, setVisible] = useState(false);
  const [step, setStep] = useState(0);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!localStorage.getItem(ONBOARDING_KEY)) {
      setVisible(true);
    }
  }, []);

  const dismiss = () => {
    localStorage.setItem(ONBOARDING_KEY, "true");
    setVisible(false);
  };

  if (!visible) return null;

  const current = STEPS[step];
  const isLast = step === STEPS.length - 1;

  return (
    <div className="fixed inset-0 z-[3000] flex items-center justify-center bg-black/50 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white px-6 py-8 text-center shadow-xl">
        {step === 0 && (
          <h2 className="mb-4 text-xl font-bold text-gray-900">{t("onboarding.welcome")}</h2>
        )}

        <div className="mb-2 text-4xl">{current.icon}</div>
        <h3 className="mb-2 text-lg font-semibold text-gray-900">{t(current.titleKey as any)}</h3>
        <p className="mb-6 text-sm text-gray-600">{t(current.textKey as any)}</p>

        {/* Progress dots */}
        <div className="mb-5 flex justify-center gap-2">
          {STEPS.map((_, i) => (
            <div
              key={i}
              className={`h-2 w-2 rounded-full transition ${i === step ? "bg-blue-600" : "bg-gray-200"}`}
            />
          ))}
        </div>

        <div className="flex gap-3">
          <button onClick={dismiss} className="flex-1 rounded-lg bg-gray-100 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-200">
            {t("onboarding.skip")}
          </button>
          <button
            onClick={() => (isLast ? dismiss() : setStep(step + 1))}
            className="flex-1 rounded-lg bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
          >
            {isLast ? t("onboarding.start") : t("onboarding.next")}
          </button>
        </div>
      </div>
    </div>
  );
}
