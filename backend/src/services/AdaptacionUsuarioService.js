/**
 * AdaptacionUsuarioService — la capa que convierte el plan genérico en
 * un plan PARA TI: respeta lesiones, tiempo disponible y feedback previo.
 *
 * Tres entradas que un coach real siempre pregunta:
 *  1. ¿Tienes alguna lesión / molestia?  → lesiones[] = ['rodilla', 'hombro', ...]
 *  2. ¿Cuánto tiempo tienes hoy?         → tiempo_disponible_min = 30/45/60/90
 *  3. ¿Cómo te sentiste la sesión anterior? → fatiga_previa = 1..5
 *
 * Estos tres factores transforman el plan SIN cambiar el banco ni el motor
 * de prescripción — son una capa de adaptación encima.
 */

// =============================================================================
// 1) LESIONES — patrones contraindicados por cada zona afectada.
// Si el usuario marca "rodilla", evitamos pliometría + zancadas pesadas + sprints
// hasta que la quite. Ofrecemos alternativas seguras.
// =============================================================================

const LESIONES_CONTRAINDICACIONES = {
  rodilla: {
    bloquea_patrones: ['PLYO', 'SPRINT', 'AGI'],
    bloquea_regex: /(box jump|salto|sprint|conos|zancada pesada|sentadilla profund|pistol)/i,
    alternativa_pilar: {
      VELOCIDAD: 'Movilidad de cadera y core',
      FUERZA: 'Trabajo de tren superior + core'
    },
    mensaje: 'Ejercicios de impacto y profundidad de rodilla pausados hasta que retires la lesión.'
  },
  lumbar: {
    bloquea_patrones: ['HINGE'],
    bloquea_regex: /(peso muerto|good morning|hiperextension|kettlebell swing)/i,
    alternativa_pilar: {
      FUERZA: 'Cadena posterior con bajo impacto (puente de glúteo, bird-dog)'
    },
    mensaje: 'Bisagra de cadera con carga pausada. Trabajamos cadena posterior sin compresión axial.'
  },
  hombro: {
    bloquea_patrones: ['PUSH_V', 'PULL_V'],
    bloquea_regex: /(press militar|push press|dominada|muscle.up|handstand|pike push)/i,
    alternativa_pilar: {
      FUERZA: 'Trabajo de tren inferior + core, empujes horizontales ligeros'
    },
    mensaje: 'Empujes y tracciones verticales pausados. Mantenemos movilidad escapular.'
  },
  tobillo: {
    bloquea_patrones: ['PLYO', 'SPRINT', 'AGI'],
    bloquea_regex: /(salto|sprint|conos|skipping|carrera)/i,
    alternativa_pilar: {
      VELOCIDAD: 'Bicicleta / remo ergómetro',
      RESISTENCIA: 'Natación o bicicleta sin impacto'
    },
    mensaje: 'Impacto pausado. Cardio sin impacto recomendado (natación, bici).'
  },
  codo: {
    bloquea_patrones: ['PULL_V', 'PULL_H'],
    bloquea_regex: /(dominada|remo|chin.up|curl)/i,
    alternativa_pilar: {
      FUERZA: 'Tren inferior + core; trabajo isométrico de tronco'
    },
    mensaje: 'Tracciones pausadas. Hombro neutro y agarre suave.'
  },
  muneca: {
    bloquea_regex: /(handstand|flexion|press|peso muerto pesado)/i,
    alternativa_pilar: {
      FUERZA: 'Mancuernas con agarre neutro, máquinas que protegen la muñeca'
    },
    mensaje: 'Apoyo de muñeca pausado. Usa agarre neutro o asas.'
  }
};

function lesionAfectaEjercicio(nombre, patron, lesion) {
  const def = LESIONES_CONTRAINDICACIONES[String(lesion).toLowerCase()];
  if (!def) return false;
  if (def.bloquea_patrones?.includes(patron)) return true;
  if (def.bloquea_regex?.test(String(nombre || ''))) return true;
  return false;
}

/**
 * @param {Array<string>} lesiones  Array tipo ['rodilla', 'hombro']
 * @param {object} ejercicio        Ejercicio del plan
 * @returns {{bloqueado: boolean, motivos: string[]}}
 */
function evaluarLesiones(lesiones, ejercicio) {
  const arr = (lesiones || []).map((l) => String(l).toLowerCase());
  if (!arr.length) return { bloqueado: false, motivos: [] };
  const motivos = [];
  for (const l of arr) {
    if (lesionAfectaEjercicio(ejercicio.nombre, ejercicio.patron_movimiento, l)) {
      const def = LESIONES_CONTRAINDICACIONES[l];
      motivos.push(`${l}: ${def?.mensaje || 'contraindicado'}`);
    }
  }
  return { bloqueado: motivos.length > 0, motivos };
}

// =============================================================================
// 2) TIEMPO DISPONIBLE — comprime sesión manteniendo lo importante.
// Estrategia: si la sesión estimada > tiempo_disponible:
//   - PRIMER paso: dropear el ÚLTIMO ejercicio accesorio (no el principal).
//   - SEGUNDO paso: reducir series en los accesorios (no en el principal).
//   - TERCERO: si todavía no cabe, reducir descansos un 20%.
// =============================================================================

function estimarMinutosEjercicio(ej) {
  const series = Number(ej.series) || 0;
  const descanso = Number(ej.descanso) || 60;
  // ~1 min de trabajo por serie + el descanso. Conservador.
  return (series * (60 + descanso)) / 60;
}

function estimarMinutosSesion(ejercicios) {
  return (ejercicios || []).reduce((acc, ej) => acc + estimarMinutosEjercicio(ej), 0);
}

