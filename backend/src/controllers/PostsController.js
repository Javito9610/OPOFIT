const PostsService = require('../services/PostsService');

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

module.exports = { crear, feed, porUsuario, detalle, toggleLike, comentar };
