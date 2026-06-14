const mysql = require('mysql2/promise');
require('dotenv').config();

// Pool de producción:
//  - connectionLimit configurable por env (default 15: aguanta picos sin
//    sobrecargar la BBDD; subir a 25-30 si el tráfico crece).
//  - connectTimeout 10s: si la BBDD no responde rápido en TCP, fallamos
//    pronto en vez de colgar la request.
//  - enableKeepAlive + keepAliveInitialDelay: evita que el proxy/balanceador
//    corte conexiones inactivas (síntoma típico en Railway / Cloud SQL:
//    ETIMEDOUT tras periodos sin tráfico).
//  - dateStrings: previene timezone drift devolviendo fechas como strings,
//    interpretadas explícitamente por cada servicio (España).
//  - ssl: si DB_SSL=true (típico Cloud SQL / Railway producción), activa TLS.
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  charset: 'utf8mb4_unicode_ci',
  waitForConnections: true,
  connectionLimit: Number(process.env.DB_POOL_SIZE || 15),
  queueLimit: 0,
  connectTimeout: 10000,
  enableKeepAlive: true,
  keepAliveInitialDelay: 10000,
  dateStrings: false,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : undefined
});

module.exports = pool;
