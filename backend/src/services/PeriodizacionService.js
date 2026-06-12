/**
 * PeriodizacionService — mesociclo 3:1 (Israetel / Helms / Schoenfeld).
 *
 * Semana del ciclo:
 *   1 → MEV   (Mínimo Volumen Efectivo): base, RPE 6, vol×1.00
 *   2 → MAV   (Volumen Adaptativo Máximo): RPE 7, vol×1.10
 *   3 → MRV   (Volumen Máximo Recuperable): RPE 8, vol×1.20, intensidad alta
 *   4 → DELOAD: vol×0.60, intensidad −10%, RPE 5
 *
 * Cómo se decide qué semana toca:
 *   - Punto de partida del usuario: `plan_variacion_seed` (cada vez que
 *     pulsa "regenerar" se desplaza una semana, lo que rompe el bucle).
 *   - Semana ISO del año + seed → módulo 4 → 1..4.
 *
 * Es la pieza que faltaba para que la app deje de proponer "misma sesión
 * todas las semanas". Ahora hay progresión + descarga, como en Caliber,
 * Future o cualquier preparador profesional.
 */

const FASES = {
  1: { fase: 'MEV',    vol: 1.00, int: 1.00, rpe_base: 6, deload: false, label: 'Base — acumulación' },
  2: { fase: 'MAV',    vol: 1.10, int: 1.05, rpe_base: 7, deload: false, label: 'Carga — sobrecarga' },
  3: { fase: 'MRV',    vol: 1.20, int: 1.10, rpe_base: 8, deload: false, label: 'Peak — pico' },
  4: { fase: 'DELOAD', vol: 0.60, int: 0.90, rpe_base: 5, deload: true,  label: 'Deload — descarga' }
};

function semanaIso(fecha = new Date()) {
  const d = new Date(Date.UTC(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()));
  const dayNum = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
}

function semanaDelMesociclo(seed = 0, fecha = new Date()) {
  const iso = semanaIso(fecha);
  const idx = (((iso + Number(seed || 0)) - 1) % 4) + 1;
  return idx;
}

function plantillaSemana(weekIdx) {
  const i = Math.max(1, Math.min(4, Number(weekIdx) || 1));
  return { idx: i, ...FASES[i] };
}

/**
 * Ajusta el tramo de series/reps/RPE según la semana del mesociclo.
 * Idempotente: weekIdx=1 deja la prescripción intacta.
 */
function ajustarPorSemana(rx, weekIdx) {
  const w = plantillaSemana(weekIdx);
  if (!rx) return rx;
  const series = Number(rx.series) || 0;
  const seriesAjustadas = w.deload
    ? Math.max(2, Math.floor(series * w.vol))
    : Math.max(series, Math.round(series * (w.vol - 1) + series));
  const rpe = rx.rpe_objetivo != null
    ? Math.max(3, Math.min(10, Math.round(rx.rpe_objetivo * w.int)))
    : w.rpe_base;
  return {
    ...rx,
    series: seriesAjustadas,
    rpe_objetivo: rpe,
    fase_mesociclo: w.fase,
    fase_label: w.label,
    deload: w.deload
  };
}

module.exports = {
  FASES,
  semanaIso,
  semanaDelMesociclo,
  plantillaSemana,
  ajustarPorSemana
};
