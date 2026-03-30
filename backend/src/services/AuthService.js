const db = require('../config/db');
const bcrypt = require('bcryptjs');
const { OAuth2Client } = require('google-auth-library');

class AuthService{
    static async registrar(userData){
       const {nombre, email, password, genero, peso, altura, oposiciones_id_oposicion, marcasIniciales}=userData;
       const connection= await db.getConnection();
       try{
            await connection.beginTransaction();

            const hashedPassword = await bcrypt.hash(password, 10);

            const alturaMetros = altura/100;
            const imc = (peso / (alturaMetros*alturaMetros)).toFixed(2);

            const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro, oposiciones_id_oposicion) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)`;
            const [userResult] = await connection.query(sqlUsuario, [
            nombre, email, hashedPassword, genero, peso, altura, imc, oposiciones_id_oposicion   
            ]);
            const userId= userResult.insertId;

            await connection.query(
                'INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]
            );

            if (marcasIniciales && marcasIniciales.length>0){
                const sqlMarcas = `INSERT INTO marcas_perfil (valord_record, fecha_logro, pruebas_oficiales_id_pruebas_oficiales, usuarios_id_usuario) VALUES (?, NOW(), ?, ?)`;
                for (const marca of marcasIniciales){
                    await connection.query(sqlMarcas, [marca.valor, marca.id_prueba, userId]);    
                }

            }

            await connection.commit();
            return{success: true, userId};
            
        }catch(error){
            await connection.rollback();
            throw error;
        }finally{
            connection.release();
        }
    }
    static async login(email, password){
        const [rows]= await db.query('SELECT * FROM usuarios WHERE email =?', [email]);
        if (rows.length===0) throw new Error('Usuario no encontrado');
        const usuario = rows[0];
        const esValida= await bcrypt.compare(password, usuario.password);
        if (!esValida) throw new Error('Contraseña incorrecta');
        delete usuario.password
        return usuario;
    }

    static async loginConGoogle(googleToken, email, nombre){
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

        const [rows] = await db.query('SELECT * FROM usuarios WHERE email = ?', [email]);
        
        if (rows.length > 0) {
            const usuario = rows[0];
            delete usuario.password;
            return usuario;
        }
        
        const connection = await db.getConnection();
        try {
            await connection.beginTransaction();
            
            const crypto = require('crypto');
            const randomPassword = crypto.randomBytes(32).toString('hex');
            const hashedPassword = await bcrypt.hash(randomPassword, 10);
            
            const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro) VALUES (?, ?, ?, 'HOMBRE', 0, 0, 0, NOW())`;
            const [userResult] = await connection.query(sqlUsuario, [nombre, email, hashedPassword]);
            const userId = userResult.insertId;
            
            await connection.query(
                'INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]
            );
            
            await connection.commit();
            
            const [nuevoUsuario] = await db.query('SELECT * FROM usuarios WHERE id_usuario = ?', [userId]);
            delete nuevoUsuario[0].password;
            return nuevoUsuario[0];
        } catch(error) {
            await connection.rollback();
            throw error;
        } finally {
            connection.release();
        }
    }
}
module.exports= AuthService;
