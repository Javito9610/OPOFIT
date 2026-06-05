/**
 * LogrosService: records personales, rachas y medallas (gamificacion tipo Strava).
 *
 * Funciones PURAS (sin BD) para poder testearlas a fondo y reutilizarlas.
 */

/**
 * Mejor marca por ejercicio a partir de registros sueltos.
 * @param {Array<{idEjercicio:number, nombre?:string, valor:number, fecha?:string, menorEsMejor?:boolean}>} registros
 * @returns {Array} un record por ejercicio, ordenado por nombre.
 */
function recordsPorEjercicio(registros) {
  const arr = Array.isArray(registros) ? registros : [];
  const porEjercicio = new Map();
  for (const r of arr) {
    const id = r.idEjercicio;
    const valor = Number(r.valor);
    if (id == null || !Number.isFinite(valor)) continue;
    const menorEsMejor = Boolean(r.menorEsMejor);
    const actual = porEjercicio.get(id);
    if (!actual) {
      porEjercicio.set(id, { idEjercicio: id, nombre: r.nombre || null, valor, fecha: r.fecha || null, menorEsMejor });
      continue;
    }
    const mejora = menorEsMejor ? valor < actual.valor : valor > actual.valor;
    if (mejora) {
      porEjercicio.set(id, { idEjercicio: id, nombre: r.nombre || actual.nombre, valor, fecha: r.fecha || null, menorEsMejor });
    }
  }
  return [...porEjercicio.values()].sort((a, b) =>
    String(a.nombre || a.idEjercicio).localeCompare(String(b.nombre || b.idEjercicio))
  );
}

/** Normaliza una fecha a 'YYYY-MM-DD' (UTC) o null si invalida. */
function ymd(fecha) {
  if (!fecha) return null;
  const d = fecha instanceof Date ? fecha : new Date(fecha);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString().slice(0, 10);
}

/** Diferencia en dias entre dos 'YYYY-MM-DD'. */
function diffDias(a, b) {
  return Math.round((Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`)) / 86400000);
}

/**
 * Racha de dias de entreno (consecutivos).
 * @param {Array<string|Date>} fechas  fechas de sesiones (cualquier orden, con repetidos)
 * @param {string|Date} hoy            referencia para "racha actual" (por defecto: hoy)
 * @returns {{actual:number, maxima:number, diasActivos:number}}
 */
function calcularRachas(fechas, hoy = new Date()) {
  const dias = [...new Set((Array.isArray(fechas) ? fechas : []).map(ymd).filter(Boolean))].sort();
  if (dias.length === 0) return { actual: 0, maxima: 0, diasActivos: 0 };

  let maxima = 1;
  let run = 1;
  for (let i = 1; i < dias.length; i++) {
    if (diffDias(dias[i - 1], dias[i]) === 1) {
      run += 1;
    } else {
      run = 1;
    }
    if (run > maxima) maxima = run;
  }

  // Racha actual: cuenta hacia atras desde hoy (o ayer, si hoy aun no se entreno).
  const refHoy = ymd(hoy);
  const ultimo = dias[dias.length - 1];
  let actual = 0;
  const desde = diffDias(ultimo, refHoy);
  if (desde === 0 || desde === 1) {
    actual = 1;
    for (let i = dias.length - 2; i >= 0; i--) {
      if (diffDias(dias[i], dias[i + 1]) === 1) actual += 1;
      else break;
    }
  }
  return { actual, maxima, diasActivos: dias.length };
}

/** Catalogo de medallas. Cada una se desbloquea con un umbral sobre las stats. */
const CATALOGO_MEDALLAS = [
  { id: 'primer_entreno', nombre: 'Primer entreno', desc: 'Completa tu primera sesion', campo: 'sesiones', umbral: 1, icono: 'star' },
  { id: 'sesiones_10', nombre: 'Constante', desc: '10 sesiones registradas', campo: 'sesiones', umbral: 10, icono: 'whatshot' },
  { id: 'sesiones_50', nombre: 'Imparable', desc: '50 sesiones registradas', campo: 'sesiones', umbral: 50, icono: 'military_tech' },
  { id: 'sesiones_100', nombre: 'Centurion', desc: '100 sesiones registradas', campo: 'sesiones', umbral: 100, icono: 'emoji_events' },
  { id: 'distancia_10', nombre: '10 km', desc: 'Acumula 10 km', campo: 'distanciaKm', umbral: 10, icono: 'directions_run' },
  { id: 'distancia_50', nombre: 'Medio centenar', desc: 'Acumula 50 km', campo: 'distanciaKm', umbral: 50, icono: 'directions_run' },
  { id: 'distancia_100', nombre: '100 km', desc: 'Acumula 100 km', campo: 'distanciaKm', umbral: 100, icono: 'terrain' },
  { id: 'racha_3', nombre: 'En racha', desc: '3 dias seguidos', campo: 'rachaMaxima', umbral: 3, icono: 'bolt' },
  { id: 'racha_7', nombre: 'Semana perfecta', desc: '7 dias seguidos', campo: 'rachaMaxima', umbral: 7, icono: 'calendar_month' },
  { id: 'desnivel_500', nombre: 'Escalador', desc: '500 m de desnivel acumulado', campo: 'desnivelM', umbral: 500, icono: 'landscape' }
];

/**
 * Estado de medallas a partir de unas stats agregadas.
 * @param {{sesiones?:number, distanciaKm?:number, rachaMaxima?:number, desnivelM?:number}} stats
 */
function medallas(stats = {}) {
  const valor = (campo) => Number(stats[campo] || 0);
  return CATALOGO_MEDALLAS.map((m) => {
    const v = valor(m.campo);
    return {
      id: m.id,
      nombre: m.nombre,
      desc: m.desc,
      icono: m.icono,
      desbloqueada: v >= m.umbral,
      progreso: Math.max(0, Math.min(1, m.umbral > 0 ? Number((v / m.umbral).toFixed(2)) : 0))
    };
  });
}

/** Junta records, rachas y medallas en un resumen de logros. */
function construirLogros({ stats = {}, fechasSesiones = [], registros = [], hoy } = {}) {
  const rachas = calcularRachas(fechasSesiones, hoy || new Date());
  const statsConRacha = { ...stats, rachaMaxima: Math.max(Number(stats.rachaMaxima || 0), rachas.maxima) };
  const meds = medallas(statsConRacha);
  return {
    rachas,
    records: recordsPorEjercicio(registros),
    medallas: meds,
    medallasDesbloqueadas: meds.filter((m) => m.desbloqueada).length,
    medallasTotales: meds.length
  };
}

module.exports = {
  CATALOGO_MEDALLAS,
  recordsPorEjercicio,
  calcularRachas,
  medallas,
  construirLogros,
  _ymd: ymd,
  _diffDias: diffDias
};
