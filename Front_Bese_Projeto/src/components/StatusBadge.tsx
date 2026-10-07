export type PaymentStatus = 'CANCELADA' | 'PAGA' | 'A_PAGAR';

const styles: Record<PaymentStatus, string> = {
  CANCELADA: 'bg-red-900/60 text-red-300 border-red-700',
  PAGA:      'bg-emerald-900/60 text-emerald-300 border-emerald-700',
  A_PAGAR:   'bg-amber-900/60 text-amber-300 border-amber-600',
};

const labels: Record<PaymentStatus, string> = {
  CANCELADA: 'Cancelada',
  PAGA: 'Paga',
  A_PAGAR: 'A Pagar',
};

export default function StatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-bold border uppercase tracking-wider ${styles[status]}`}>
      {labels[status]}
    </span>
  );
}
