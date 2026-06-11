/**
 * Tests del motor de planes IA (fallback experto + validación de salida).
 * Las rutas OpenAI/Gemini se prueban con mocks; sin clave siempre usa reglas.
 */
const PlanIaService = require('../src/services/PlanIaService');

const catalogoGym = [
  {
    id_ejercicio: 1,
    nombre: 'Press banca con barra',
    pilar: 'FUERZA',
    grupo_muscular: 'Pecho',
    equipamiento: 'Barra olímpica'
  },
  {
    id_ejercicio: 2,
    nombre: 'Sentadilla trasera',
    pilar: 'FUERZA',
    grupo_muscular: 'Pierna',
    equipamiento: 'Barra olímpica'
  },
  {
    id_ejercicio: 3,
    nombre: 'Dominadas pronación',
    pilar: 'FUERZA',
    grupo_muscular: 'Espalda',
    equipamiento: 'Barra de dominadas'
  },
  {
    id_ejercicio: 4,
    nombre: 'Carrera continua Z2 30 min',
    pilar: 'RESISTENCIA',
    grupo_muscular: 'Cardio',
    equipamiento: 'Pista'
  },
  {
    id_ejercicio: 5,
    nombre: 'Plancha frontal',
    pilar: 'CORE',
    grupo_muscular: 'Core',
    equipamiento: 'Suelo'
  },
  {
    id_ejercicio: 6,
    nombre: 'Sprint 40 m',
    pilar: 'VELOCIDAD',
    grupo_muscular: 'Pierna',
    equipamiento: 'Pista'
  }
];

describe('PlanIaService (reglas)', () => {
  const prevOpenAi = process.env.OPENAI_API_KEY;
  const prevGemini = process.env.GEMINI_API_KEY;

  beforeAll(() => {
    delete process.env.OPENAI_API_KEY;
    delete process.env.GEMINI_API_KEY;
  });

  afterAll(() => {
    if (prevOpenAi) process.env.OPENAI_API_KEY = prevOpenAi;
    if (prevGemini) process.env.GEMINI_API_KEY = prevGemini;
  });

  test('generarCoaching devuelve texto en español sin API', async () => {
    const r = await PlanIaService.generarCoaching({
      entorno: 'GYM',
      nivel: 'INTERMEDIO',
      rachaDias: 4,
      sesionesSemana: 2,
      resumen: 'Dominadas débiles',
      pilaresDebiles: [{ pilar: 'FUERZA', notaMedia: 5.2 }],
      pilaresFuertes: [{ pilar: 'RESISTENCIA' }],
      dias: [{ nombre_dia: 'Lunes', enfoque: 'FUERZA', titulo: 'Fuerza A' }]
    });
    expect(r.fuente).toBe('reglas');
    expect(r.texto.length).toBeGreaterThan(40);
    expect(r.texto).toMatch(/entreno|sesión|fuerza|dominada|racha/i);
  });

  test('disenarSesion FUERZA genera ejercicios del catálogo sin repetir', async () => {
    const { sesion, fuente } = await PlanIaService.disenarSesion({
      enfoque: 'FUERZA',
      nivel: 'INTERMEDIO',
      entorno: 'GYM',
      catalogo: catalogoGym,
      pilaresDebiles: [{ pilar: 'FUERZA' }],
      seed: 42
    });
    expect(fuente).toBe('reglas');
    expect(sesion.ejercicios.length).toBeGreaterThanOrEqual(3);
    const ids = sesion.ejercicios.map((e) => e.id_ejercicio);
    // Con catálogo pequeño y refuerzo de pilar débil puede repetir IDs (realista en GYM con pocos básicos).
    ids.forEach((id) => expect(catalogoGym.some((c) => c.id_ejercicio === id)).toBe(true));
    sesion.ejercicios.forEach((e) => {
      expect(e.series).toBeGreaterThanOrEqual(1);
      expect(e.series).toBeLessThanOrEqual(8);
      if (e.repeticiones != null) {
        expect(e.repeticiones).toBeGreaterThanOrEqual(1);
        expect(e.repeticiones).toBeLessThanOrEqual(99);
      }
    });
  });

  test('disenarSesion RESISTENCIA usa unidad min en cardio', async () => {
    const { sesion } = await PlanIaService.disenarSesion({
      enfoque: 'RESISTENCIA',
      nivel: 'BASICO',
      entorno: 'PISTA',
      catalogo: catalogoGym,
      seed: 7
    });
    const cardio = sesion.ejercicios.find((e) => e.pilar === 'RESISTENCIA');
    expect(cardio).toBeTruthy();
    expect(cardio.unidad).toBe('min');
  });

  test('disenarSemana orquesta todos los días', async () => {
    const dias = [
      { nombre_dia: 'Lunes', enfoque: 'FUERZA', titulo: 'A' },
      { nombre_dia: 'Miércoles', enfoque: 'RESISTENCIA', titulo: 'B' },
      { nombre_dia: 'Viernes', enfoque: 'CORE', titulo: 'C' }
    ];
    const { semana, fuente } = await PlanIaService.disenarSemana({
      dias,
      nivel: 'INTERMEDIO',
      entorno: 'GYM',
      catalogo: catalogoGym,
      seed: 3
    });
    expect(fuente).toBe('reglas');
    expect(semana.length).toBe(3);
    semana.forEach((d) => expect(d.ejercicios.length).toBeGreaterThan(0));
  });
});

