/**
 * AnalisisService: analitica deportiva avanzada tipo Strava (y mas).
 *
 * Son funciones PURAS (no tocan la BD) para que sean faciles de testear y
 * reutilizar tanto desde endpoints stateless como desde otros servicios.
 *
 * Incluye:
 *  - Zonas de frecuencia cardiaca (modelo 5 zonas por %FCmax).
 *  - Distribucion de tiempo en zonas a partir de muestras de pulso.
 *  - Carga de entreno (hrTSS) basada en reserva de frecuencia cardiaca.
 *  - Prediccion de marca para una distancia objetivo (formula de Riegel).
 *  - Estimacion de VO2max (test de Cooper).
 */

const ZONAS = [
  { zona: 1, nombre: 'Recuperacion', minPct: 0.5, maxPct: 0.6, color: '#9E9E9E' },
  { zona: 2, nombre: 'Resistencia', minPct: 0.6, maxPct: 0.7, color: '#4CAF50' },
  { zona: 3, nombre: 'Aerobico', minPct: 0.7, maxPct: 0.8, color: '#FFC107' },
  { zona: 4, nombre: 'Umbral', minPct: 0.8, maxPct: 0.9, color: '#FF9800' },
  { zona: 5, nombre: 'Maximo', minPct: 0.9, maxPct: 1.0, color: '#F44336' }
];

/** Devuelve la FCmax: la indicada o estimada por edad (Tanaka: 208 - 0.7*edad). */
function fcMaxEstimada({ fcMax, edad } = {}) {
  const fm = Number(fcMax);
  if (Number.isFinite(fm) && fm > 0) return Math.round(fm);
  const e = Number(edad);
  if (Number.isFinite(e) && e > 0 && e < 120) return Math.round(208 - 0.7 * e);
  return null;
}

/** Calcula los 5 rangos de pulso (lpm) a partir de FCmax. */
function zonasFC({ fcMax, edad } = {}) {
  const fm = fcMaxEstimada({ fcMax, edad });
  if (!fm) throw new Error('FCMAX_REQUERIDA');
  return {
    fcMax: fm,
    zonas: ZONAS.map((z) => ({
      zona: z.zona,
      nombre: z.nombre,
      color: z.color,
      min: Math.round(fm * z.minPct),
      // El maximo de cada zona es exclusivo salvo la ultima (incluye FCmax).
      max: z.zona === 5 ? fm : Math.round(fm * z.maxPct) - 1
    }))
  };
}

/** Devuelve el numero de zona (1-5) para un pulso dado y una FCmax. */
function zonaDeFc(fc, fcMax) {
  if (!Number.isFinite(fc) || fc <= 0) return null;
  const pct = fc / fcMax;
  if (pct < ZONAS[0].minPct) return 1; // por debajo de Z1 se cuenta como Z1
  for (const z of ZONAS) {
    if (pct < z.maxPct) return z.zona;
  }
  return 5;
}

/**
 * Distribucion de tiempo en zonas a partir de muestras de pulso.
 * @param {number[]} muestras  pulsos (lpm), uno por intervalo
 * @param {object} opts        { fcMax, edad, intervaloSeg = 1 }
 */
function distribucionZonas(muestras, { fcMax, edad, intervaloSeg = 1 } = {}) {
  const fm = fcMaxEstimada({ fcMax, edad });
  if (!fm) throw new Error('FCMAX_REQUERIDA');
  const arr = Array.isArray(muestras) ? muestras.map(Number).filter((x) => Number.isFinite(x) && x > 0) : [];
  const conteo = [0, 0, 0, 0, 0];
  for (const fc of arr) {
    const z = zonaDeFc(fc, fm);
    if (z) conteo[z - 1] += 1;
  }
  const totalMuestras = arr.length || 1;
  const intervalo = Number(intervaloSeg) > 0 ? Number(intervaloSeg) : 1;
  return {
    fcMax: fm,
    muestras: arr.length,
    zonas: ZONAS.map((z, i) => ({
      zona: z.zona,
      nombre: z.nombre,
      color: z.color,
      segundos: conteo[i] * intervalo,
      porcentaje: Number(((conteo[i] / totalMuestras) * 100).toFixed(1))
    }))
  };
}

