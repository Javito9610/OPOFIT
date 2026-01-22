/*
CONFIGURACION Y CONEXION CON LA BASE DE DATOS:
Usamos mysql2/promise para trabajar con async/await
Crear un pool es algo muy recomendado en produccion/desarrollo porque:
-Reusa conexiones (mejor rendimiento).
-Controla limite de conexiones simultaneas
-Evita crear y destruir conexiones por cada consulta
Basicamente lo que hace es optimizar las conexiones a la BBDD
*/ 
const mysql= require('mysql2/promise'); //version con promesas

// Nos aseguramos de que las variables de entorno esten
require('dotenv').config();

//creamos el pool con parametros sacados de .env
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
})
/*Exportaremos el pool. En los servicios haremos:
const db=require('../config/db');
const[rows]=await db.query('SELECT...')
*/
module.exports=pool;


