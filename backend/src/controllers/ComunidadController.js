const GruposService = require('../services/GruposService');
const NearbyService = require('../services/NearbyService');

const listarGrupos = async (req, res) => {
  try {
    const idOposicion = req.query.idOposicion != null && req.query.idOposicion !== ''
      ? Number(req.query.idOposicion)
      : null;
    const data = await GruposService.listarGrupos(req.usuario.id, idOposicion);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const crearGrupo = async (req, res) => {
  try {
    const { nombre, descripcion, idOposicion } = req.body || {};
    const data = await GruposService.crearGrupo(req.usuario.id, {
      nombre,
      descripcion,
      idOposicion: idOposicion != null ? Number(idOposicion) : null
    });
    res.status(201).json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'OPOSICION_NO_VALIDA' ? 400 : e.message === 'NOMBRE_OBLIGATORIO' ? 400 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const unirseGrupo = async (req, res) => {
  try {
    await GruposService.unirse(req.usuario.id, Number(req.params.idGrupo));
    res.json({ ok: true, msg: 'Te has unido al grupo' });
  } catch (e) {
    const status = ['GRUPO_NO_ENCONTRADO', 'YA_ERES_MIEMBRO'].includes(e.message) ? 400 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const salirGrupo = async (req, res) => {
  try {
    await GruposService.salir(req.usuario.id, Number(req.params.idGrupo));
    res.json({ ok: true, msg: 'Has salido del grupo' });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const mensajesGrupo = async (req, res) => {
  try {
    const limite = Number(req.query.limite) || 50;
    const data = await GruposService.mensajes(req.usuario.id, Number(req.params.idGrupo), limite);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const enviarMensajeGrupo = async (req, res) => {
  try {
    const { texto } = req.body || {};
    const data = await GruposService.enviarMensaje(req.usuario.id, Number(req.params.idGrupo), texto);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const crearQuedada = async (req, res) => {
  try {
    const data = await GruposService.crearQuedada(req.usuario.id, Number(req.params.idGrupo), req.body || {});
    res.status(201).json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const listarQuedadas = async (req, res) => {
  try {
    const data = await GruposService.listarQuedadas(req.usuario.id, Number(req.params.idGrupo));
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const actualizarUbicacion = async (req, res) => {
  try {
    const { lat, lng, visible } = req.body || {};
    const data = await NearbyService.actualizarUbicacion(req.usuario.id, lat, lng, !!visible);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const listarCerca = async (req, res) => {
  try {
    const lat = Number(req.query.lat);
    const lng = Number(req.query.lng);
    const radioKm = req.query.radioKm != null ? Number(req.query.radioKm) : 15;
    const data = await NearbyService.listarCerca(req.usuario.id, lat, lng, radioKm);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

module.exports = {
  listarGrupos,
  crearGrupo,
  unirseGrupo,
  salirGrupo,
  mensajesGrupo,
  enviarMensajeGrupo,
  crearQuedada,
  listarQuedadas,
  actualizarUbicacion,
  listarCerca
};
