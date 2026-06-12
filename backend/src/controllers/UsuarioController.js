const db = require('../config/db');
const RutinaService = require('../services/RutinasService');
const MarcaValidator = require('../utils/MarcaValidator');
const { guardarAvatar } = require('../services/AvatarService');

const obtenerPerfil = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (userId == null) {
      return res.status(401).json({ ok: false, msg: 'Sesión no válida' });
    }
    const [rows] = await db.query(
      `SELECT u.nombre, u.email, u.peso, u.altura, u.imc, u.avatar_url, u.modo_uso,
              u.oposiciones_id_oposicion AS oposicionId, o.nombre AS oposicionNombre,
              u.ubicacion_visible, u.dias_entreno_semana
       FROM usuarios u
       LEFT JOIN oposiciones o ON u.oposiciones_id_oposicion = o.id_oposicion
       WHERE u.id_usuario = ?`,
      [userId]
    );
    if (!rows?.length) {
      return res.status(404).json({ ok: false, msg: 'Usuario no encontrado' });
    }
    const u = rows[0];
    return res.status(200).json({
      ok: true,
      data: {
        nombre: u.nombre,
        email: u.email,
        peso: u.peso,
        altura: u.altura,
        imc: u.imc,
        avatarUrl: u.avatar_url,
        modoUso: u.modo_uso || 'OPOSITOR',
        oposicionId: u.oposicionId,
        oposicionNombre: u.oposicionNombre,
        ubicacionVisible: !!u.ubicacion_visible,
        diasEntrenoSemana: Number(u.dias_entreno_semana) || 5
      }
    });
  } catch (error) {
    console.error('Error en obtenerPerfil:', error.message);
    return res.status(500).json({ ok: false, msg: 'Error al obtener el perfil' });
  }
};

