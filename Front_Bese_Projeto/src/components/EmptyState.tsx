interface EmptyStateProps {
  title?: string;
  hint?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export default function EmptyState({ title, hint, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-white/15 bg-black/20 px-6 py-10 text-center">
      <span className="text-3xl" aria-hidden>🔍</span>
      <p className="text-gray-200 font-bold">{title ?? 'Nenhum registro encontrado'}</p>
      {hint && <p className="text-stone-500 text-sm max-w-md">{hint}</p>}
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="mt-2 px-4 py-2 rounded-md bg-amber-500 hover:bg-amber-400 text-stone-950 text-sm font-bold uppercase tracking-widest transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}
