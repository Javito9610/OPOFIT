const db = require('../config/db');
const RutinaService = require('../services/RutinasService');
const actualizarPerfil = async (req, res) => {
  try {
    const {
      peso,
      altura,
      oposicionId,
      nuevasMarcas
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
    if (!hasPeso && !hasAltura && !hasMarcas) {
      return res.status(400).json({
        ok: false,
        msg: "Debes enviar peso/altura válidos y/o al menos una marca para actualizar el perfil"
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
    if (hasMarcas) {
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
    const resultadoNivel = resolvedOposicionId ? await RutinaService.calcularNotaYNivel(userId, resolvedOposicionId) : null;
    if (!resultadoNivel) {
      return res.status(200).json({
        ok: true,
        msg: "Perfil actualizado, pero no se pudo recalcular el nivel. Revisa tus marcas."
      });
    }
    res.status(200).json({
      ok: true,
      msg: "Perfil actualizado",
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
const actualizarSettings = async (req, res) => {
  try {
    const {
      userId,
      unidadPeso,
      unidadDistancia,
      horaRecordatorio,
      recordatorioActivo
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
module.exports = {
  actualizarPerfil,
  actualizarSettings,
  eliminarCuenta
};
