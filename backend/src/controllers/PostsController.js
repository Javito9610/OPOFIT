const PostsService = require('../services/PostsService');
const PostsModerService = require('../services/PostsModerService');

const crear = async (req, res) => {
  try {
    const data = await PostsService.crear(req.usuario.id, req.body || {});
    res.status(201).json({ ok: true, data });
  } catch (e) {
    console.error('Posts crear:', e.message);
    const code = e.message === 'TITULO_OBLIGATORIO' ? 400 : 500;
    res.status(code).json({ ok: false, msg: e.message });
  }
};

const feed = async (req, res) => {
  try {
    const limite = Number(req.query.limite) || 30;
    const data = await PostsService.feed(req.usuario.id, limite);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Posts feed:', e.message);
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const porUsuario = async (req, res) => {
  try {
    const target = Number(req.params.userId);
    const limite = Number(req.query.limite) || 30;
    const data = await PostsService.porUsuario(req.usuario.id, target, limite);
    res.json({ ok: true, data });
  } catch (e) {
    console.error('Posts porUsuario:', e.message);
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const detalle = async (req, res) => {
  try {
    const data = await PostsService.detalle(req.usuario.id, Number(req.params.idPost));
    res.json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'POST_NO_ENCONTRADO' ? 404
      : e.message === 'SIN_PERMISO' ? 403 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const toggleLike = async (req, res) => {
  try {
    const data = await PostsService.toggleLike(req.usuario.id, Number(req.params.idPost));
    res.json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'POST_NO_ENCONTRADO' ? 404
      : e.message === 'SIN_PERMISO' ? 403 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const comentar = async (req, res) => {
  try {
    const data = await PostsService.comentar(
      req.usuario.id,
      Number(req.params.idPost),
      req.body?.texto
    );
    res.status(201).json({ ok: true, data });
  } catch (e) {
    const status = e.message === 'COMENTARIO_VACIO' ? 400
      : e.message === 'POST_NO_ENCONTRADO' ? 404
      : e.message === 'SIN_PERMISO' ? 403 : 500;
    res.status(status).json({ ok: false, msg: e.message });
  }
};

const reportarPost = async (req, res) => {
  try {
    const userId = req.usuario.id;
    const { motivo, detalle } = req.body || {};
    const r = await PostsModerService.reportarPost(userId, Number(req.params.idPost), motivo, detalle);
    res.status(201).json({ ok: true, data: r });
  } catch (e) {
    const code = e.message === 'POST_NO_ENCONTRADO' ? 404
      : e.message === 'NO_PUEDES_REPORTAR_TU_POST' ? 400 : 500;
    res.status(code).json({ ok: false, msg: e.message });
  }
};

const reportarComentario = async (req, res) => {
  try {
    const userId = req.usuario.id;
    const { motivo, detalle } = req.body || {};
    const r = await PostsModerService.reportarComentario(userId, Number(req.params.idComentario), motivo, detalle);
    res.status(201).json({ ok: true, data: r });
  } catch (e) {
    const code = e.message === 'COMENTARIO_NO_ENCONTRADO' ? 404
      : e.message === 'NO_PUEDES_REPORTAR_TU_COMENTARIO' ? 400 : 500;
    res.status(code).json({ ok: false, msg: e.message });
  }
};

const listarReportesPendientes = async (req, res) => {
  try {
    const limite = Number(req.query.limite) || 50;
    const data = await PostsModerService.listarPendientes({ limite });
    res.json({ ok: true, data });
  } catch (e) {
    res.status(500).json({ ok: false, msg: e.message });
  }
};

const resolverReporte = async (req, res) => {
  try {
    const r = await PostsModerService.resolver(Number(req.params.idReporte), req.body?.accion);
    res.json({ ok: true, data: r });
  } catch (e) {
    res.status(400).json({ ok: false, msg: e.message });
  }
};

module.exports = {
  crear, feed, porUsuario, detalle, toggleLike, comentar,
  reportarPost, reportarComentario, listarReportesPendientes, resolverReporte
};