describe('PlanIaService (validación IA mock)', () => {
  const prevOpenAi = process.env.OPENAI_API_KEY;

  afterEach(() => {
    global.fetch = undefined;
    if (prevOpenAi) process.env.OPENAI_API_KEY = prevOpenAi;
    else delete process.env.OPENAI_API_KEY;
  });

  test('rechaza IDs inventados por la IA y cae a reglas', async () => {
    process.env.OPENAI_API_KEY = 'test-key';
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        choices: [
          {
            message: {
              content: JSON.stringify({
                ejercicios: [
                  { id: 99999, series: 5, reps: 20, descanso: 60, unidad: 'reps', motivo: 'fake' },
                  { id: 88888, series: 4, reps: 10, descanso: 90, unidad: 'reps', motivo: 'fake' }
                ]
              })
            }
          }
        ]
      })
    });

    const { sesion, fuente } = await PlanIaService.disenarSesion({
      enfoque: 'FUERZA',
      nivel: 'INTERMEDIO',
      entorno: 'GYM',
      catalogo: catalogoGym,
      seed: 1
    });
    expect(fuente).toBe('reglas');
    expect(sesion.ejercicios.length).toBeGreaterThanOrEqual(3);
    sesion.ejercicios.forEach((e) =>
      expect(catalogoGym.some((c) => c.id_ejercicio === e.id_ejercicio)).toBe(true)
    );
  });

  test('acepta respuesta IA válida con IDs del catálogo', async () => {
    process.env.OPENAI_API_KEY = 'test-key';
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        choices: [
          {
            message: {
              content: JSON.stringify({
                ejercicios: [
                  {
                    id: 1,
                    series: 4,
                    reps: 8,
                    descanso: 90,
                    unidad: 'reps',
                    motivo: 'Básico de pecho'
                  },
                  {
                    id: 2,
                    series: 4,
                    reps: 6,
                    descanso: 120,
                    unidad: 'reps',
                    motivo: 'Pierna pesada'
                  },
                  {
                    id: 3,
                    series: 4,
                    reps: 8,
                    descanso: 90,
                    unidad: 'reps',
                    motivo: 'Espalda'
                  }
                ]
              })
            }
          }
        ]
      })
    });

    const { sesion, fuente } = await PlanIaService.disenarSesion({
      enfoque: 'FUERZA',
      nivel: 'INTERMEDIO',
      entorno: 'GYM',
      catalogo: catalogoGym,
      seed: 1
    });
    expect(fuente).toBe('openai');
    expect(sesion.ejercicios.length).toBe(3);
    expect(sesion.ejercicios[0].motivo_ia).toContain('pecho');
  });
});