const actualizarPerfil = async (req, res) => {
  try {
    const {
      peso,
      altura,
      oposicionId,
      nuevasMarcas,
      nombre,
      avatarUrl,
      modoUso,
      ubicacionVisible,
      diasEntrenoSemana
    } = req.body;
    const userId = req.usuario?.id;
    if (userId == null) {
      return res.status(400).json({
        ok: false,
        msg: "Falta el identificador del usuario"
      });
    }
    if (nuevasMarcas != null && !Array.isArray(nuevasMarcas)) {
      return res.status(400).json({
        ok: false,
        msg: "El campo nuevasMarcas debe ser una lista"
      });
    }
    const hasPeso = typeof peso === 'number' && Number.isFinite(peso) && peso > 0;
    const hasAltura = typeof altura === 'number' && Number.isFinite(altura) && altura > 0;
    const hasMarcas = Array.isArray(nuevasMarcas) && nuevasMarcas.length > 0;
    const hasNombre = typeof nombre === 'string' && nombre.trim().length > 0;
    const hasAvatar = avatarUrl !== undefined && avatarUrl !== null;
    const hasModo = modoUso === 'OPOSITOR' || modoUso === 'FITNESS';
    const hasUbicVisible = ubicacionVisible !== undefined && ubicacionVisible !== null;
    const diasInt = Number(diasEntrenoSemana);
    const hasDias = Number.isFinite(diasInt) && diasInt >= 1 && diasInt <= 7;
    if (!hasPeso && !hasAltura && !hasMarcas && !hasNombre && !hasAvatar && !hasModo && !hasUbicVisible && !hasDias) {
      return res.status(400).json({
        ok: false,
        msg: 'Debes enviar al menos un campo para actualizar el perfil'
      });
    }
    const [userExists] = await db.query('SELECT id_usuario FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!userExists || userExists.length === 0) {
      return res.status(401).json({
        ok: false,
        msg: "Sesión inválida o usuario no existe. Vuelve a iniciar sesión."
      });
    }
    if (hasPeso && hasAltura) {
      const imc = (peso / (altura / 100) ** 2).toFixed(2);
      await db.query('UPDATE usuarios SET peso = ?, altura = ?, imc = ? WHERE id_usuario = ?', [peso, altura, imc, userId]);
    } else if (hasPeso) {
      const [[row]] = await db.query('SELECT altura FROM usuarios WHERE id_usuario = ?', [userId]);
      const currentAltura = Number(row?.altura);
      if (Number.isFinite(currentAltura) && currentAltura > 0) {
        const imc = (peso / (currentAltura / 100) ** 2).toFixed(2);
        await db.query('UPDATE usuarios SET peso = ?, imc = ? WHERE id_usuario = ?', [peso, imc, userId]);
      } else {
        await db.query('UPDATE usuarios SET peso = ? WHERE id_usuario = ?', [peso, userId]);
      }
    } else if (hasAltura) {
      const [[row]] = await db.query('SELECT peso FROM usuarios WHERE id_usuario = ?', [userId]);
      const currentPeso = Number(row?.peso);
      if (Number.isFinite(currentPeso) && currentPeso > 0) {
        const imc = (currentPeso / (altura / 100) ** 2).toFixed(2);
        await db.query('UPDATE usuarios SET altura = ?, imc = ? WHERE id_usuario = ?', [altura, imc, userId]);
      } else {
        await db.query('UPDATE usuarios SET altura = ? WHERE id_usuario = ?', [altura, userId]);
      }
    }
    if (hasNombre) {
      await db.query('UPDATE usuarios SET nombre = ? WHERE id_usuario = ?', [nombre.trim().substring(0, 80), userId]);
    }
    if (hasAvatar) {
      const url = String(avatarUrl || '').trim().substring(0, 512) || null;
      await db.query('UPDATE usuarios SET avatar_url = ? WHERE id_usuario = ?', [url, userId]);
    }
    if (hasModo) {
      await db.query('UPDATE usuarios SET modo_uso = ? WHERE id_usuario = ?', [modoUso, userId]);
      if (modoUso === 'FITNESS') {
        await db.query('UPDATE usuarios SET oposiciones_id_oposicion = NULL WHERE id_usuario = ?', [userId]);
      }
    }
    if (hasUbicVisible) {
      await db.query('UPDATE usuarios SET ubicacion_visible = ? WHERE id_usuario = ?', [ubicacionVisible ? 1 : 0, userId]);
    }
    if (hasDias) {
      await db.query('UPDATE usuarios SET dias_entreno_semana = ? WHERE id_usuario = ?', [diasInt, userId]);
      // Cambiar los días de entreno invalida el plan cacheado: hay que
      // regenerar con la nueva distribución semanal.
      await db.query('DELETE FROM planes_generados_cache WHERE usuarios_id_usuario = ?', [userId]).catch(() => {});
    }
    if (hasMarcas) {
      // Coherencia: validamos TODAS las marcas antes de escribir ninguna,
      // para no dejar el perfil a medias y rechazar valores imposibles.
      const [{ genero } = {}] = (await db.query('SELECT genero FROM usuarios WHERE id_usuario = ?', [userId]))[0] || [];
      const erroresMarca = [];
      for (const marca of nuevasMarcas) {
        if (!marca || marca.id_prueba == null || marca.valor == null) continue;
        const [pRows] = await db.query(
          'SELECT nombre_prueba, mejor_si_es_menor, unidad_entrada FROM pruebas_oficiales WHERE id_pruebas_oficiales = ?',
          [marca.id_prueba]
        );
        const prueba = pRows && pRows.length ? pRows[0] : { unidad_entrada: null };
        const v = MarcaValidator.validarMarcaPrueba(prueba, marca.valor, genero || null);
        if (!v.ok) erroresMarca.push({ id_prueba: marca.id_prueba, msg: v.msg });
      }
      if (erroresMarca.length > 0) {
        return res.status(400).json({
          ok: false,
          msg: erroresMarca.map((e) => e.msg).join('; '),
          errores: erroresMarca
        });
      }
      for (const marca of nuevasMarcas) {
        if (!marca || marca.id_prueba == null || marca.valor == null) continue;
        const [upd] = await db.query(`UPDATE marcas_perfil
                     SET valord_record = ?, fecha_logro = NOW()
                     WHERE usuarios_id_usuario = ? AND pruebas_oficiales_id_pruebas_oficiales = ?`, [marca.valor, userId, marca.id_prueba]);
        if (!upd || upd.affectedRows === 0) {
          await db.query(`INSERT INTO marcas_perfil (usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales, valord_record, fecha_logro)
                         VALUES (?, ?, ?, NOW())`, [userId, marca.id_prueba, marca.valor]);
        }
        await db.query(`DELETE mp FROM marcas_perfil mp
                     JOIN (
                       SELECT usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales, MAX(id_marcas_perfil) AS keep_id
                       FROM marcas_perfil
                       WHERE usuarios_id_usuario = ? AND pruebas_oficiales_id_pruebas_oficiales = ?
                       GROUP BY usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales
                     ) t
                     ON mp.usuarios_id_usuario = t.usuarios_id_usuario
                     AND mp.pruebas_oficiales_id_pruebas_oficiales = t.pruebas_oficiales_id_pruebas_oficiales
                     WHERE mp.id_marcas_perfil <> t.keep_id`, [userId, marca.id_prueba]);
      }
    }
    const resolvedOposicionId = oposicionId ?? null;
    if (!hasMarcas && !hasPeso && !hasAltura) {
      return res.status(200).json({ ok: true, msg: 'Perfil actualizado' });
    }
    const resultadoNivel = resolvedOposicionId ? await RutinaService.calcularNotaYNivel(userId, resolvedOposicionId) : null;
    if (!resultadoNivel) {
      return res.status(200).json({
        ok: true,
        msg: 'Perfil actualizado, pero no se pudo recalcular el nivel. Revisa tus marcas.'
      });
    }
    res.status(200).json({
      ok: true,
      msg: 'Perfil actualizado',
      nuevoNivel: resultadoNivel.nivelSugerido,
      nuevaNota: resultadoNivel.notaMedia
    });
  } catch (error) {
    console.error("Error en actualizarPerfil:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al actualizar el perfil"
    });
  }
};
const eliminarCuenta = async (req, res) => {
  const userId = req.usuario?.id;
  if (userId == null) {
    return res.status(401).json({
      ok: false,
      msg: 'Sesión no válida'
    });
  }
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    const [userRows] = await connection.query('SELECT id_usuario FROM usuarios WHERE id_usuario = ?', [userId]);
    if (!userRows || userRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        ok: false,
        msg: 'Usuario no encontrado'
      });
    }
    const [sesiones] = await connection.query('SELECT id_historial_sesion FROM historial_sesiones WHERE usuarios_id_usuario = ?', [userId]);
    const idsSesion = sesiones.map(s => s.id_historial_sesion);
    if (idsSesion.length > 0) {
      const placeholders = idsSesion.map(() => '?').join(',');
      await connection.query(`DELETE FROM registro_resultados WHERE historial_sesiones_id_historial_sesiones IN (${placeholders})`, idsSesion);
    }
    await connection.query('DELETE FROM historial_sesiones WHERE usuarios_id_usuario = ?', [userId]);
    await connection.query(`DELETE drp FROM detalle_rutina_pers drp
             INNER JOIN rutinas_pers rp ON drp.rutinas_pers_id_rutina_pers = rp.id_rutina_pers
             WHERE rp.usuarios_id_usuario = ?`, [userId]);
    await connection.query('DELETE FROM rutinas_pers WHERE usuarios_id_usuario = ?', [userId]);
    await connection.query('DELETE FROM marcas_perfil WHERE usuarios_id_usuario = ?', [userId]);
    await connection.query('DELETE FROM settings WHERE usuarios_id_usuario = ?', [userId]);
    await connection.query('DELETE FROM usuarios WHERE id_usuario = ?', [userId]);
    await connection.commit();
    return res.status(200).json({
      ok: true,
      msg: 'Cuenta eliminada correctamente'
    });
  } catch (error) {
    await connection.rollback();
    console.error('Error en eliminarCuenta:', error.message);
    return res.status(500).json({
      ok: false,
      msg: 'No se pudo eliminar la cuenta'
    });
  } finally {
    connection.release();
  }
};
const obtenerAjustes = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión no válida' });
    const [[u]] = await db.query(
      `SELECT COALESCE(s.unidad_peso, 'kg') AS unidadPeso,
              COALESCE(s.unidad_distancia, 'km') AS unidadDistancia,
              COALESCE(s.material_disponible, 'NADA') AS materialDisponible,
              COALESCE(u.hora_recordatorio_entreno, '18:00:00') AS horaRecordatorio,
              COALESCE(u.recordatorio_entreno_activo, 1) AS recordatorioActivo
         FROM usuarios u
         LEFT JOIN settings s ON s.usuarios_id_usuario = u.id_usuario
        WHERE u.id_usuario = ?
        LIMIT 1`,
      [userId]
    );
    if (!u) return res.status(404).json({ ok: false, msg: 'Usuario no encontrado' });
    // Extract just the hour as integer for UI (the column stores TIME 'HH:MM:SS')
    const horaInt = parseInt(String(u.horaRecordatorio).split(':')[0], 10) || 18;
    return res.status(200).json({
      ok: true,
      data: {
        unidadPeso: u.unidadPeso,
        unidadDistancia: u.unidadDistancia,
        materialDisponible: String(u.materialDisponible || 'NADA').split(',').filter(Boolean),
        horaRecordatorio: horaInt,
        recordatorioActivo: Number(u.recordatorioActivo) === 1
      }
    });
  } catch (e) {
    console.error('obtenerAjustes:', e.message);
    return res.status(500).json({ ok: false, msg: 'Error al cargar ajustes' });
  }
};

