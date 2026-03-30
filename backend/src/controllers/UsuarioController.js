const db = require('../config/db');
const RutinaService = require('../services/RutinasService');

const actualizarPerfil = async (req, res) => {
    try {
        const { userId, peso, altura, oposicionId, nuevasMarcas } = req.body;

        if (!userId || !peso || !altura || !nuevasMarcas) {
            return res.status(400).json({ 
                ok: false, 
                msg: "Faltan datos físicos o marcas para actualizar el perfil" 
            });
        }

        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para modificar el perfil de otro usuario" });
        }

        const imc = (peso / ((altura / 100) ** 2)).toFixed(2);
        await db.query(
            'UPDATE usuarios SET peso = ?, altura = ?, imc = ? WHERE id_usuario = ?',
            [peso, altura, imc, userId]
        );

        for (const marca of nuevasMarcas) {
            await db.query(
                `INSERT INTO marcas_perfil (usuarios_id_usuario, pruebas_oficiales_id_pruebas_oficiales, valord_record, fecha_logro)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE valord_record = VALUES(valord_record), fecha_logro = NOW()`,
                [userId, marca.id_prueba, marca.valor]
            );
        }

        const resultadoNivel = await RutinaService.calcularNotaYNivel(userId, oposicionId);

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
        res.status(500).json({ ok: false, msg: "Error al actualizar el perfil" });
    }
};

const actualizarSettings = async (req, res) => {
    try {
        const { userId, unidadPeso, unidadDistancia } = req.body;

        if (!userId || !unidadPeso || !unidadDistancia) {
            return res.status(400).json({ ok: false, msg: "Faltan preferencias de unidades" });
        }

        if (parseInt(userId) !== req.usuario.id) {
            return res.status(403).json({ ok: false, msg: "No tienes permiso para modificar ajustes de otro usuario" });
        }

        await db.query(
            'UPDATE settings SET unidad_peso = ?, unidad_distancia = ? WHERE usuarios_id_usuario = ?',
            [unidadPeso, unidadDistancia, userId]
        );
        res.status(200).json({ ok: true, msg: "Ajustes guardados" });
    } catch (error) {
        console.error("Error en actualizarSettings:", error.message);
        res.status(500).json({ ok: false, msg: "Error al guardar los ajustes" });
    }
};

module.exports = { actualizarPerfil, actualizarSettings };
