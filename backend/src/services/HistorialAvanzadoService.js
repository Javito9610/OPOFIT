const db = require('../config/db');

/**
 * Servicio de historial profesional/jerárquico (Plan → Sesión → Ejercicio).
 *
 * Calcula agregados, deltas vs sesión anterior, récords y enlaza con
 * actividades GPS cuando existen.
 */
function if_(cond, a, b) { return cond ? a : b; }

class HistorialAvanzadoService {
  static rangoPorPeriodo(periodo) {
    const ahora = new Date();
    const desde = new Date(ahora);
    switch (String(periodo).toLowerCase()) {
      case 'all': desde.setFullYear(2000, 0, 1); break;
      case 'year': desde.setFullYear(desde.getFullYear() - 1); break;
      case 'month': desde.setMonth(desde.getMonth() - 1); break;
      case 'week':
      default: desde.setDate(desde.getDate() - 7); break;
    }
    return { desde, hasta: ahora };
  }

  static async resumen(userId, periodo) {
    const { desde } = HistorialAvanzadoService.rangoPorPeriodo(periodo);
    const desdeYmd = desde.toISOString().slice(0, 10);
    const distancePattern = '(carrera|trote|rodaje|fartlek|marcha|caminar|bici|ciclismo|sprint)';
    const swimPattern = '(natacion|natación|nadar|nado)';

    const [agregados] = await db.query(
      `SELECT COUNT(*) AS sesiones,
              COALESCE(SUM(duracion_oficial), 0) AS segundos_total
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ? AND fecha_entreno >= ?`,
      [userId, desdeYmd]
    );

    const [gpsAgg] = await db.query(
      `SELECT COUNT(*) AS actividades,
              COALESCE(SUM(distancia_m), 0) AS distancia,
              COALESCE(SUM(duracion_seg), 0) AS dur,
              COALESCE(SUM(desnivel_pos_m), 0) AS desnivel
       FROM gps_actividades
       WHERE usuarios_id_usuario = ?
         AND iniciada_en >= ?`,
      [userId, desde.getTime()]
    );

    const [ejKm] = await db.query(
      `SELECT
         COALESCE(SUM(CASE WHEN LOWER(e.nombre) REGEXP ? THEN r.valor_conseguido ELSE 0 END), 0) AS km_carrera,
         COALESCE(SUM(CASE WHEN LOWER(e.nombre) REGEXP ? THEN r.valor_conseguido / 1000 ELSE 0 END), 0) AS km_nado
       FROM registro_resultados r
       JOIN historial_sesiones h ON h.id_historial_sesion = r.historial_sesiones_id_historial_sesiones
       JOIN ejercicios e ON e.id_ejercicio = r.ejercicios_id_ejercicio
       WHERE h.usuarios_id_usuario = ? AND h.fecha_entreno >= ?`,
      [distancePattern, swimPattern, userId, desdeYmd]
    );

    // Heatmap: unimos sesiones de entreno + actividades GPS para que el calendario
    // muestre TODA la actividad del usuario (antes solo pintaba entrenos clásicos
    // y se veían huecos los días que solo había salida GPS).
    const [porDia] = await db.query(
      `SELECT dia, SUM(n) AS n, SUM(segs) AS segs FROM (
         SELECT DATE(fecha_entreno) AS dia, COUNT(*) AS n, COALESCE(SUM(duracion_oficial), 0) AS segs
           FROM historial_sesiones
          WHERE usuarios_id_usuario = ?
            AND fecha_entreno >= DATE_SUB(CURDATE(), INTERVAL 120 DAY)
          GROUP BY DATE(fecha_entreno)
         UNION ALL
         SELECT DATE(FROM_UNIXTIME(iniciada_en / 1000)) AS dia,
                COUNT(*) AS n,
                COALESCE(SUM(duracion_seg), 0) AS segs
           FROM gps_actividades
          WHERE usuarios_id_usuario = ?
            AND iniciada_en >= UNIX_TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 120 DAY)) * 1000
          GROUP BY DATE(FROM_UNIXTIME(iniciada_en / 1000))
       ) u
       GROUP BY dia
       ORDER BY dia ASC`,
      [userId, userId]
    );

    const [porTipo] = await db.query(
      `SELECT
         CASE
           WHEN h.tipo_rutina = 'PERS' THEN 'PERSONAL'
           WHEN ro.enfoque_tipo IS NOT NULL THEN ro.enfoque_tipo
           ELSE 'OTRO'
         END AS tipo,
         COUNT(*) AS n
       FROM historial_sesiones h
       LEFT JOIN rutinas_opo ro ON ro.id_rutina_opo = h.rutinas_opo_id_rutina_opo
       WHERE h.usuarios_id_usuario = ? AND h.fecha_entreno >= ?
       GROUP BY tipo`,
      [userId, desdeYmd]
    );

    // Devolvemos modalidad + score_tipo para que el frontend muestre la unidad
    // correcta (kg, reps, seg, kcal, rondas, etc.) en lugar de "120,00" pelado.
    // Top PRs como Strong/Hevy: solo ejercicios donde el usuario ha registrado
    // ≥2 sesiones (hay PROGRESIÓN, no marcas iniciales). Antes se llenaba
    // con cualquier ejercicio hecho una sola vez con un número grande
    // (ej: "Natación crol 50m: 300m"). El usuario reportaba que era basura.
    const [topPrs] = await db.query(
      `SELECT e.nombre AS ejercicio,
              MAX(r.valor_conseguido) AS valor,
              MIN(r.valor_conseguido) AS valor_min,
              COUNT(*) AS n_intentos,
              e.pilar,
              e.modalidad,
              e.score_tipo
         FROM registro_resultados r
         JOIN historial_sesiones h ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
         JOIN ejercicios e ON r.ejercicios_id_ejercicio = e.id_ejercicio
        WHERE h.usuarios_id_usuario = ?
        GROUP BY e.id_ejercicio, e.nombre, e.pilar, e.modalidad, e.score_tipo
       HAVING COUNT(*) >= 2
        ORDER BY (MAX(r.valor_conseguido) - MIN(r.valor_conseguido)) DESC, valor DESC
        LIMIT 5`,
      [userId]
    );

    // Sumamos segundos de entrenos clásicos + sesiones GPS para "Minutos" totales.
    // Antes el card "Minutos: 0" engañaba porque las salidas GPS no contaban.
    const segundosEntrenos = Number(agregados?.[0]?.segundos_total || 0);
    const segundosGps = Number(gpsAgg?.[0]?.dur || 0);
    const segundosTotal = segundosEntrenos + segundosGps;
    const minutos = Math.round(segundosTotal / 60);
    // Mismo criterio para el contador de "Sesiones": entrenos + actividades GPS.
    const sesionesEntrenos = Number(agregados?.[0]?.sesiones || 0);
    const sesionesGps = Number(gpsAgg?.[0]?.actividades || 0);
    const sesionesTotal = sesionesEntrenos + sesionesGps;
    const gpsDistanciaKm = Number(gpsAgg?.[0]?.distancia || 0) / 1000;
    const kmCarrera = Number(ejKm?.[0]?.km_carrera || 0);
    const kmNado = Number(ejKm?.[0]?.km_nado || 0);
    const distanciaTotalKm = Number((gpsDistanciaKm + kmCarrera + kmNado).toFixed(2));

    // kcal estimadas simplificadas: GPS reales si los hay + estimación para ejercicios de carrera
    // (~60 kcal/km como aproximación para 70 kg)
    const [usuario] = await db.query(
      'SELECT peso FROM usuarios WHERE id_usuario = ?',
      [userId]
    );
    const peso = Number(usuario?.[0]?.peso) || 70;
    const kcalGps = await db.query(
      `SELECT COALESCE(SUM(distancia_m * ?  / 1000), 0) AS kcal_estim
       FROM gps_actividades
       WHERE usuarios_id_usuario = ? AND iniciada_en >= ?`,
      [Number((peso * 0.9).toFixed(2)), userId, desde.getTime()]
    );
    const kcalCarrera = Math.round(kmCarrera * peso * 0.9);
    const kcalTotal = Math.round(Number(kcalGps[0]?.[0]?.kcal_estim || 0)) + kcalCarrera;

    return {
      periodo: periodo || 'week',
      // Total real = entrenos + actividades GPS (no solo entrenos).
      sesiones: sesionesTotal,
      sesionesEntrenos,
      sesionesGps,
      minutos,
      distanciaTotalKm,
      kcalTotal,
      gps: {
        actividades: Number(gpsAgg?.[0]?.actividades || 0),
        distanciaM: Number(gpsAgg?.[0]?.distancia || 0),
        duracionSeg: Number(gpsAgg?.[0]?.dur || 0),
        desnivelPosM: Number(gpsAgg?.[0]?.desnivel || 0)
      },
      heatmap: porDia.map((d) => ({
        dia: typeof d.dia === 'string' ? d.dia.slice(0, 10) : new Date(d.dia).toISOString().slice(0, 10),
        sesiones: Number(d.n),
        minutos: Math.round(Number(d.segs) / 60)
      })),
      // La distribución también suma las actividades GPS como categoría "GPS"
      // (igual que sesionesTotal arriba), para que el donut refleje la
      // actividad real del usuario y no solo entrenos clásicos.
      porTipo: (() => {
        const base = porTipo.map((t) => ({ tipo: t.tipo, sesiones: Number(t.n) }));
        if (sesionesGps > 0) base.push({ tipo: 'GPS', sesiones: sesionesGps });
        return base;
      })(),
      topPrs: topPrs.map((p) => ({
        ejercicio: p.ejercicio,
        valor: Number(p.valor),
        pilar: p.pilar,
        scoreTipo: p.score_tipo || null,
        unidad: (() => {
          // Mapeo score_tipo → unidad mostrable. Coincide con la lógica del
          // frontend para que ambos lados queden consistentes.
          switch (p.score_tipo) {
            case 'peso': return 'kg';
            case 'tiempo':
            case 'tiempo_max': return 's';
            case 'distancia': return 'm';
            case 'calorias': return 'kcal';
            case 'rondas':
            case 'rondas_completadas': return 'rondas';
            default: return 'reps';
          }
        })()
      }))
    };
  }

