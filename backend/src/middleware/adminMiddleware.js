const validarAdminApiKey = (req, res, next) => {
  const key = process.env.ADMIN_API_KEY;
  if (!key) {
    return res.status(503).json({
      ok: false,
      msg: 'Panel admin no configurado (ADMIN_API_KEY)'
    });
  }
  const provided = req.header('X-Admin-Key') || req.query.adminKey;
  if (provided !== key) {
    return res.status(403).json({ ok: false, msg: 'Clave de administración inválida' });
  }
  next();
};

module.exports = { validarAdminApiKey };
