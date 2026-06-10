const AmigosService = require('../services/AmigosService');

const listar = async (req, res) => {
  try {
    const amigos = await AmigosService.listarAmigos(req.usuario.id);
    const pendientes = await AmigosService.solicitudesPendientes(req.usuario.id);
    res.json({ ok: true, data: { amigos, pendientes } });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const buscar = async (req, res) => {
  try {
    // Aceptamos tres aliases: q (preferido), nombre (legacy) y email — así
    // mantenemos la compatibilidad con el cliente antiguo y a la vez podemos
    // buscar por email desde el nuevo input.
    const consulta = (req.query.q || req.query.nombre || req.query.email || '').trim();
    const idOposicionRaw = req.query.idOposicion;
    const idOposicion = idOposicionRaw == null || idOposicionRaw === ''
      ? null
      : Number(idOposicionRaw);
    if (!consulta) {
      return res.status(400).json({ ok: false, msg: 'Indica nombre o email' });
    }
    const data = await AmigosService.buscarPorNombre(req.usuario.id, consulta, idOposicion);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const solicitar = async (req, res) => {
  try {
    const { idUsuario } = req.body || {};
    await AmigosService.enviarSolicitud(req.usuario.id, Number(idUsuario));
    res.json({ ok: true, msg: 'Solicitud enviada' });
  } catch (e) {
    const code = e.message;
    const status =
      code === 'DISTINTA_OPOSICION' ? 400 : code === 'USUARIO_NO_ENCONTRADO' ? 404 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const responder = async (req, res) => {
  try {
    const { idAmistad, aceptar } = req.body || {};
    await AmigosService.responderSolicitud(req.usuario.id, Number(idAmistad), !!aceptar);
    res.json({ ok: true });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const chat = async (req, res) => {
  try {
    const otroId = Number(req.params.otroId);
    const data = await AmigosService.obtenerChat(req.usuario.id, otroId);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const enviarMensaje = async (req, res) => {
  try {
    const { idDestinatario, texto } = req.body || {};
    const data = await AmigosService.enviarMensaje(req.usuario.id, Number(idDestinatario), texto || '');
    res.json({ ok: true, data });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

const feed = async (req, res) => {
  try {
    const data = await AmigosService.feedActividad(req.usuario.id);
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

module.exports = { listar, buscar, solicitar, responder, chat, enviarMensaje, feed };