  static async listarSesiones(userId, filtros) {
    const where = ['h.usuarios_id_usuario = ?'];
    const params = [userId];
    if (filtros.tipo === 'OPO' || filtros.tipo === 'PERS') {
      where.push('h.tipo_rutina = ?');
      params.push(filtros.tipo);
    }
    if (filtros.planId) {
      where.push('pd.planes_id_plan = ?');
      params.push(Number(filtros.planId));
    }
    if (filtros.desde) {
      where.push('h.fecha_entreno >= ?');
      params.push(filtros.desde);
    }
    if (filtros.hasta) {
      where.push('h.fecha_entreno <= ?');
      params.push(filtros.hasta);
    }
    const sql = `
      SELECT h.id_historial_sesion AS id, h.fecha_entreno, h.tipo_rutina,
             h.duracion_oficial, h.gps_actividad_uuid,
             h.rutinas_opo_id_rutina_opo AS id_rutina_opo,
             h.rutinas_pers_id_rutina_pers AS id_rutina_pers,
             ro.enfoque_tipo, ro.nivel,
             pd.planes_id_plan AS id_plan, pd.titulo_sesion AS titulo,
             p.nombre AS plan_nombre, p.nivel AS plan_nivel,
             rp.nombre_personalizado,
             (SELECT COUNT(*) FROM registro_resultados rr WHERE rr.historial_sesiones_id_historial_sesiones = h.id_historial_sesion) AS n_ejercicios
      FROM historial_sesiones h
      LEFT JOIN rutinas_opo ro ON ro.id_rutina_opo = h.rutinas_opo_id_rutina_opo
      LEFT JOIN plan_dias pd ON pd.rutinas_opo_id = ro.id_rutina_opo
      LEFT JOIN planes_entrenamiento p ON p.id_plan = pd.planes_id_plan
      LEFT JOIN rutinas_pers rp ON rp.id_rutina_pers = h.rutinas_pers_id_rutina_pers
      WHERE ${where.join(' AND ')}
      ORDER BY h.fecha_entreno DESC
      LIMIT ?
    `;
    const limit = Math.min(Number(filtros.limit) || 100, 500);
    params.push(limit);
    const [rows] = await db.query(sql, params);
    return rows.map((r) => ({
      id: r.id,
      fechaEntreno: r.fecha_entreno,
      tipoRutina: r.tipo_rutina,
      duracionSeg: r.duracion_oficial,
      gpsActividadUuid: r.gps_actividad_uuid,
      idRutinaOpo: r.id_rutina_opo,
      idRutinaPers: r.id_rutina_pers,
      enfoque: r.enfoque_tipo,
      nivel: r.nivel,
      idPlan: r.id_plan,
      tituloSesion: r.titulo,
      planNombre: r.plan_nombre,
      planNivel: r.plan_nivel,
      nombrePersonalizado: r.nombre_personalizado,
      nEjercicios: Number(r.n_ejercicios || 0)
    }));
  }

