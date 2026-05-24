# Mejoras implementadas en OpoFit

## Resumen

Se han añadido las funcionalidades recomendadas (fases 1–6). **iOS** y **wearables** quedan documentados como fase siguiente (no son un port automático).

## 1. Entrenamiento en curso (mejorado)

- Pantallas `EntrenamientosScreen` y `EntrenamientoPersonalizadoScreen` ya tenían cronómetro y registro paso a paso.
- **Nuevo:** indicador visual del paso actual (“Paso X de N”) y resaltado del ejercicio activo.
- **Backend:** eliminado el límite de un entreno por rutina y día (`ProgresoService`).

## 2. Simulacro de prueba física oficial

- **API:** `GET /api/simulacros/pruebas/:idOposicion`, `POST /api/simulacros/guardar`, `GET /api/simulacros/historial/:idOposicion`
- **App:** pantalla `SimulacroScreen` — guía prueba a prueba, cronómetro para tiempos, nota estimada con baremos de BD.
- Tablas: `simulacros`, `simulacro_pruebas` (ver migración).

## 3. Notificaciones push (FCM)

- **App:** `firebase-messaging`, `OpoFitMessagingService`, registro de token al iniciar sesión.
- **API:** `POST /api/notifications/token`
- **Admin:** envío masivo de recordatorios y noticias por oposición.
- **Cron opcional:** `NOTIFICATIONS_CRON=true` → recordatorio diario a las 8:00.

## 4. Backoffice web

- URL: `https://tu-servidor/admin/` (archivos en `backend/public/admin/`)
- CRUD: oposiciones, ejercicios, pruebas oficiales, baremos.
- Push: recordatorios y noticias por oposición.
- Requiere cabecera `X-Admin-Key` = variable `ADMIN_API_KEY`.

## 5. Ranking / perfil público

- **API:** `GET /api/ranking/:idOposicion`, `PUT /api/ranking/perfil-publico`
- **App:** `RankingScreen` + interruptor perfil público.
- Solo usuarios con `perfil_publico = 1` aparecen en el ranking.

## 6. Monetización (freemium por contenido)

**Todas las oposiciones tienen parte gratuita:**

| Gratis (cualquier oposición) | Premium |
|------------------------------|---------|
| Rutinas nivel **BÁSICO** | Rutinas **INTERMEDIO** y **AVANZADO** |
| Baremos parciales (4 filas/prueba) | Baremos completos |
| Simulacro oficial con nota | Historial de simulacros |
| Info y pruebas oficiales | Ranking ampliado |

**Oposiciones en catálogo (verificadas):** PN, Guardia Civil, Bomberos Comunidad de Madrid, Bomberos Ayuntamiento de Madrid, Ejército Tropa y Marinería (BOE 2026), Ayudante IIPP (sin prueba física deportiva). Ver `BBDD/OPOSICIONES_FUENTES.md`.
- **Prueba dev:** `PREMIUM_DEV_MODE=true` + botón en app “Activar Premium (prueba)”.
- **Producción:** conectar Google Play Billing (pendiente).

---

## Despliegue obligatorio

### 1. Migración de base de datos

Ejecuta en MySQL (Railway o local):

```bash
mysql -u USER -p mydb < BBDD/migration_v2.sql
mysql -u USER -p mydb < BBDD/migration_v3_oposiciones.sql
mysql -u USER -p mydb < BBDD/seed_oposiciones_oficiales.sql
```

**No uses** `seed_nuevas_oposiciones.sql` en producción (datos genéricos incorrectos).

Para regenerar el seed oficial tras editar pruebas:

```bash
node backend/scripts/generar-seed-oposiciones-oficial.js
```

Si tu MySQL no soporta `ADD COLUMN IF NOT EXISTS`, ejecuta los `ALTER` uno a uno omitiendo columnas que ya existan.

### 2. Variables de entorno (Railway / `.env`)

```env
ADMIN_API_KEY=tu_clave_secreta_larga
PREMIUM_DEV_MODE=true
NOTIFICATIONS_CRON=true
FIREBASE_SERVICE_ACCOUNT_JSON={...}
```

**Importante (errores HTTP 500 en Premium / Ranking / Simulacro):** el backend nuevo usa columnas y tablas que no existían en la BD antigua. Al arrancar, el servidor aplica migraciones automáticas (`DbMigrationService`). Tras desplegar en Railway, revisa los logs: debe aparecer `[migrate] Esquema comprobado OK`. Si Premium sigue fallando al activar prueba, confirma `PREMIUM_DEV_MODE=true` en Railway.

### 3. Backend

```bash
cd backend && npm install && npm start
```

### 4. App Android

- Añade en Firebase Console → Cloud Messaging (si no está).
- Compila e instala el APK; acepta permiso de notificaciones en Android 13+.

---

## Fase siguiente (no implementada)

| Funcionalidad | Enfoque recomendado |
|---------------|---------------------|
| **iOS** | Kotlin Multiplatform (UI compartida) o app Swift que consuma la misma API REST |
| **Wearables** | Health Connect (Android), Google Fit API; Apple HealthKit en iOS |

Ver detalles en el mismo documento o solicitar un plan de implementación por fases.
