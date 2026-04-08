import type { TrustLevel } from "@/types/api";
import { trustLevelColor } from "@/lib/utils";
import { t } from "@/lib/i18n";

interface TrustBadgeProps {
  level: TrustLevel;
  className?: string;
}

export default function TrustBadge({ level, className = "" }: TrustBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold text-white ${trustLevelColor(level)} ${className}`}
    >
      {t(`trust.${level}` as any)}
    </span>
  );
}
