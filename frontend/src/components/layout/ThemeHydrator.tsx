"use client";

import { useEffect } from "react";
import { useThemeStore } from "@/stores/theme";

export default function ThemeHydrator() {
  const hydrate = useThemeStore((s) => s.hydrate);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return null;
}
