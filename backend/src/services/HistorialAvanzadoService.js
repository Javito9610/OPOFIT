const db = require('../config/db');

/**
 * Servicio de historial profesional/jerárquico (Plan → Sesión → Ejercicio).
 *
 * Calcula agregados, deltas vs sesión anterior, récords y enlaza con
 * actividades GPS cuando existen.
 */
class HistorialAvanzadoService {
  static rangoPorPeriodo(periodo) {
    const ahora = new Date();
    const desde = new Date(ahora);
    switch (String(periodo).toLowerCase()) {
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

    const [agregados] = await db.query(
      `SELECT COUNT(*) AS sesiones,
              COALESCE(SUM(duracion_oficial), 0) AS minutos
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

    const [porDia] = await db.query(
      `SELECT DATE(fecha_entreno) AS dia, COUNT(*) AS n, COALESCE(SUM(duracion_oficial), 0) AS mins
       FROM historial_sesiones
       WHERE usuarios_id_usuario = ?
         AND fecha_entreno >= DATE_SUB(CURDATE(), INTERVAL 120 DAY)
       GROUP BY DATE(fecha_entreno)
       ORDER BY dia ASC`,
      [userId]
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

    const [topPrs] = await db.query(
      `SELECT e.nombre AS ejercicio, MAX(r.valor_conseguido) AS valor
       FROM registro_resultados r
       JOIN historial_sesiones h ON r.historial_sesiones_id_historial_sesiones = h.id_historial_sesion
       JOIN ejercicios e ON r.ejercicios_id_ejercicio = e.id_ejercicio
       WHERE h.usuarios_id_usuario = ?
       GROUP BY e.id_ejercicio, e.nombre
       ORDER BY valor DESC
       LIMIT 5`,
      [userId]
    );

    return {
      periodo: periodo || 'week',
      sesiones: Number(agregados?.[0]?.sesiones || 0),
      minutos: Number(agregados?.[0]?.minutos || 0),
      gps: {
        actividades: Number(gpsAgg?.[0]?.actividades || 0),
        distanciaM: Number(gpsAgg?.[0]?.distancia || 0),
        duracionSeg: Number(gpsAgg?.[0]?.dur || 0),
        desnivelPosM: Number(gpsAgg?.[0]?.desnivel || 0)
      },
      heatmap: porDia.map((d) => ({
        dia: typeof d.dia === 'string' ? d.dia.slice(0, 10) : new Date(d.dia).toISOString().slice(0, 10),
        sesiones: Number(d.n),
        minutos: Number(d.mins)
      })),
      porTipo: porTipo.map((t) => ({ tipo: t.tipo, sesiones: Number(t.n) })),
      topPrs: topPrs.map((p) => ({ ejercicio: p.ejercicio, valor: Number(p.valor) }))
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
              r.valor_conseguido, e.nombre AS nombre_ejercicio
       FROM registro_resultados r
       JOIN historial_sesiones h ON h.id_historial_sesion = r.historial_sesiones_id_historial_sesiones
       JOIN ejercicios e ON e.id_ejercicio = r.ejercicios_id_ejercicio
       WHERE h.usuarios_id_usuario = ? AND r.ejercicios_id_ejercicio = ?
       ORDER BY h.fecha_entreno ASC`,
      [userId, idEjercicio]
    );
    if (!puntos.length) return null;
    const vals = puntos.map((p) => Number(p.valor_conseguido));
    const best = Math.max(...vals);
    const worst = Math.min(...vals);
    const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
    const lifetime = puntos.length;
    return {
      ejercicio: puntos[0].nombre_ejercicio,
      sesiones: lifetime,
      mejor: best,
      peor: worst,
      media: Number(avg.toFixed(2)),
      puntos: puntos.map((p) => ({
        idSesion: p.id_sesion,
        fechaEntreno: p.fecha_entreno,
        duracionSeg: p.duracion_oficial,
        valor: Number(p.valor_conseguido)
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
