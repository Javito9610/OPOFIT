const fs = require('fs');
const path = require('path');
const db = require('../config/db');
const RutinaService = require('./RutinasService');

const JSON_PATH = path.resolve(__dirname, '../data/banco-planes-parsed.json');
const BANCO_VERSION = 2;

class BancoPlanesImportService {
  static async findOrCreateEjercicio(nombre, pilar) {
    const limpio = String(nombre || '').trim().slice(0, 200);
    if (!limpio) return null;
    const like = `%${limpio.slice(0, 40).replace(/%/g, '')}%`;
    const [rows] = await db.query(
      `SELECT id_ejercicio FROM ejercicios
       WHERE LOWER(nombre) = LOWER(?) OR LOWER(nombre) LIKE LOWER(?)
       ORDER BY CHAR_LENGTH(nombre) ASC LIMIT 1`,
      [limpio, like]
    );
    if (rows.length) return rows[0].id_ejercicio;

    const cat = pilar === 'VELOCIDAD' ? 'Velocidad' : pilar === 'RESISTENCIA' ? 'Cardio' : 'Fuerza';
    const [ins] = await db.query(
      `INSERT INTO ejercicios (nombre, video_url, instrucciones_tecnicas, categoria, pilar, grupo_muscular, equipamiento)
       VALUES (?, NULL, ?, ?, ?, 'General', 'Variable')`,
      [limpio, `Prescripción del banco OpoFit: ${limpio}`, cat, pilar || 'FUERZA']
    );
    return ins.insertId;
  }

  static async ensureOposiciones(data) {
    for (const opo of data.oposiciones) {
      const [ex] = await db.query('SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?', [
        opo.idPreferido
      ]);
      if (ex.length) {
        await db.query(
          'UPDATE oposiciones SET nombre = ?, incluida_gratis = 1 WHERE id_oposicion = ?',
          [opo.nombre, opo.idPreferido]
        );
      } else {
        await db.query(
          'INSERT INTO oposiciones (id_oposicion, nombre, incluida_gratis) VALUES (?, ?, 1)',
          [opo.idPreferido, opo.nombre]
        );
      }
    }
  }

  static async importarBancoCompleto(force = false) {
    const [[meta]] = await db.query(
      `SELECT valor FROM app_meta WHERE clave = 'banco_planes_version' LIMIT 1`
    ).catch(() => [[null]]);

    if (!force && meta?.valor === String(BANCO_VERSION)) {
      return { skipped: true, version: BANCO_VERSION };
    }

    if (!fs.existsSync(JSON_PATH)) {
      throw new Error(`Falta ${JSON_PATH}. Ejecuta: node scripts/parse-banco-planes.js`);
    }
    const data = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
    await BancoPlanesImportService.ensureOposiciones(data);

    await db.query('DELETE pde FROM plan_dia_ejercicios pde INNER JOIN plan_dias pd ON pde.plan_dias_id = pd.id_plan_dia');
    await db.query('DELETE FROM plan_dias');
    await db.query('DELETE FROM planes_entrenamiento WHERE fuente = ?', ['opofit_banco_planes']);

    let totalEjercicios = 0;
    let totalDias = 0;

    for (const opo of data.oposiciones) {
      const idOpo = opo.idPreferido;
      for (const plan of opo.planes || []) {
        const [insPlan] = await db.query(
          `INSERT INTO planes_entrenamiento
           (oposiciones_id_oposicion, nivel, genero, nombre, dias_por_semana, fuente)
           VALUES (?, ?, ?, ?, ?, 'opofit_banco_planes')`,
          [
            idOpo,
            plan.nivel,
            plan.genero,
            `Plan ${plan.nivel} · ${plan.genero === 'HOMBRE' ? 'Hombre' : 'Mujer'}`,
            plan.dias.length
          ]
        );
        const idPlan = insPlan.insertId;

        for (const dia of plan.dias) {
          const [insRutina] = await db.query(
            `INSERT INTO rutinas_opo (nivel, genero, enfoque_tipo, oposiciones_id_oposicion)
             VALUES (?, ?, ?, ?)`,
            [plan.nivel, plan.genero, dia.enfoque, idOpo]
          );
          const idRutinaOpo = insRutina.insertId;

          let ordenEj = 0;
          for (const ej of dia.ejercicios || []) {
            ordenEj += 1;
            const idEj = await BancoPlanesImportService.findOrCreateEjercicio(ej.nombre, dia.enfoque);
            if (!idEj) continue;
            const series = ej.series || 1;
            const reps = ej.repeticiones || 10;
            const desc = ej.descanso ?? 90;
            await db.query(
              `INSERT INTO detalle_rutina_opo
               (ejercicios_id_ejercicio, rutinas_opo_id_rutina_opo, repeticiones, series, descanso)
               VALUES (?, ?, ?, ?, ?)`,
              [idEj, idRutinaOpo, reps, series, desc]
            );
            totalEjercicios += 1;
          }

          const [insDia] = await db.query(
            `INSERT INTO plan_dias
             (planes_id_plan, dia_semana, orden, enfoque_tipo, rutinas_opo_id, titulo_sesion, descripcion_sesion)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [
              idPlan,
              dia.dia_semana,
              dia.orden,
              dia.enfoque,
              idRutinaOpo,
              dia.titulo || dia.descripcion?.slice(0, 200),
              dia.descripcion
            ]
          );
          const idPlanDia = insDia.insertId;

          ordenEj = 0;
          for (const ej of dia.ejercicios || []) {
            ordenEj += 1;
            const idEj = await BancoPlanesImportService.findOrCreateEjercicio(ej.nombre, dia.enfoque);
            await db.query(
              `INSERT INTO plan_dia_ejercicios
               (plan_dias_id, orden, nombre_prescripcion, ejercicios_id_ejercicio, series, repeticiones, descanso, notas)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
              [
                idPlanDia,
                ordenEj,
                ej.nombre,
                idEj,
                ej.series || 1,
                ej.repeticiones || 10,
                ej.descanso ?? 90,
                ej.unidad || 'reps'
              ]
            );
          }
          totalDias += 1;
        }
      }
    }

    await db.query(
      `INSERT INTO app_meta (clave, valor) VALUES ('banco_planes_version', ?)
       ON DUPLICATE KEY UPDATE valor = VALUES(valor)`,
      [String(BANCO_VERSION)]
    ).catch(async () => {
      await db.query(`
        CREATE TABLE IF NOT EXISTS app_meta (
          clave VARCHAR(64) PRIMARY KEY,
          valor VARCHAR(32) NOT NULL
        ) ENGINE=InnoDB
      `);
      await db.query(
        `INSERT INTO app_meta (clave, valor) VALUES ('banco_planes_version', ?)
         ON DUPLICATE KEY UPDATE valor = VALUES(valor)`,
        [String(BANCO_VERSION)]
      );
    });

    console.log(
      `[banco] Importados ${data.oposiciones.length} oposiciones, ${totalDias} días, ${totalEjercicios} líneas de ejercicio`
    );
    return {
      skipped: false,
      version: BANCO_VERSION,
      oposiciones: data.oposiciones.length,
      dias: totalDias
    };
  }
}

module.exports = BancoPlanesImportService;
