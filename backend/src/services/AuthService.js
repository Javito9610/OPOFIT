// Vamos a importar db y bcrypt para hashear la password y generar la conexion con la base de datos
const db = require('../config/db');
const bcrypt = require('bcryptjs');
const { OAuth2Client } = require('google-auth-library');

// Comenzamos con la lógica de negocio.

class AuthService{
    static async registrar(userData){
       //1 RECIBIMOS LOS DATOS:
       //El movil nos envía un paquete con todo. Las 'marcasIniciales' vienen como una lista
       //La lista es de este tipo: [{id_prueba: 5, valor: 4.05}, {id_prueba: 8, valor: 15}]
       //Desestructuramos los datos para poder manipularlos más adelante
       const {nombre, email, password, genero, peso, altura, oposiciones_id_oposicion, marcasIniciales}=userData;
       //2 PEDIMOS UNA CONEXIÓN:
       // Vamos a tocar 3 tablas: usuarios, settings y marcas_perfil
       //Pedimos una conexión exclusiva al pool.
       const connection= await db.getConnection();
       try{
            // EMPEZAMOS LA TRANSACCIÓN:
            //Si los datos se quedan a la mitad, no se guardará nada.
            await connection.beginTransaction();

            //4 SEGURIDAD:
            //Encriptamos la clave para que nadie (ni el administrador) pueda verla en la base de datos.
            const hashedPassword = await bcrypt.hash(password, 10);

            //5 CÁLCULO FÍSICO:
            //Calculamos el IMC del usuario antes de guardarlo.
            const alturaMetros = altura/100;
            const imc = (peso / (alturaMetros*alturaMetros)).toFixed(2);

            //6 GUARDAR USUARIO (tabla 'usuarios'):
            // Aqui ya incluimos el 'id_oposicion'. El usuario ya queda "atado" a su oposición.
            const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro, oposiciones_id_oposicion) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)`;
            const [userResult] = await connection.query(sqlUsuario, [
            nombre, email, hashedPassword, genero, peso, altura, imc, oposiciones_id_oposicion   
            ]);
            // Obtenemos el ID que la base de datos ha dado automáticamente a este nuevo usuario.
            const userId= userResult.insertId;

            //7 GUARDAR CONFIGURACIÓN (tabla 'settings'):
            // Creamos sus ajustes por defecto (kg y km) vinculados a su nuevo ID
            await connection.query(
                'INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]
            );

            //8 GUARDAR MARCAS (Tabla 'marcas_perfil'):
            //Si el usuario ha rellenado marcas en el registro, las guardamos una por una.
            if (marcasIniciales && marcasIniciales.length>0){
                const sqlMarcas = `INSERT INTO marcas_perfil (valord_record, fecha_logro, pruebas_oficiales_id_pruebas_oficiales, usuarios_id_usuario) VALUES (?, NOW(), ?, ?)`;
                for (const marca of marcasIniciales){
                    // Aqui se guarda el valor y el ID de cada prueba para cada usuario que se registra
                    await connection.query(sqlMarcas, [marca.valor, marca.id_prueba, userId]);    
                }

            }

            //9 TODO OK:  Si hemos llegado hasta aqui sin errores, confirmamos a la base de datos que guarde todo
            await connection.commit();
            return{success: true, userId};
            
        }catch(error){
            //10 ERRORES: Si falla cualquier cosa o algo como el email ya existia, deshacemos todo para no dejar datos basura
            await connection.rollback();
            throw error;
        }finally{
            //11 LIBERAR: Soltamos conexión para que el servidor no colapse
            connection.release();
        }
    }
    //LOGIN: Solo comprueba si el email y la password coinciden
    static async login(email, password){
        const [rows]= await db.query('SELECT * FROM usuarios WHERE email =?', [email]);
        if (rows.length===0) throw new Error('Usuario no encontrado');
        const usuario = rows[0];
        const esValida= await bcrypt.compare(password, usuario.password);
        if (!esValida) throw new Error('Contraseña incorrecta');
        delete usuario.password //por seguridad no devolvemos la contraseña al movil
        return usuario; // Devolvemos el usuario completo
    }

    //LOGIN CON GOOGLE: Busca o crea un usuario autenticado con Google
    static async loginConGoogle(googleToken, email, nombre){
        // Verificamos el token de Google con la librería oficial
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

        // Validamos que el email del token coincida con el email enviado
        if (payload.email !== email) {
            throw new Error('El email del token no coincide con el email proporcionado');
        }

        // Buscamos si el usuario ya existe por email
        const [rows] = await db.query('SELECT * FROM usuarios WHERE email = ?', [email]);
        
        if (rows.length > 0) {
            // El usuario ya existe, lo devolvemos
            const usuario = rows[0];
            delete usuario.password;
            return usuario;
        }
        
        // Si no existe, creamos uno nuevo con datos mínimos
        const connection = await db.getConnection();
        try {
            await connection.beginTransaction();
            
            // Generamos una contraseña aleatoria segura (el usuario usa Google, no la necesita)
            const crypto = require('crypto');
            const randomPassword = crypto.randomBytes(32).toString('hex');
            const hashedPassword = await bcrypt.hash(randomPassword, 10);
            
            // Nota: El género se establece por defecto como 'HOMBRE'. El usuario debe actualizarlo en su perfil.
            // El campo genero es NOT NULL en la BBDD, por lo que se requiere un valor inicial.
            const sqlUsuario = `INSERT INTO usuarios (nombre, email, password, genero, peso, altura, imc, fecha_registro) VALUES (?, ?, ?, 'HOMBRE', 0, 0, 0, NOW())`;
            const [userResult] = await connection.query(sqlUsuario, [nombre, email, hashedPassword]);
            const userId = userResult.insertId;
            
            // Creamos settings por defecto
            await connection.query(
                'INSERT INTO settings(unidad_peso, unidad_distancia, usuarios_id_usuario) VALUES(?, ?, ?)', ['kg', 'km', userId]
            );
            
            await connection.commit();
            
            // Devolvemos el usuario recién creado
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