  static async detalleSesion(userId, idSesion) {
    const [head] = await db.query(
      `SELECT h.id_historial_sesion AS id, h.fecha_entreno, h.tipo_rutina,
              h.duracion_oficial, h.gps_actividad_uuid,
              h.rutinas_opo_id_rutina_opo AS id_rutina_opo,
              h.rutinas_pers_id_rutina_pers AS id_rutina_pers,
              ro.enfoque_tipo, ro.nivel,
              pd.planes_id_plan AS id_plan, pd.titulo_sesion AS titulo,
              p.nombre AS plan_nombre,
              rp.nombre_personalizado
       FROM historial_sesiones h
       LEFT JOIN rutinas_opo ro ON ro.id_rutina_opo = h.rutinas_opo_id_rutina_opo
       LEFT JOIN plan_dias pd ON pd.rutinas_opo_id = ro.id_rutina_opo
       LEFT JOIN planes_entrenamiento p ON p.id_plan = pd.planes_id_plan
       LEFT JOIN rutinas_pers rp ON rp.id_rutina_pers = h.rutinas_pers_id_rutina_pers
       WHERE h.usuarios_id_usuario = ? AND h.id_historial_sesion = ?
       LIMIT 1`,
      [userId, idSesion]
    );
    if (!head.length) return null;
    const h = head[0];

    const [ejercicios] = await db.query(
      `SELECT r.id_resultado, r.ejercicios_id_ejercicio AS id_ejercicio,
              e.nombre AS nombre_ejercicio, e.categoria, e.pilar, r.valor_conseguido AS valor
       FROM registro_resultados r
       JOIN ejercicios e ON e.id_ejercicio = r.ejercicios_id_ejercicio
       WHERE r.historial_sesiones_id_historial_sesiones = ?`,
      [idSesion]
    );

    const enriquecidos = [];
    for (const ej of ejercicios) {
      const [previo] = await db.query(
        `SELECT r2.valor_conseguido
         FROM registro_resultados r2
         JOIN historial_sesiones h2 ON h2.id_historial_sesion = r2.historial_sesiones_id_historial_sesiones
         WHERE h2.usuarios_id_usuario = ? AND r2.ejercicios_id_ejercicio = ?
           AND h2.fecha_entreno < ?
         ORDER BY h2.fecha_entreno DESC LIMIT 1`,
        [userId, ej.id_ejercicio, h.fecha_entreno]
      );
      const [maxRow] = await db.query(
        `SELECT MAX(r3.valor_conseguido) AS mx
         FROM registro_resultados r3
         JOIN historial_sesiones h3 ON h3.id_historial_sesion = r3.historial_sesiones_id_historial_sesiones
         WHERE h3.usuarios_id_usuario = ? AND r3.ejercicios_id_ejercicio = ?
           AND h3.id_historial_sesion <> ?`,
        [userId, ej.id_ejercicio, idSesion]
      );
      const previoVal = previo[0]?.valor_conseguido != null ? Number(previo[0].valor_conseguido) : null;
      const recordHistorico = maxRow[0]?.mx != null ? Number(maxRow[0].mx) : null;
      const esPr = recordHistorico == null || Number(ej.valor) > recordHistorico;
      enriquecidos.push({
        idResultado: ej.id_resultado,
        idEjercicio: ej.id_ejercicio,
        nombre: ej.nombre_ejercicio,
        categoria: ej.categoria,
        pilar: ej.pilar,
        valor: Number(ej.valor),
        valorAnterior: previoVal,
        delta: previoVal != null ? Number((Number(ej.valor) - previoVal).toFixed(2)) : null,
        esPr
      });
    }

    let gpsActividad = null;
    if (h.gps_actividad_uuid) {
      const [gpsRow] = await db.query(
        `SELECT * FROM gps_actividades WHERE usuarios_id_usuario = ? AND uuid_local = ? LIMIT 1`,
        [userId, h.gps_actividad_uuid]
      );
      if (gpsRow.length) {
        const g = gpsRow[0];
        gpsActividad = {
          id: g.uuid_local,
          type: g.tipo,
          distanceM: Number(g.distancia_m),
          durationSec: g.duracion_seg,
          avgPaceSecPerKm: Number(g.ritmo_medio_spkm),
          avgSpeedMps: Number(g.velocidad_media_mps),
          elevationGainM: Number(g.desnivel_pos_m || 0),
          avgHrBpm: g.cadencia_media_ppm == null ? null : Number(g.cadencia_media_ppm)
        };
      }
    }

    return {
      id: h.id,
      fechaEntreno: h.fecha_entreno,
      tipoRutina: h.tipo_rutina,
      duracionSeg: h.duracion_oficial,
      enfoque: h.enfoque_tipo,
      nivel: h.nivel,
      idPlan: h.id_plan,
      tituloSesion: h.titulo,
      planNombre: h.plan_nombre,
      nombrePersonalizado: h.nombre_personalizado,
      idRutinaOpo: h.id_rutina_opo,
      idRutinaPers: h.id_rutina_pers,
      gpsActividadUuid: h.gps_actividad_uuid,
      gpsActividad,
      ejercicios: enriquecidos
    };
  }

