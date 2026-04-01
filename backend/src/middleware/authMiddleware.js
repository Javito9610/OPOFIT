const jwt = require('jsonwebtoken');
const validarToken = (req, res, next) => {
  const authHeader = req.header('Authorization');
  if (!authHeader) {
    return res.status(401).json({
      ok: false,
      msg: 'No hay token en la petición. Acceso denegado.'
    });
  }
  const token = authHeader.split(' ')[1];
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
