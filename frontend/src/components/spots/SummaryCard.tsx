/**
 * A small card displaying a label and a value.
 * Used in spot summary to show availability, price, safety, etc.
 *
 * Props:
 *   label - The title text (e.g., "Disponibilidade")
 *   value - The value text (e.g., "AVAILABLE")
 *   color - Optional Tailwind text color class (e.g., "text-green-700")
 */
interface SummaryCardProps {
  label: string;
  value: string;
  color?: string;
}

export default function SummaryCard({ label, value, color }: SummaryCardProps) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-3 text-center">
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`mt-1 text-lg font-semibold ${color || "text-gray-900"}`}>{value}</p>
    </div>
  );
}