  static async historialEjercicio(userId, idEjercicio) {
    const [puntos] = await db.query(
      `SELECT h.id_historial_sesion AS id_sesion, h.fecha_entreno, h.duracion_oficial,
              h.gps_actividad_uuid,
              r.valor_conseguido,
              e.nombre AS nombre_ejercicio, e.categoria, e.pilar, e.video_url
       FROM registro_resultados r
       JOIN historial_sesiones h ON h.id_historial_sesion = r.historial_sesiones_id_historial_sesiones
       JOIN ejercicios e ON e.id_ejercicio = r.ejercicios_id_ejercicio
       WHERE h.usuarios_id_usuario = ? AND r.ejercicios_id_ejercicio = ?
       ORDER BY h.fecha_entreno ASC`,
      [userId, idEjercicio]
    );
    if (!puntos.length) return null;
    const head = puntos[0];
    const nombre = head.nombre_ejercicio;
    const nombreLower = String(nombre).toLowerCase();
    const isCarrera = /(carrera|trote|rodaje|fartlek)/.test(nombreLower);
    const isSprint = /(sprint|100 ?m|60 ?m|30 ?m|400 ?m|800 ?m|1000 ?m)/.test(nombreLower);
    const isNado = /(natacion|natación|nadar|nado)/.test(nombreLower);
    const isBici = /(bici|ciclismo)/.test(nombreLower);
    const isCardio = head.pilar === 'RESISTENCIA' || head.pilar === 'VELOCIDAD' || isCarrera || isNado || isBici || isSprint;

    // Heurística: si el valor parece tiempo (sprint o ejercicios cortos con valor < 60 y nombre con tiempo),
    // menor=mejor. Si el valor parece distancia (km, m grandes, reps), mayor=mejor.
    const vals = puntos.map((p) => Number(p.valor_conseguido));
    const avgValRaw = vals.reduce((a, b) => a + b, 0) / vals.length;
    // En sprints típicos los tiempos son < 600s. Si es sprint o velocidad y los valores son pequeños (< 600), asumimos tiempo.
    const valoresParecenTiempo = (isSprint || head.pilar === 'VELOCIDAD') && avgValRaw < 600;
    const menorEsMejor = valoresParecenTiempo;
    const distanceUnit = if_(
      isCarrera || isBici, 'km',
      if_(isNado, 'm', if_(valoresParecenTiempo, 's', 'reps'))
    );

    const best = menorEsMejor ? Math.min(...vals) : Math.max(...vals);
    const worst = menorEsMejor ? Math.max(...vals) : Math.min(...vals);
    const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
    const totalDistanceKm = isCarrera || isBici
      ? vals.reduce((s, v) => s + v, 0)
      : isNado
        ? vals.reduce((s, v) => s + v, 0) / 1000
        : 0;

    // Métricas extra cuando hay GPS asociado: ritmo medio / max, vel max, HR media/max
    let mejorRitmoSpKm = null, mejorVelMps = 0, hrMaxLifetime = 0, totalDesnivelM = 0, kcalLifetime = 0, distGpsKm = 0;
    const uuids = puntos.map((p) => p.gps_actividad_uuid).filter(Boolean);
    if (uuids.length > 0) {
      const ph = uuids.map(() => '?').join(',');
      const [gpsRows] = await db.query(
        `SELECT uuid_local, ritmo_medio_spkm, ritmo_min_spkm, velocidad_max_mps,
                cadencia_media_ppm, desnivel_pos_m, distancia_m
         FROM gps_actividades
         WHERE usuarios_id_usuario = ? AND uuid_local IN (${ph})`,
        [userId, ...uuids]
      );
      for (const g of gpsRows) {
        const r = Number(g.ritmo_min_spkm || g.ritmo_medio_spkm);
        if (r > 0 && (mejorRitmoSpKm === null || r < mejorRitmoSpKm)) mejorRitmoSpKm = r;
        if (Number(g.velocidad_max_mps) > mejorVelMps) mejorVelMps = Number(g.velocidad_max_mps);
        if (Number(g.desnivel_pos_m) > 0) totalDesnivelM += Number(g.desnivel_pos_m);
        distGpsKm += Number(g.distancia_m) / 1000;
      }
    }

    return {
      ejercicio: nombre,
      pilar: head.pilar || null,
      categoria: head.categoria || null,
      esCardio: isCardio,
      menorEsMejor,
      unidad: distanceUnit,
      sesiones: puntos.length,
      mejor: Number(best.toFixed(2)),
      peor: Number(worst.toFixed(2)),
      media: Number(avg.toFixed(2)),
      totalDistanciaKm: Number(totalDistanceKm.toFixed(2)),
      mejorRitmoSpKm,
      mejorVelMps: mejorVelMps > 0 ? mejorVelMps : null,
      totalDesnivelM: Math.round(totalDesnivelM),
      kcalLifetime,
      distGpsKm: Number(distGpsKm.toFixed(2)),
      puntos: puntos.map((p) => ({
        idSesion: p.id_sesion,
        fechaEntreno: p.fecha_entreno,
        duracionSeg: p.duracion_oficial,
        valor: Number(p.valor_conseguido),
        gpsActividadUuid: p.gps_actividad_uuid
      }))
    };
  }

