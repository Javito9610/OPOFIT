
//Cargamos variables de entorno desde .env
//dotenv lee el fichero .env y pone las variables en process.env
require('dotenv').config();

//Importamos express
const express = require('express');
//Importamos cors para permitir peticiones desde otro origen. Ejemplo: El fronted servido en el mismo servidor o distinto
const cors = require('cors');

//creamos la aplicación express (Objeto central)(EL SERVIDOR)
const app = express();

//Puerto donde se escuchará al servidor. Primero intentara con la variable de entorno y sino con con el puerto 3000
const port = process.env.PORT || 3000;

/*
Midlewares globales:
-app.use(cors()): permite que peticiones desde el navegador a este servidor no sean bloqueadas por la política de cors.
-app.use(express.json()): analiza cuerpos json de las peticiones y los pone en req.body.
-app.use(express.static('public')): sirve archivos estaticos de la carpeta public (CSS,JS...)
*/
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

/* 
Rutas:
Aquí se montan los routers que definiremos en /src/routes
Nota: usaremos los prefijos '/api/...' para diferenciar la api del contenido estático.
*/

/*Los entrutadores no se creanuno por cada tabla, sino uno por cada funcionalidad. porque si tuvieramos infinitud de tablas, tendriamos que hacer muchisimos enrutadores*/
app.use('/api/auth', require('./src/routes/auth.routes')); //Abarca las tablas user y oposicion. Para el registro.
app.use('/api/user', require('./src/routes/user.routes')); //Abarca las tablas usuarios, settings y marcas_perfil. donde se cambian las marcas del atleta y las unidades de medida
app.use('/api/oposiciones', require('./src/routes/oposiciones.routes')); //Abarca las tablas oposiciones, marcas_oficiales y noticias
app.use('/api/rutinas', require('./src/routes/rutinas.routes')); //Abarca las tablas rutinas_opo, detalle_rutina_opo, ejercicios y requisitos_nivel.m Cruza requisutos_nivel y rutina_opo, para en funcion de los requisitos y las marcas del perfil pueda ofrecer una rutina especifica. Ejercicios están contenidos en rutinas
app.use('/api/rutinas_pers', require('./src/routes/rutinas_pers.routes'));//Abarca las tablas rutinas_pers,detalle_rutina_pers y ejercicios. Para gestionar la funcionalidad de entrenamiento libre.
app.use('/api/historial', require('./src/routes/historial.routes')); //Abarca las tablas historial_sesiones y registro_resultados. Para que cada vez que el usuario termine el entreno, registre la fecha y las marcas conseguidas y pueda consultarlas

/*Para levantar el servidor y aceptar conexiones en el puerto indicado*/
app.listen(port,()=>{
    console.log(`Servidor ejecutandose en http://localhost:${port}`);
});



