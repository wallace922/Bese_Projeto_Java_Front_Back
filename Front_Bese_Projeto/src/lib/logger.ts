/**
 * Log restrito a desenvolvimento.
 *
 * Em produção (`npm run build`), estas chamadas não emitem nada — evita
 * expor pilhas de erro e dados sensíveis no DevTools de qualquer visitante.
 * Erro real de produção deve ir para monitoramento (Sentry ou similar),
 * não para o console do navegador.
 */
function emit(level: 'error' | 'warn', args: unknown[]): void {
  if (import.meta.env.DEV) {
    // eslint-disable-next-line no-console
    console[level](...args);
  }
}

export const logger = {
  error(...args: unknown[]): void {
    emit('error', args);
  },
  warn(...args: unknown[]): void {
    emit('warn', args);
  },
};