const actualizarSettings = async (req, res) => {
  try {
    const {
      userId,
      unidadPeso,
      unidadDistancia,
      horaRecordatorio,
      recordatorioActivo,
      materialDisponible // array opcional con códigos: ['BARRA_DOMINADAS','KB',...]
    } = req.body;
    if (!userId || !unidadPeso || !unidadDistancia) {
      return res.status(400).json({
        ok: false,
        msg: "Faltan preferencias de unidades"
      });
    }
    if (parseInt(userId) !== req.usuario.id) {
      return res.status(403).json({
        ok: false,
        msg: "No tienes permiso para modificar ajustes de otro usuario"
      });
    }
    await db.query('UPDATE settings SET unidad_peso = ?, unidad_distancia = ? WHERE usuarios_id_usuario = ?', [unidadPeso, unidadDistancia, userId]);
    if (Array.isArray(materialDisponible)) {
      // Validamos contra una lista cerrada para no permitir basura en BD.
      const VALIDOS = new Set([
        'NADA', 'BARRA_DOMINADAS', 'BARRA_OLIMPICA', 'MANCUERNAS', 'KB',
        'TRX', 'ANILLAS', 'GOMAS', 'COMBA', 'SACO', 'FOAM', 'BANCO',
        'CAJA', 'BICI', 'REMO', 'ECHO_BIKE', 'SKI_ERG', 'PISCINA',
        'PISTA', 'MONTANA', 'GIMNASIO_COMPLETO'
      ]);
      const limpio = materialDisponible
        .map((m) => String(m).trim().toUpperCase())
        .filter((m) => VALIDOS.has(m));
      const csv = limpio.length ? limpio.join(',') : 'NADA';
      await db.query('UPDATE settings SET material_disponible = ? WHERE usuarios_id_usuario = ?', [csv, userId]);
    }
    if (horaRecordatorio != null) {
      const h = String(horaRecordatorio).match(/^\d{1,2}/)?.[0] || '18';
      await db.query('UPDATE usuarios SET hora_recordatorio_entreno = ? WHERE id_usuario = ?', [
        `${h.padStart(2, '0')}:00:00`,
        userId
      ]);
    }
    if (recordatorioActivo != null) {
      await db.query('UPDATE usuarios SET recordatorio_entreno_activo = ? WHERE id_usuario = ?', [
        recordatorioActivo ? 1 : 0,
        userId
      ]);
    }
    res.status(200).json({
      ok: true,
      msg: "Ajustes guardados"
    });
  } catch (error) {
    console.error("Error en actualizarSettings:", error.message);
    res.status(500).json({
      ok: false,
      msg: "Error al guardar los ajustes"
    });
  }
};
const subirAvatar = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    const { imagenBase64 } = req.body || {};
    if (!userId) {
      return res.status(401).json({ ok: false, msg: 'Sesión no válida' });
    }
    const avatarPath = guardarAvatar(userId, imagenBase64);
    await db.query('UPDATE usuarios SET avatar_url = ? WHERE id_usuario = ?', [avatarPath, userId]);
    return res.status(200).json({ ok: true, avatarUrl: avatarPath });
  } catch (e) {
    console.error('subirAvatar:', e.message);
    return res.status(400).json({ ok: false, msg: e.message || 'No se pudo guardar la foto' });
  }
};