/**
 * Comprime un array de ejercicios para que entren en `minutos`.
 * @returns {{ejercicios: Array, ajustes: string[]}}
 */
function comprimirSesion(ejercicios, minutosObjetivo) {
  if (!Array.isArray(ejercicios) || !minutosObjetivo) {
    return { ejercicios: ejercicios || [], ajustes: [] };
  }
  let lista = [...ejercicios];
  const ajustes = [];
  let estimado = estimarMinutosSesion(lista);
  if (estimado <= minutosObjetivo) return { ejercicios: lista, ajustes };

  // Fase 1: dropear accesorios desde el final hasta el penúltimo, dejando
  // siempre el primero (el ejercicio principal) y el último (suele ser core).
  while (estimado > minutosObjetivo && lista.length > 3) {
    // Elimina el penúltimo (accesorio "más sacrificable")
    const i = lista.length - 2;
    const droppeado = lista[i];
    lista = [...lista.slice(0, i), ...lista.slice(i + 1)];
    ajustes.push(`Eliminado "${droppeado?.nombre || 'accesorio'}" para entrar en ${minutosObjetivo} min.`);
    estimado = estimarMinutosSesion(lista);
  }

  // Fase 2: reducir 1 serie en accesorios (no en el primero).
  if (estimado > minutosObjetivo) {
    lista = lista.map((ej, i) => {
      if (i === 0) return ej;
      const series = Math.max(2, (Number(ej.series) || 3) - 1);
      return { ...ej, series };
    });
    ajustes.push(`Reducida 1 serie en accesorios.`);
    estimado = estimarMinutosSesion(lista);
  }

  // Fase 3: descansos -20% (en accesorios) si aún no cabe.
  if (estimado > minutosObjetivo) {
    lista = lista.map((ej, i) => {
      if (i === 0) return ej;
      const descanso = Math.max(30, Math.round((Number(ej.descanso) || 60) * 0.8));
      return { ...ej, descanso };
    });
    ajustes.push(`Descansos −20% en accesorios.`);
    estimado = estimarMinutosSesion(lista);
  }

  return { ejercicios: lista, ajustes };
}

// =============================================================================
// 3) AUTOREGULACIÓN POR FATIGA — el usuario reporta cómo se sintió.
// fatiga_previa 1..5:
//   1 = pude haber hecho mucho más  → próxima sesión +10% volumen
//   2 = pude haber hecho un poco más → próxima sesión +5%
//   3 = justo lo que pude            → mantén
//   4 = me costó                     → próxima sesión -10%
//   5 = no acabé / me destrocé       → próxima sesión -25% + más descanso
// =============================================================================

const MULTIPLICADOR_FATIGA = {
  1: 1.10,
  2: 1.05,
  3: 1.00,
  4: 0.90,
  5: 0.75
};

function ajustarPorFatiga(ejercicios, fatigaPrevia) {
  const f = Number(fatigaPrevia);
  if (!f || f < 1 || f > 5) return { ejercicios, ajuste: null };
  const mult = MULTIPLICADOR_FATIGA[f];
  if (mult === 1.00) return { ejercicios, ajuste: null };
  const lista = (ejercicios || []).map((ej) => {
    const series = Math.max(2, Math.round((Number(ej.series) || 3) * mult));
    const descanso = f >= 4
      ? Math.round((Number(ej.descanso) || 60) * 1.2)  // más descanso si está fatigado
      : ej.descanso;
    return { ...ej, series, descanso };
  });
  const ajuste = f >= 4
    ? `Autoregulación: fatiga reportada ${f}/5 → volumen ${Math.round((1 - mult) * 100)}% menos + descansos 20% más.`
    : `Autoregulación: nivel ${f}/5 → volumen +${Math.round((mult - 1) * 100)}%.`;
  return { ejercicios: lista, ajuste };
}

// =============================================================================
// API pública
// =============================================================================

/**
 * Adapta una lista de ejercicios al usuario.
 * @param {Array} ejercicios
 * @param {object} ctx { lesiones, tiempoDisponibleMin, fatigaPrevia }
 * @returns {{ejercicios, ajustes: string[], avisos: string[]}}
 */
function adaptarSesion(ejercicios, ctx = {}) {
  let lista = ejercicios || [];
  const ajustes = [];
  const avisos = [];

  // 1) Filtro por lesiones.
  if (ctx.lesiones?.length) {
    const original = lista;
    lista = lista.filter((ej) => {
      const r = evaluarLesiones(ctx.lesiones, ej);
      if (r.bloqueado) {
        avisos.push(`${ej.nombre}: ${r.motivos.join('; ')}`);
        return false;
      }
      return true;
    });
    if (lista.length < original.length) {
      ajustes.push(`${original.length - lista.length} ejercicio(s) descartado(s) por lesión.`);
    }
  }

  // 2) Autoregulación por fatiga previa (antes de comprimir).
  if (ctx.fatigaPrevia) {
    const r = ajustarPorFatiga(lista, ctx.fatigaPrevia);
    lista = r.ejercicios;
    if (r.ajuste) ajustes.push(r.ajuste);
  }

  // 3) Compresión por tiempo disponible.
  if (ctx.tiempoDisponibleMin) {
    const r = comprimirSesion(lista, ctx.tiempoDisponibleMin);
    lista = r.ejercicios;
    ajustes.push(...r.ajustes);
  }

  return { ejercicios: lista, ajustes, avisos };
}

module.exports = {
  LESIONES_CONTRAINDICACIONES,
  evaluarLesiones,
  comprimirSesion,
  ajustarPorFatiga,
  estimarMinutosSesion,
  estimarMinutosEjercicio,
  adaptarSesion
};
