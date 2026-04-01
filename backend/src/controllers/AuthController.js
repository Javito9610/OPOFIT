const AuthService = require('../services/AuthService');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const registrar = async (req, res) => {
  try {
    const userData = req.body;
    userData.email = String(userData.email || '').trim().toLowerCase();
    if (!userData.email || !userData.password) {
      return res.status(400).json({
        ok: false,
        msg: 'Faltan campos obligatorios (email o contraseña)'
      });
    }
    const resultado = await AuthService.registrar(userData);
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor'
      });
    }
    const token = jwt.sign({
      id: resultado.userId,
      email: userData.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    res.status(201).json({
      ok: true,
      msg: '¡Registro completado con éxito!',
      userId: resultado.userId,
      token: token,
      user: resultado.user
    });
  } catch (error) {
    console.error("Error en el registro:", error.message);
    if (error.message.includes("Duplicate entry") || error.message.includes("ya existe")) {
      return res.status(409).json({
        ok: false,
        msg: 'Este correo electrónico ya está registrado'
      });
    }
    if (error.message.includes('Oposición no válida')) {
      return res.status(400).json({
        ok: false,
        msg: 'Oposición no válida. Selecciona una oposición existente.'
      });
    }
    res.status(500).json({
      ok: false,
      msg: 'Error en el proceso de registro'
    });
  }
};
const login = async (req, res) => {
  try {
    const email = String(req.body?.email || '').trim().toLowerCase();
    const password = String(req.body?.password || '');
    if (!email || !password) {
      return res.status(400).json({
        ok: false,
        msg: "Email y contraseña requeridos"
      });
    }
    const usuario = await AuthService.login(email, password);
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const token = jwt.sign({
      id: usuario.id_usuario,
      email: usuario.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    res.status(200).json({
      ok: true,
      user: usuario,
      token: token
    });
  } catch (error) {
    const msg = error.message === 'Usuario no encontrado' ? 'Usuario no encontrado' : error.message === 'Contraseña incorrecta' ? 'Contraseña incorrecta' : "Credenciales incorrectas";
    res.status(401).json({
      ok: false,
      msg
    });
  }
};
const loginConGoogle = async (req, res) => {
  try {
    const {
      googleToken,
      email,
      nombre
    } = req.body;
    if (!googleToken || !email) {
      return res.status(400).json({
        ok: false,
        msg: "Token de Google y email son requeridos"
      });
    }
    const usuario = await AuthService.loginConGoogle(googleToken, email, nombre || 'Usuario Google');
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const token = jwt.sign({
      id: usuario.id_usuario,
      email: usuario.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    res.status(200).json({
      ok: true,
      user: usuario,
      userId: usuario.id_usuario,
      token: token
    });
  } catch (error) {
    console.error("Error en loginConGoogle:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al autenticar con Google"
    });
  }
};
const registrarConGoogle = async (req, res) => {
  try {
    const {
      googleToken,
      email,
      nombre
    } = req.body;
    if (!googleToken || !email) {
      return res.status(400).json({
        ok: false,
        msg: "Token de Google y email son requeridos"
      });
    }
    const usuario = await AuthService.registrarConGoogle(googleToken, email, nombre || 'Usuario Google');
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const token = jwt.sign({
      id: usuario.id_usuario,
      email: usuario.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    res.status(200).json({
      ok: true,
      user: usuario,
      userId: usuario.id_usuario,
      token: token
    });
  } catch (error) {
    console.error("Error en registrarConGoogle:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al registrar con Google"
    });
  }
};
const loginConFirebase = async (req, res) => {
  try {
    const {
      idToken
    } = req.body || {};
    if (!idToken) {
      return res.status(400).json({
        ok: false,
        msg: "idToken es requerido"
      });
    }
    const usuario = await AuthService.loginConFirebase(idToken);
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const token = jwt.sign({
      id: usuario.id_usuario,
      email: usuario.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    return res.status(200).json({
      ok: true,
      user: usuario,
      userId: usuario.id_usuario,
      token
    });
  } catch (error) {
    console.error("Error en loginConFirebase:", error.message);
    if (error.code === 'USER_NOT_REGISTERED') {
      return res.status(404).json({
        ok: false,
        code: 'USER_NOT_REGISTERED',
        msg: 'No hay cuenta con este correo. Regístrate primero en la pantalla de registro (también con Google).'
      });
    }
    const msg = error.message?.includes('Token') || error.message?.includes('verify') || error.message?.includes('Firebase') ? "Token de Firebase inválido" : "Error al autenticar con Firebase";
    return res.status(401).json({
      ok: false,
      msg
    });
  }
};
const registrarConFirebase = async (req, res) => {
  try {
    const {
      idToken,
      nombre,
      genero,
      peso,
      altura,
      oposiciones_id_oposicion
    } = req.body || {};
    if (!idToken) {
      return res.status(400).json({
        ok: false,
        msg: "idToken es requerido"
      });
    }
    const usuario = await AuthService.registrarConFirebase(idToken, {
      nombre,
      genero,
      peso,
      altura,
      oposiciones_id_oposicion
    });
    if (!process.env.JWT_SECRET) {
      return res.status(500).json({
        ok: false,
        msg: 'Error de configuración del servidor: JWT_SECRET no definido.'
      });
    }
    const token = jwt.sign({
      id: usuario.id_usuario,
      email: usuario.email
    }, process.env.JWT_SECRET, {
      expiresIn: '24h'
    });
    return res.status(201).json({
      ok: true,
      msg: '¡Registro completado con éxito!',
      userId: usuario.id_usuario,
      token,
      user: usuario
    });
  } catch (error) {
    console.error("Error en registrarConFirebase:", error.message);
    if (error.code === 'ALREADY_REGISTERED') {
      return res.status(409).json({
        ok: false,
        code: 'ALREADY_REGISTERED',
        msg: 'Este correo ya está registrado. Inicia sesión con Google.'
      });
    }
    if (error.message === 'Oposición no válida' || error.message === 'Género no válido' || error.message && (error.message.includes('obligatorio') || error.message.includes('no válido') || error.message.includes('Email no disponible'))) {
      return res.status(400).json({
        ok: false,
        msg: error.message
      });
    }
    if (error.message?.includes('Token') || error.message?.includes('verify')) {
      return res.status(401).json({
        ok: false,
        msg: 'Token de Firebase inválido'
      });
    }
    return res.status(500).json({
      ok: false,
      msg: 'Error al completar el registro con Google'
    });
  }
};
const me = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({
      ok: false,
      msg: "Token no válido o caducado."
    });
    const [rows] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!rows || rows.length === 0) {
      return res.status(401).json({
        ok: false,
        msg: "Sesión inválida o usuario no existe. Vuelve a iniciar sesión."
      });
    }
    const user = rows[0];
    delete user.password;
    return res.status(200).json({
      ok: true,
      user
    });
  } catch (e) {
    console.error("Error en auth.me:", e.message);
    return res.status(500).json({
      ok: false,
      msg: "Error al validar la sesión"
    });
  }
};
module.exports = {
  registrar,
  login,
  loginConGoogle,
  registrarConGoogle,
  loginConFirebase,
  registrarConFirebase,
  me
};
