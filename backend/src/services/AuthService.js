const db = require('../config/db');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const {
  OAuth2Client
} = require('google-auth-library');
class AuthService {
  static async cambiarPassword(userId, actual, nueva) {
    const nuevaLimpia = String(nueva || '');
    if (nuevaLimpia.length < 6) throw new Error('PASSWORD_CORTA');
    const [rows] = await db.query('SELECT password FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!rows?.length) throw new Error('USUARIO_NO_ENCONTRADO');
    const esValida = await bcrypt.compare(String(actual || ''), rows[0].password);
    if (!esValida) throw new Error('PASSWORD_ACTUAL_INCORRECTA');
    const hashed = await bcrypt.hash(nuevaLimpia, 10);
    await db.query('UPDATE usuarios SET password = ? WHERE id_usuario = ?', [hashed, userId]);
    return { ok: true };
  }

  static async registrar(userData) {
    const {
      nombre,
      email,
      password,
      genero,
      peso,
      altura,
      oposiciones_id_oposicion,
      modo_uso,
      marcasIniciales
    } = userData;
    const normalizedEmail = String(email || '').trim().toLowerCase();
    const modoUso = String(modo_uso || 'OPOSITOR').trim().toUpperCase() === 'FITNESS' ? 'FITNESS' : 'OPOSITOR';
    let oposicionFinal = oposiciones_id_oposicion;
    if (modoUso === 'FITNESS') {
      oposicionFinal = null;
    } else if (oposicionFinal == null) {
      throw new Error('Oposición obligatoria');
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      if (oposicionFinal != null) {
        const [op] = await connection.query('SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?', [oposicionFinal]);
        if (!op || op.length === 0) {
          throw new Error('Oposición no válida');
        }
      }
      const hashedPassword = await bcrypt.hash(password, 10);
      const alturaMetros = altura / 100;
      const imc = (peso / (alturaMetros * alturaMetros)).toFixed(2);
      const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro, oposiciones_id_oposicion, modo_uso) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)`;
      const [userResult] = await connection.query(sqlUsuario, [nombre, normalizedEmail, hashedPassword, genero, peso, altura, imc, oposicionFinal, modoUso]);
      const userId = userResult.insertId;
      await connection.query('INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]);
      if (marcasIniciales && marcasIniciales.length > 0) {
        const sqlMarcas = `INSERT INTO marcas_perfil (valord_record, fecha_logro, pruebas_oficiales_id_pruebas_oficiales, usuarios_id_usuario) VALUES (?, NOW(), ?, ?)`;
        for (const marca of marcasIniciales) {
          await connection.query(sqlMarcas, [marca.valor, marca.id_prueba, userId]);
        }
      }
      await connection.commit();
      const [userRows] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
      const user = userRows[0];
      delete user.password;
      return {
        success: true,
        userId,
        user
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
  static async login(email, password) {
    const normalizedEmail = String(email || '').trim().toLowerCase();
    const [rows] = await db.query('SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))', [normalizedEmail]);
    if (rows.length === 0) throw new Error('Usuario no encontrado');
    const usuario = rows[0];
    const esValida = await bcrypt.compare(password, usuario.password);
    if (!esValida) throw new Error('Contraseña incorrecta');
    delete usuario.password;
    return usuario;
  }
  static async _verificarTokenGoogle(googleToken, email) {
    const clientId = process.env.GOOGLE_CLIENT_ID;
    if (!clientId) {
      throw new Error('GOOGLE_CLIENT_ID no está configurado en las variables de entorno');
    }
    const client = new OAuth2Client(clientId);
    const ticket = await client.verifyIdToken({
      idToken: googleToken,
      audience: clientId
    });
    const payload = ticket.getPayload();
    if (payload.email !== email) {
      throw new Error('El email del token no coincide con el email proporcionado');
    }
    return payload;
  }
  static async loginConGoogle(googleToken, email, nombre) {
    const normalizedEmail = String(email || '').trim().toLowerCase();
    await AuthService._verificarTokenGoogle(googleToken, normalizedEmail);
    const [rows] = await db.query('SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))', [normalizedEmail]);
    if (rows.length > 0) {
      const usuario = rows[0];
      delete usuario.password;
      return usuario;
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const randomPassword = crypto.randomBytes(32).toString('hex');
      const hashedPassword = await bcrypt.hash(randomPassword, 10);
      const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro) VALUES (?, ?, ?, 'HOMBRE', 0, 0, 0, NOW())`;
      const [userResult] = await connection.query(sqlUsuario, [nombre, normalizedEmail, hashedPassword]);
      const userId = userResult.insertId;
      await connection.query('INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]);
      await connection.commit();
      const [nuevoUsuario] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
      delete nuevoUsuario[0].password;
      return nuevoUsuario[0];
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
  static async registrarConGoogle(googleToken, email, nombre) {
    const normalizedEmail = String(email || '').trim().toLowerCase();
    await AuthService._verificarTokenGoogle(googleToken, normalizedEmail);
    const [rows] = await db.query('SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))', [normalizedEmail]);
    if (rows.length > 0) {
      const usuario = rows[0];
      delete usuario.password;
      return usuario;
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const randomPassword = crypto.randomBytes(32).toString('hex');
      const hashedPassword = await bcrypt.hash(randomPassword, 10);
      const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro) VALUES (?, ?, ?, 'HOMBRE', 0, 0, 0, NOW())`;
      const [userResult] = await connection.query(sqlUsuario, [nombre, normalizedEmail, hashedPassword]);
      const userId = userResult.insertId;
      await connection.query('INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]);
      await connection.commit();
      const [nuevoUsuario] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
      delete nuevoUsuario[0].password;
      return nuevoUsuario[0];
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
  static async loginConFirebase(idToken) {
    const {
      initFirebaseAdmin
    } = require('../config/firebaseAdmin');
    const admin = initFirebaseAdmin();
    const decoded = await admin.auth().verifyIdToken(idToken);
    const normalizedEmail = String(decoded.email || '').trim().toLowerCase();
    if (!normalizedEmail) throw new Error('Email no disponible en el token de Firebase');
    const [rows] = await db.query('SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))', [normalizedEmail]);
    if (rows.length > 0) {
      const usuario = rows[0];
      delete usuario.password;
      return usuario;
    }
    const err = new Error('USER_NOT_REGISTERED');
    err.code = 'USER_NOT_REGISTERED';
    throw err;
  }
  static async registrarConFirebase(idToken, profile) {
    const {
      initFirebaseAdmin
    } = require('../config/firebaseAdmin');
    const admin = initFirebaseAdmin();
    const decoded = await admin.auth().verifyIdToken(idToken);
    const normalizedEmail = String(decoded.email || '').trim().toLowerCase();
    if (!normalizedEmail) throw new Error('Email no disponible en el token de Firebase');
    const nombreFinal = String(profile.nombre || decoded.name || '').trim();
    const genero = String(profile.genero || '').trim().toUpperCase();
    const peso = parseFloat(profile.peso);
    const altura = parseFloat(profile.altura);
    const oposiciones_id_oposicion = profile.oposiciones_id_oposicion != null ? parseInt(profile.oposiciones_id_oposicion, 10) : null;
    if (!nombreFinal) throw new Error('El nombre es obligatorio');
    if (!genero || genero !== 'HOMBRE' && genero !== 'MUJER') throw new Error('Género no válido');
    if (!Number.isFinite(peso) || peso <= 0) throw new Error('Peso no válido');
    if (!Number.isFinite(altura) || altura <= 0) throw new Error('Altura no válida');
    if (oposiciones_id_oposicion == null || Number.isNaN(oposiciones_id_oposicion)) throw new Error('Oposición obligatoria');
    const [existe] = await db.query('SELECT id_usuario FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))', [normalizedEmail]);
    if (existe.length > 0) {
      const e = new Error('ALREADY_REGISTERED');
      e.code = 'ALREADY_REGISTERED';
      throw e;
    }
    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const [op] = await connection.query('SELECT id_oposicion FROM oposiciones WHERE id_oposicion = ?', [oposiciones_id_oposicion]);
      if (!op || op.length === 0) {
        throw new Error('Oposición no válida');
      }
      const randomPassword = crypto.randomBytes(32).toString('hex');
      const hashedPassword = await bcrypt.hash(randomPassword, 10);
      const alturaMetros = altura / 100;
      const imc = (peso / (alturaMetros * alturaMetros)).toFixed(2);
      const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro, oposiciones_id_oposicion) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)`;
      const [userResult] = await connection.query(sqlUsuario, [nombreFinal, normalizedEmail, hashedPassword, genero, peso, altura, imc, oposiciones_id_oposicion]);
      const userId = userResult.insertId;
      await connection.query('INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]);
      await connection.commit();
      const [nuevoUsuario] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
      delete nuevoUsuario[0].password;
      return nuevoUsuario[0];
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}
module.exports = AuthService;
