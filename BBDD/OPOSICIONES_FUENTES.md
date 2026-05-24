# Catálogo de oposiciones — fuentes oficiales

Documento para comercialización y revisión legal. **Consultar siempre la convocatoria vigente** antes de publicitar “pruebas oficiales”.

## Resumen por oposición (id en BD)

| ID | Nombre en app | Pruebas físicas en simulacro | Fuente principal |
|----|---------------|------------------------------|------------------|
| 1 | Policía Nacional - Escala Básica | Sí (seed base) | Convocatoria PN / BOE |
| 2 | Guardia Civil - Acceso Libre | Sí (seed base) | Convocatoria GC / BOE |
| 3 | Bomberos - **Comunidad de Madrid** (Conductor) | 6 pruebas | **BOCM Orden 122/2022** (11 feb 2022) |
| 4 | Bomberos - **Ayuntamiento de Madrid** (Especialista) | 5 cronometradas | Bases BOAM 2025 (Ayto. Madrid) |
| 5 | Ayudante Instituciones Penitenciarias | **No** | Sin prueba física deportiva en oposición |
| 6 | Ejército - Tropa y Marinería | 4 pruebas | **BOE-A-2026-436**, Apéndice 4 |

## Bomberos: Comunidad de Madrid ≠ Ayuntamiento de Madrid

Son **procesos selectivos distintos** con pruebas distintas.

### Comunidad de Madrid (id 3)

Orden 122/2022, BOCM. Bombero Especialista **Conductor**. Pruebas y marcas mínimas eliminatorias:

| Prueba | Mínimo (H/M según tabla BOCM) |
|--------|-------------------------------|
| Natación 50 m crol | 51"40 |
| Trepa cuerda 6 m | 14"65 |
| Press 40 kg (reps en 60 s) | 19 rep |
| Carrera 60 m | 10"00 |
| Carrera 300 m | 50"00 |
| Carrera 2.000 m | 7'40"00 |

Baremos en app: tabla oficial BOCM (puntuación 0-10 interpolada para entrenamiento).

PDF: https://www.bocm.es/boletin/CM_Orden_BOCM/2022/02/11/BOCM-20220211-6.PDF

### Ayuntamiento de Madrid (id 4)

Bases **Bombero/a Especialista** (12 pruebas en convocatoria). En la app solo las **cronometradas medibles** sin instalación especial:

- 200 m, 1.500 m, 100 m acuáticos (25 m buceo + 75 m crol), trepa 6,5 m, torre 8 plantas.

**No incluidas en simulacro numérico** (apto/no apto en sede): espacios confinados, trabajo en altura, habilidades con EPI (cargas, maza, corte, puntal, pala).

Baremos Ayto. Madrid en app: **orientativos** — validar con PDF de bases antes de comercializar como “nota oficial”.

## Ejército de Tierra — Tropa y Marinería (id 6)

**BOE-A-2026-436** (8 ene 2026), Apéndice 4. Calificación oficial: **Apto / No Apto** (no nota 0-10).

| Prueba en BOE | En app | Mínimo apto (H / M) |
|---------------|--------|----------------------|
| Flexo-extensiones brazos (2 min) | Sí | ≥9 / ≥5 repeticiones |
| “Abdominales” (en BOE = **plancha isométrica**) | Sí | ≥40 s / ≥40 s |
| Circuito agilidad-velocidad | Sí | ≤15,4 s / ≤17,1 s |
| Carrera 2.000 m | Sí | ≤11'54" / ≤12'58" |

**No son** las pruebas antiguas (flexiones + abdominales 2 min + salto vertical) que aparecían en versiones genéricas del seed.

PDF: https://www.boe.es/boe/dias/2026/01/08/pdfs/BOE-A-2026-436.pdf

## Instituciones Penitenciarias (id 5)

El acceso de **Ayudante** se resuelve por oposición teórica y **reconocimiento médico**. No hay bloque de pruebas físicas deportivas con baremos como en PN o bomberos. La app **no ofrece simulacro** para esta oposición.

## Despliegue

```bash
mysql -u USER -p mydb < BBDD/migration_v3_oposiciones.sql
mysql -u USER -p mydb < BBDD/seed_oposiciones_oficiales.sql
```

## Aviso comercial

Incluir en la ficha de la tienda (Google Play):

> Las pruebas y baremos se basan en convocatorias publicadas (BOE, BOCM, ayuntamientos). Cada territorio puede cambiar requisitos. Comprueba siempre la convocatoria oficial de tu plaza.
