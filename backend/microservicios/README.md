# OpoFit Microservicios

Módulos independientes que se ejecutan en background o como scripts standalone.

## Componentes

- **NoticiasMicro** (`noticias-micro.js`): refresca cache de noticias RSS por oposición.
  Invalida cache → re-fetcha → envía push de convocatorias/plazos nuevos.
- **BaremoCheckMicro** (`baremo-check-micro.js`): comprueba diariamente que los baremos
  en BD no se han desviado de la tabla oficial. Emite warnings en log si encuentra diff.

## Ejecutar

In-process (recomendado en Railway):

```bash
NOTIFICATIONS_CRON=true npm start
```

El cron interno (`app.js`) lanza ambos microservicios:
- Noticias: cada 6 horas
- Baremos: cada 24 horas (a las 04:00 UTC)

Standalone (CLI / GitHub Action / cron del sistema):

```bash
node microservicios/run.js noticias
node microservicios/run.js baremos
node microservicios/run.js all
```

## Variables de entorno

```env
NOTIFICATIONS_CRON=true       # activa el cron interno
BAREMO_CHECK_CRON=0 4 * * *   # opcional, override del cron de baremos
NOTICIAS_CRON=0 */6 * * *     # opcional, override del cron de noticias
```