  static async historialPlan(userId, idPlan) {
    const [rows] = await db.query(
      `SELECT h.id_historial_sesion AS id, h.fecha_entreno, h.duracion_oficial,
              h.gps_actividad_uuid,
              ro.enfoque_tipo, ro.nivel,
              pd.id_plan_dia, pd.dia_semana, pd.titulo_sesion
       FROM historial_sesiones h
       JOIN rutinas_opo ro ON ro.id_rutina_opo = h.rutinas_opo_id_rutina_opo
       LEFT JOIN plan_dias pd ON pd.rutinas_opo_id = ro.id_rutina_opo
       WHERE h.usuarios_id_usuario = ? AND pd.planes_id_plan = ?
       ORDER BY h.fecha_entreno DESC`,
      [userId, idPlan]
    );
    const [planMeta] = await db.query(
      `SELECT id_plan, nombre, nivel, genero, dias_por_semana FROM planes_entrenamiento WHERE id_plan = ? LIMIT 1`,
      [idPlan]
    );
    return {
      plan: planMeta[0] || null,
      totalSesiones: rows.length,
      sesiones: rows.map((r) => ({
        id: r.id,
        fechaEntreno: r.fecha_entreno,
        duracionSeg: r.duracion_oficial,
        enfoque: r.enfoque_tipo,
        nivel: r.nivel,
        idPlanDia: r.id_plan_dia,
        diaSemana: r.dia_semana,
        tituloSesion: r.titulo_sesion,
        gpsActividadUuid: r.gps_actividad_uuid
      }))
    };
  }
}

module.exports = HistorialAvanzadoService;
