const db = require('../config/db');
const NotificationService = require('../services/NotificationService');

const listOposiciones = async (_req, res) => {
  const [rows] = await db.query('SELECT * FROM oposiciones ORDER BY id_oposicion');
  res.json({ ok: true, data: rows });
};

const upsertOposicion = async (req, res) => {
  const { id_oposicion, nombre, incluida_gratis } = req.body || {};
  if (id_oposicion) {
    await db.query(
      'UPDATE oposiciones SET nombre = ?, incluida_gratis = ? WHERE id_oposicion = ?',
      [nombre, incluida_gratis ? 1 : 0, id_oposicion]
    );
    return res.json({ ok: true, id: id_oposicion });
  }
  const [r] = await db.query(
    'INSERT INTO oposiciones (nombre, incluida_gratis) VALUES (?, ?)',
    [nombre, incluida_gratis ? 1 : 0]
  );
  res.json({ ok: true, id: r.insertId });
};

const listEjercicios = async (_req, res) => {
  const [rows] = await db.query('SELECT * FROM ejercicios ORDER BY nombre');
  res.json({ ok: true, data: rows });
};

const upsertEjercicio = async (req, res) => {
  const { id_ejercicio, nombre, video_url, instrucciones_tecnicas } = req.body || {};
  if (id_ejercicio) {
    await db.query(
      'UPDATE ejercicios SET nombre=?, video_url=?, instrucciones_tecnicas=? WHERE id_ejercicio=?',
      [nombre, video_url || null, instrucciones_tecnicas || null, id_ejercicio]
    );
    return res.json({ ok: true, id: id_ejercicio });
  }
  const [r] = await db.query(
    'INSERT INTO ejercicios (nombre, video_url, instrucciones_tecnicas) VALUES (?,?,?)',
    [nombre, video_url || null, instrucciones_tecnicas || null]
  );
  res.json({ ok: true, id: r.insertId });
};

const listPruebas = async (req, res) => {
  const idOpo = req.query.oposicion;
  const [rows] = await db.query(
    'SELECT * FROM pruebas_oficiales WHERE oposiciones_id_oposicion = ? ORDER BY id_pruebas_oficiales',
    [idOpo]
  );
  res.json({ ok: true, data: rows });
};

const upsertPrueba = async (req, res) => {
  const {
    id_pruebas_oficiales,
    nombre_prueba,
    descripcion,
    trucos,
    oposiciones_id_oposicion,
    mejor_si_es_menor
  } = req.body || {};
  if (id_pruebas_oficiales) {
    await db.query(
      `UPDATE pruebas_oficiales SET nombre_prueba=?, descripcion=?, trucos=?,
       oposiciones_id_oposicion=?, mejor_si_es_menor=? WHERE id_pruebas_oficiales=?`,
      [
        nombre_prueba,
        descripcion,
        trucos || null,
        oposiciones_id_oposicion,
        mejor_si_es_menor ? 1 : 0,
        id_pruebas_oficiales
      ]
    );
    return res.json({ ok: true, id: id_pruebas_oficiales });
  }
  const [r] = await db.query(
    `INSERT INTO pruebas_oficiales (nombre_prueba, descripcion, trucos, oposiciones_id_oposicion, mejor_si_es_menor)
     VALUES (?,?,?,?,?)`,
    [nombre_prueba, descripcion, trucos || null, oposiciones_id_oposicion, mejor_si_es_menor ? 1 : 0]
  );
  res.json({ ok: true, id: r.insertId });
};

const upsertBaremo = async (req, res) => {
  const { id_baremo, pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota } =
    req.body || {};
  if (id_baremo) {
    await db.query(
      'UPDATE baremos_puntuacion SET genero=?, marca_valor=?, nota=? WHERE id_baremo=?',
      [genero, marca_valor, nota, id_baremo]
    );
    return res.json({ ok: true, id: id_baremo });
  }
  const [r] = await db.query(
    `INSERT INTO baremos_puntuacion (pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota)
     VALUES (?,?,?,?)`,
    [pruebas_oficiales_id_pruebas_oficiales, genero, marca_valor, nota]
  );
  res.json({ ok: true, id: r.insertId });
};

const enviarRecordatorios = async (_req, res) => {
  const r = await NotificationService.enviarRecordatorioEntreno();
  res.json({ ok: true, ...r });
};

const enviarNoticia = async (req, res) => {
  const { idOposicion, titulo, cuerpo } = req.body || {};
  const r = await NotificationService.enviarNoticiaOposicion(idOposicion, titulo, cuerpo);
  res.json({ ok: true, ...r });
};

module.exports = {
  listOposiciones,
  upsertOposicion,
  listEjercicios,
  upsertEjercicio,
  listPruebas,
  upsertPrueba,
  upsertBaremo,
  enviarRecordatorios,
  enviarNoticia
};
