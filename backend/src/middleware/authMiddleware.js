const jwt = require('jsonwebtoken');

const validarToken = (req, res, next) => {
    // 1. Buscamos el token en la cabecera 'Authorization'
    const authHeader = req.header('Authorization');

    if (!authHeader) {
        return res.status(401).json({
            ok: false,
            msg: 'No hay token en la petición. Acceso denegado.'
        });
    }

    // El formato suele ser "Bearer TOKEN", así que quitamos la palabra Bearer
    const token = authHeader.split(' ')[1];

    try {
        // 2. Verificamos el token con la misma clave secreta que usas en el Login
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'TU_CLAVE_SECRETA_DEL_TFG');
        
        // 3. Metemos los datos del usuario en la petición para que el controlador los use
        req.usuario = decoded;
        
        // 4. ¡Todo ok! Pasamos al siguiente paso (el controlador)
        next();
    } catch (error) {
        return res.status(401).json({
            ok: false,
            msg: 'Token no válido o caducado.'
        });
    }
};

module.exports = { validarToken };