/**
 * Carga de entrenamiento hrTSS basada en reserva de FC (metodo Karvonen).
 * IF = (FCmedia - FCreposo) / (FCmax - FCreposo); TSS = horas * IF^2 * 100.
 */
function estimarTSS({ durSeg, avgHr, fcReposo = 60, fcMax, edad } = {}) {
  const fm = fcMaxEstimada({ fcMax, edad });
  const dur = Number(durSeg);
  const hr = Number(avgHr);
  const reposo = Number(fcReposo);
  if (!fm) throw new Error('FCMAX_REQUERIDA');
  if (!Number.isFinite(dur) || dur <= 0) throw new Error('DURACION_INVALIDA');
  if (!Number.isFinite(hr) || hr <= 0) throw new Error('FC_MEDIA_INVALIDA');
  if (fm <= reposo) throw new Error('FCMAX_MENOR_QUE_REPOSO');
  let intensidad = (hr - reposo) / (fm - reposo);
  intensidad = Math.max(0, Math.min(1.05, intensidad));
  const horas = dur / 3600;
  const tss = Math.round(horas * intensidad * intensidad * 100);
  return {
    tss,
    intensidad: Number(intensidad.toFixed(3)),
    durSeg: dur,
    fcMax: fm
  };
}

/**
 * Prediccion de tiempo para una distancia objetivo (formula de Riegel).
 * t2 = t1 * (d2/d1)^exponente. El exponente 1.06 es el valor clasico de Riegel.
 */
function predecirTiempo({ distanciaM, tiempoSeg, objetivoM, exponente = 1.06 } = {}) {
  const d1 = Number(distanciaM);
  const t1 = Number(tiempoSeg);
  const d2 = Number(objetivoM);
  const exp = Number(exponente);
  if (!Number.isFinite(d1) || d1 <= 0) throw new Error('DISTANCIA_INVALIDA');
  if (!Number.isFinite(t1) || t1 <= 0) throw new Error('TIEMPO_INVALIDO');
  if (!Number.isFinite(d2) || d2 <= 0) throw new Error('OBJETIVO_INVALIDO');
  const t2 = t1 * Math.pow(d2 / d1, Number.isFinite(exp) && exp > 0 ? exp : 1.06);
  const ritmoSegPorKm = t2 / (d2 / 1000);
  return {
    objetivoM: d2,
    tiempoEstimadoSeg: Math.round(t2),
    tiempoEstimado: formatoTiempo(t2),
    ritmoSegPorKm: Math.round(ritmoSegPorKm),
    ritmo: `${formatoTiempo(ritmoSegPorKm)}/km`
  };
}

/** VO2max estimado por el test de Cooper (distancia recorrida en 12 min). */
function vo2maxCooper(distancia12minM) {
  const d = Number(distancia12minM);
  if (!Number.isFinite(d) || d <= 0) throw new Error('DISTANCIA_INVALIDA');
  const vo2 = (d - 504.9) / 44.73;
  return Number(Math.max(0, vo2).toFixed(1));
}

/** Formatea segundos a H:MM:SS o M:SS. */
function formatoTiempo(totalSeg) {
  const s = Math.max(0, Math.round(Number(totalSeg) || 0));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const seg = s % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(seg)}` : `${m}:${pad(seg)}`;
}

module.exports = {
  ZONAS,
  fcMaxEstimada,
  zonasFC,
  zonaDeFc,
  distribucionZonas,
  estimarTSS,
  predecirTiempo,
  vo2maxCooper,
  formatoTiempo
};