/**
 * Actualiza SOLO el material disponible. Endpoint ligero para el flujo de
 * selección de entorno (al elegir CASA/PISTA/MIXTO la app pregunta qué
 * material tienes y lo guarda aquí, sin arrastrar unidades ni recordatorios
 * como exige actualizarSettings).
 */
const actualizarMaterial = async (req, res) => {
  try {
    const userId = req.usuario?.id;
    if (!userId) return res.status(401).json({ ok: false, msg: 'Sesión no válida' });
    const { materialDisponible } = req.body || {};
    if (!Array.isArray(materialDisponible)) {
      return res.status(400).json({ ok: false, msg: 'materialDisponible debe ser una lista' });
    }
    const VALIDOS = new Set([
      'NADA', 'BARRA_DOMINADAS', 'BARRA_OLIMPICA', 'MANCUERNAS', 'KB',
      'TRX', 'ANILLAS', 'GOMAS', 'COMBA', 'SACO', 'FOAM', 'BANCO',
      'CAJA', 'BICI', 'REMO', 'ECHO_BIKE', 'SKI_ERG', 'PISCINA',
      'PISTA', 'MONTANA', 'GIMNASIO_COMPLETO'
    ]);
    const limpio = materialDisponible
      .map((m) => String(m).trim().toUpperCase())
      .filter((m) => VALIDOS.has(m));
    const csv = limpio.length ? limpio.join(',') : 'NADA';
    await db.query(
      'UPDATE settings SET material_disponible = ? WHERE usuarios_id_usuario = ?',
      [csv, userId]
    );
    // Invalidamos el plan cacheado: el material cambia los ejercicios que la
    // IA puede usar, así que la próxima carga regenera con el material nuevo.
    await db.query(
      'DELETE FROM planes_generados_cache WHERE usuarios_id_usuario = ?',
      [userId]
    ).catch(() => {});
    return res.status(200).json({ ok: true, material: limpio });
  } catch (e) {
    console.error('actualizarMaterial:', e.message);
    return res.status(500).json({ ok: false, msg: 'No se pudo guardar el material' });
  }
};

module.exports = {
  obtenerPerfil,
  obtenerAjustes,
  actualizarPerfil,
  actualizarSettings,
  actualizarMaterial,
  eliminarCuenta,
  subirAvatar
};
