const fs = require('fs');
const path = require('path');
const db = require('../config/db');
const EntornoEntreno = require('../utils/EntornoEntreno');
const EjercicioMetadataService = require('./EjercicioMetadataService');

// v4: ampliación con 138 ejercicios profesionales nuevos (Core, Hombros, Agilidad,
// Pliometría, Pierna/Glúteo, Pecho/Tirón, Brazos, Movilidad oposición).
const BANCO_VERSION = 4;

function resolveJsonPath() {
  const candidates = [
    path.resolve(__dirname, '../../data/ejercicios-banco-500.json'),
    path.resolve(__dirname, '../data/ejercicios-banco-500.json'),
    path.join(process.cwd(), 'data/ejercicios-banco-500.json')
  ];
  return candidates.find((p) => fs.existsSync(p)) || candidates[0];
}

function categoriaDesdePilar(pilar) {
  switch (String(pilar || '').toUpperCase()) {
    case 'MOVILIDAD':
      return 'Movilidad';
    case 'CORE':
      return 'Core';
    case 'RESISTENCIA':
      return 'Cardio';
    case 'VELOCIDAD':
      return 'Velocidad';
    default:
      return 'Fuerza';
  }
}

class EjerciciosBanco500Service {
  static async seedBanco500(force = false) {
    let meta = null;
    try {
      const [rows] = await db.query(
        `SELECT valor FROM app_meta WHERE clave = 'ejercicios_banco_500_version' LIMIT 1`
      );
      meta = rows[0];
    } catch (_) {
      /* app_meta puede no existir aún */
    }

    if (!force && meta?.valor === String(BANCO_VERSION)) {
      const [[{ n }]] = await db.query('SELECT COUNT(*) AS n FROM ejercicios');
      if (Number(n) >= 500) {
        return { skipped: true, version: BANCO_VERSION, total: Number(n) };
      }
    }

    const jsonPath = resolveJsonPath();
    if (!fs.existsSync(jsonPath)) {
      console.error(`[ejercicios-500] No se encuentra ${jsonPath}`);
      return { skipped: true, error: 'JSON_NOT_FOUND' };
    }

    const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    const ejercicios = data.ejercicios || [];
    let insertados = 0;
    let actualizados = 0;

    for (const e of ejercicios) {
      const nombre = String(e.nombre || '').trim().slice(0, 200);
      if (!nombre) continue;

      const pilar = String(e.pilar || 'FUERZA').toUpperCase();
      const grupo = EjercicioMetadataService.inferirGrupoMuscular(e.grupo_muscular, nombre, pilar);
      const equip = e.equipamiento || '—';
      const instr = EjercicioMetadataService.enriquecerInstrucciones(
        nombre,
        pilar,
        e.instrucciones_tecnicas
      );
      const entornos =
        e.entornos ||
        EntornoEntreno.inferirEntornosDesdeEquipamiento(equip, pilar).join(',');
      const ilust =
        e.tipo_ilustracion ||
        EntornoEntreno.inferirTipoIlustracion(nombre, pilar, grupo);
      const categoria = categoriaDesdePilar(pilar);

      const [exists] = await db.query(
        'SELECT id_ejercicio FROM ejercicios WHERE LOWER(nombre) = LOWER(?) LIMIT 1',
        [nombre]
      );

      if (exists.length) {
        await db.query(
          `UPDATE ejercicios SET
             instrucciones_tecnicas = ?,
             categoria = COALESCE(categoria, ?),
             pilar = COALESCE(pilar, ?),
             grupo_muscular = ?,
             equipamiento = COALESCE(equipamiento, ?),
             entornos = COALESCE(entornos, ?),
             tipo_ilustracion = COALESCE(tipo_ilustracion, ?)
           WHERE id_ejercicio = ?`,
          [instr, categoria, pilar, grupo, equip, entornos, ilust, exists[0].id_ejercicio]
        );
        actualizados += 1;
        continue;
      }

      await db.query(
        `INSERT INTO ejercicios
           (nombre, video_url, instrucciones_tecnicas, categoria, pilar, grupo_muscular, equipamiento, entornos, tipo_ilustracion)
         VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?)`,
        [nombre, instr, categoria, pilar, grupo, equip, entornos, ilust]
      );
      insertados += 1;
    }

    await db
      .query(
        `INSERT INTO app_meta (clave, valor) VALUES ('ejercicios_banco_500_version', ?)
         ON DUPLICATE KEY UPDATE valor = VALUES(valor)`,
        [String(BANCO_VERSION)]
      )
      .catch(async () => {
        await db.query(`
          CREATE TABLE IF NOT EXISTS app_meta (
            clave VARCHAR(64) PRIMARY KEY,
            valor VARCHAR(32) NOT NULL
          ) ENGINE=InnoDB
        `);
        await db.query(
          `INSERT INTO app_meta (clave, valor) VALUES ('ejercicios_banco_500_version', ?)
           ON DUPLICATE KEY UPDATE valor = VALUES(valor)`,
          [String(BANCO_VERSION)]
        );
      });

    const [[{ total }]] = await db.query('SELECT COUNT(*) AS total FROM ejercicios');
    console.log(
      `[ejercicios-500] Banco ampliado: +${insertados} nuevos, ${actualizados} actualizados, total BD: ${total}`
    );
    return {
      skipped: false,
      version: BANCO_VERSION,
      insertados,
      actualizados,
      total: Number(total)
    };
  }
}

module.exports = EjerciciosBanco500Service;
