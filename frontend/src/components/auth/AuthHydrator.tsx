"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/auth";

export default function AuthHydrator() {
  const hydrate = useAuthStore((s) => s.hydrate);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return null;
}
