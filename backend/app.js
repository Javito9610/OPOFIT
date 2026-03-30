require('dotenv').config();

const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');

const app = express();

const port = process.env.PORT || 3000;

const globalLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100,
    message: { ok: false, msg: 'Demasiadas peticiones. Inténtalo de nuevo más tarde.' }
});

const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    message: { ok: false, msg: 'Demasiados intentos de autenticación. Espera unos minutos.' }
});

app.use(cors());
app.use(express.json());
app.use(express.static('public'));
app.use(globalLimiter);

app.use('/api/auth', authLimiter, require('./src/routes/AuthRoute'));
app.use('/api/user', require('./src/routes/UsuarioRoute'));
app.use('/api/oposiciones', require('./src/routes/OposicionRoute'));
app.use('/api/rutinas', require('./src/routes/RutinaRoute'));
app.use('/api/rutinas-pers', require('./src/routes/RutinaPersRoute'));
app.use('/api/historial', require('./src/routes/ProgresoRoute'));
app.use('/api/info', require('./src/routes/InfoPruebasRoute'));

app.listen(port,()=>{
    console.log(`Servidor ejecutandose en http://localhost:${port}`);
});
