const jwt = require('jsonwebtoken');

/** Lee JWT del header Authorization o, en flujos OAuth por navegador, de ?token= */
function extraerToken(req) {
  const authHeader = req.header('Authorization');
  if (authHeader) {
    const partes = authHeader.split(' ');
    if (partes.length === 2 && partes[0] === 'Bearer') return partes[1];
    if (partes.length === 1) return partes[0];
  }
  const q = req.query?.token;
  if (typeof q === 'string' && q.trim()) return q.trim();
  return null;
}

const validarToken = (req, res, next) => {
  const token = extraerToken(req);
  if (!token) {
    return res.status(401).json({
      ok: false,
      msg: 'No hay token en la petición. Acceso denegado.'
    });
  }
  try {
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.usuario = decoded;
    next();
  } catch (error) {
    return res.status(401).json({
      ok: false,
      msg: 'Token no válido o caducado.'
    });
  }
};
module.exports = {
  validarToken
};
