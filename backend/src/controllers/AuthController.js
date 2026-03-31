const AuthService= require('../services/AuthService');
const jwt =require('jsonwebtoken');

const registrar= async(req, res)=>{
    try{
        const userData=req.body;

        if (!userData.email || !userData.password) {
            return res.status(400).json({
                ok: false,
                msg: 'Faltan campos obligatorios (email o contraseña)'
            });
        }

        const resultado= await AuthService.registrar(userData);

        if (!process.env.JWT_SECRET) {
            return res.status(500).json({ ok: false, msg: 'Error de configuración del servidor' });
        }
        const token = jwt.sign(
            { id: resultado.userId, email: userData.email },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.status(201).json({
            ok: true,
            msg: '¡Registro completado con éxito!',
            userId: resultado.userId,
            token: token,
            user: resultado.user
        });
    }catch(error){
        console.error("Error en el registro:", error.message)

        if (error.message.includes("Duplicate entry") || error.message.includes("ya existe")) {
            return res.status(409).json({
                ok: false,
                msg: 'Este correo electrónico ya está registrado'
            });
        }

        res.status(500).json({
            ok: false,
            msg: 'Error en el proceso de registro'
        });
    }
    
};

const login =async(req,res)=>{
    try {
        const{email,password}=req.body;

        if (!email || !password) {
            return res.status(400).json({ ok: false, msg: "Email y contraseña requeridos" });
        }

        const usuario= await AuthService.login(email,password);
        
        if (!process.env.JWT_SECRET) {
            return res.status(500).json({ ok: false, msg: 'Error de configuración del servidor: JWT_SECRET no definido.' });
        }
        const token= jwt.sign(
            {id: usuario.id_usuario, email:usuario.email},
            process.env.JWT_SECRET, 
            { expiresIn: '24h' }

        )
        res.status(200).json({
            ok: true,
            user: usuario,
            token: token
        });
        
    } catch (error) {
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
            userId: usuario.id_usuario,
            token: token
        });
    } catch (error) {
        console.error("Error en loginConGoogle:", error.message);
        if (error.code === 'USUARIO_NO_REGISTRADO') {
            return res.status(404).json({
                ok: false,
                msg: error.message
            });
        }
        res.status(500).json({
            ok: false,
            msg: "Error al autenticar con Google"
        });
    }
};

const registrarConGoogle = async(req, res) => {
    try {
        const { googleToken, email, nombre } = req.body;

        if (!googleToken || !email) {
            return res.status(400).json({ ok: false, msg: "Token de Google y email son requeridos" });
        }

        const usuario = await AuthService.registrarConGoogle(googleToken, email, nombre || 'Usuario Google');

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

module.exports= {registrar,login,loginConGoogle,registrarConGoogle};
