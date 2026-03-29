// Importamos el service que ya tenemos y que hace el trabajo en la BBDD
const AuthService= require('../services/AuthService');
const jwt =require('jsonwebtoken'); //Importacion de la libreria jwt
/*
CONTROLADOR DEL REGISTRO:
recibe la petición del movil, da la orden y responde
(Guarda el usuario con todos los datos seleccionados o rellenados en los formularios/ campos)
*/
const registrar= async(req, res)=>{
    // nustro try-catch actua como una red de seguridad para que si AuthService lanza un error, el servidor no se caiga y de una respuesta lógica al usuario
    try{
        //req.body es el paquete de datos o "solicitud" por parte del usuario, que llega desde el móvil
        const userData=req.body;

        // Evita que el servidor trabaje si el móvil no envía lo básico.
        if (!userData.email || !userData.password) {
            return res.status(400).json({
                ok: false,
                msg: 'Faltan campos obligatorios (email o contraseña)'
            });
        }

        //Llamamos a la función registrar de AuthService
        //Usamos await porque tambien se usa en el servicio ya que se tarda un poco en hablar con la base de datos
        const resultado= await AuthService.registrar(userData);

        //Si el Service termina sin errores, enviamos un código 201 (Creado)
        //Mandamos un JSON al movil confirmando que todo fué bien
        res.status(201).json({
            ok: true,
            msg: '¡Usuario registrado, configurado y con marcas guardadas!',
            userId: resultado.userId
        });
    }catch(error){
        //Si algo sale mal (si el email ya existia o falló la conexión) se salta aquí
        //Imprimimos el error en la consola del servidor para que lo veamos
        console.error("Error en el registro:", error.message)

        // Si la base de datos dice que el email ya existe, devolvemos 409 (Conflicto)
        // en lugar de un error 500 genérico.
        if (error.message.includes("Duplicate entry") || error.message.includes("ya existe")) {
            return res.status(409).json({
                ok: false,
                msg: 'Este correo electrónico ya está registrado'
            });
        }

        // Luego respondemos al movil con un código 500 (error del servidor)
        // Le enviamos un mensaje de error para que la app sepa que falló

        res.status(500).json({
            ok: false,
            msg: 'Error en el proceso de registro',
            error: error.message
        });
    }
    

};

const login =async(req,res)=>{
    try {
        //De la petición nos quedamos solamente con el email y la pass
        const{email,password}=req.body;

        //VALIDACIÓN DE CAMPOS VACÍOS
        if (!email || !password) {
            return res.status(400).json({ ok: false, msg: "Email y contraseña requeridos" });
        }

        const usuario= await AuthService.login(email,password);// Se lo enviamos al service para que lo envie a la base de datos y lo compruebe. Nos devolvera todos los datos de ese usuario para esas credenciales
        
        //Generamos el Token de seguridad: código cifrado que dura 24 horas:
        if (!process.env.JWT_SECRET) {
            return res.status(500).json({ ok: false, msg: 'Error de configuración del servidor: JWT_SECRET no definido.' });
        }
        const token= jwt.sign(
            {id: usuario.id_usuario, email:usuario.email},
            process.env.JWT_SECRET, 
            { expiresIn: '24h' }

        )
        //Si el login es correcto me devolverá un 200 (OK)
        res.status(200).json({
            ok: true,
            user: usuario, // enviamos todos los datos del usuario para que la app los use
            token: token
        });
        
    } catch (error) {
        // si el email no existe o la clave esta mal, capturamos el error para que la pp no se rompa
        // respondemos con un 401 (No autorizado)
        res.status(401).json({
            ok: false,
            msg: "Credenciales incorrectas"
        });
    }
};

const loginConGoogle = async(req, res) => {
    try {
        const { googleToken, email, nombre } = req.body;

        if (!googleToken || !email) {
            return res.status(400).json({ ok: false, msg: "Token de Google y email son requeridos" });
        }

        const usuario = await AuthService.loginConGoogle(googleToken, email, nombre || 'Usuario Google');

        // Generamos el token JWT
        if (!process.env.JWT_SECRET) {
            return res.status(500).json({ ok: false, msg: 'Error de configuración del servidor: JWT_SECRET no definido.' });
        }
        const token = jwt.sign(
            { id: usuario.id_usuario, email: usuario.email },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.status(200).json({
            ok: true,
            user: usuario,
            token: token
        });
    } catch (error) {
        console.error("Error en loginConGoogle:", error.message);
        res.status(500).json({
            ok: false,
            msg: "Error al autenticar con Google",
            error: error.message
        });
    }
};

//Exportamos las funciones para que su correspondiente archivo de routes pueda utilizarlas:
module.exports= {registrar,login,loginConGoogle};

