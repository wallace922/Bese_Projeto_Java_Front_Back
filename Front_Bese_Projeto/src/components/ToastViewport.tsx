import { useToast, type ToastKind } from '../contexts/ToastContext';

const styles: Record<ToastKind, { wrap: string; icon: string }> = {
  success: { wrap: 'bg-emerald-950/95 border-emerald-700 text-emerald-200', icon: '✔' },
  error:   { wrap: 'bg-red-950/95 border-red-700 text-red-200',             icon: '✕' },
  info:    { wrap: 'bg-sky-950/95 border-sky-700 text-sky-200',             icon: 'ℹ' },
};

export default function ToastViewport() {
  const { toasts, dismiss } = useToast();
  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 w-[calc(100%-2rem)] max-w-sm" role="status" aria-live="polite">
      {toasts.map(t => {
        const s = styles[t.kind];
        return (
          <div
            key={t.id}
            className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-sm shadow-2xl backdrop-blur animate-fadeIn ${s.wrap}`}
          >
            <span className="mt-0.5 shrink-0 font-bold" aria-hidden>{s.icon}</span>
            <span className="flex-1 whitespace-pre-wrap">{t.message}</span>
            <button
              onClick={() => dismiss(t.id)}
              className="shrink-0 opacity-60 hover:opacity-100 transition-opacity font-bold"
              aria-label="Fechar notificação"
            >
              ✕
            </button>
          </div>
        );
      })}
    </div>
  );
}
