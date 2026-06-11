jest.mock('../src/config/db', () => ({ query: jest.fn() }));
jest.mock('../src/services/PlanIaService', () => ({
  generarCoaching: jest.fn().mockResolvedValue({ texto: 'Coaching test', fuente: 'reglas' })
}));

const db = require('../src/config/db');
const PlanGeneradorService = require('../src/services/PlanGeneradorService');

const planBase = {
  id_plan: 1,
  dias_por_semana: 2,
  dia_hoy: 1,
  semana: [
    {
      id_plan_dia: 10,
      dia_semana: 1,
      nombre_dia: 'Lunes',
      enfoque: 'FUERZA',
      titulo: 'Fuerza A',
      ejercicios: [
        {
          id_ejercicio: 1,
          nombre: 'Press banca con barra',
          pilar: 'FUERZA',
          grupo_muscular: 'Pecho',
          series: 4,
          repeticiones: 8,
          descanso: 90
        },
        {
          id_ejercicio: 2,
          nombre: 'Sentadilla trasera',
          pilar: 'FUERZA',
          grupo_muscular: 'Pierna',
          series: 4,
          repeticiones: 6,
          descanso: 120
        }
      ],
      es_hoy: true,
      completada: false
    },
    {
      id_plan_dia: 11,
      dia_semana: 3,
      nombre_dia: 'Miércoles',
      enfoque: 'RESISTENCIA',
      titulo: 'Cardio',
      ejercicios: [
        {
          id_ejercicio: 3,
          nombre: 'Carrera continua Z2 30 min',
          pilar: 'RESISTENCIA',
          series: 1,
          repeticiones: 30,
          descanso: 0
        }
      ],
      es_hoy: false,
      completada: false
    }
  ],
  personalizacion: { resumen: 'Plan adaptado', pilares_debiles: [] }
};

const catalogoCasa = [
  {
    id_ejercicio: 101,
    nombre: 'Flexiones estándar',
    pilar: 'FUERZA',
    grupo_muscular: 'Pecho',
    equipamiento: 'Suelo',
    entornos: 'CASA,CALISTENIA',
    tipo_ilustracion: 'PUSH'
  },
  {
    id_ejercicio: 102,
    nombre: 'Sentadilla con mochila',
    pilar: 'FUERZA',
    grupo_muscular: 'Pierna',
    equipamiento: 'Mochila',
    entornos: 'CASA',
    tipo_ilustracion: 'SQUAT'
  },
  {
    id_ejercicio: 103,
    nombre: 'Carrera en el sitio Z2 15 min',
    pilar: 'RESISTENCIA',
    grupo_muscular: 'Cardio',
    equipamiento: 'Suelo',
    entornos: 'CASA',
    tipo_ilustracion: 'RUN'
  },
  {
    id_ejercicio: 104,
    nombre: 'Flexiones inclinadas (mesa)',
    pilar: 'FUERZA',
    grupo_muscular: 'Pecho',
    equipamiento: 'Mesa',
    entornos: 'CASA',
    tipo_ilustracion: 'PUSH'
  },
  {
    id_ejercicio: 105,
    nombre: 'Zancadas en el sitio',
    pilar: 'FUERZA',
    grupo_muscular: 'Pierna',
    equipamiento: 'Suelo',
    entornos: 'CASA',
    tipo_ilustracion: 'SQUAT'
  },
  {
    id_ejercicio: 106,
    nombre: 'Burpees',
    pilar: 'RESISTENCIA',
    grupo_muscular: 'Cardio',
    equipamiento: 'Suelo',
    entornos: 'CASA',
    tipo_ilustracion: 'RUN'
  },
  {
    id_ejercicio: 107,
    nombre: 'Puente de glúteo',
    pilar: 'FUERZA',
    grupo_muscular: 'Glúteo',
    equipamiento: 'Suelo',
    entornos: 'CASA',
    tipo_ilustracion: 'SQUAT'
  },
  {
    id_ejercicio: 108,
    nombre: 'Mountain climbers',
    pilar: 'CORE',
    grupo_muscular: 'Core',
    equipamiento: 'Suelo',
    entornos: 'CASA',
    tipo_ilustracion: 'PLANK'
  }
];

describe('PlanGeneradorService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('generarSemana sustituye ejercicios para entorno CASA', async () => {
    db.query.mockResolvedValueOnce([catalogoCasa]);
    const { plan, sustituciones } = await PlanGeneradorService.generarSemana(
      planBase,
      1,
      'CASA',
      2
    );
    expect(sustituciones).toBeGreaterThan(0);
    const nombres = plan.semana.flatMap((d) => d.ejercicios.map((e) => e.nombre));
    expect(nombres.some((n) => n.includes('Flexión') || n.includes('mochila') || n.includes('sitio'))).toBe(
      true
    );
    const conIlust = plan.semana.flatMap((d) => d.ejercicios).find((e) => e.tipo_ilustracion);
    expect(conIlust?.tipo_ilustracion).toBeTruthy();
  });

  test('generarSemana no sustituye con MIXTO', async () => {
    const { plan, sustituciones } = await PlanGeneradorService.generarSemana(planBase, 1, 'MIXTO', 0);
    expect(sustituciones).toBe(0);
    expect(plan.semana[0].ejercicios[0].nombre).toBe('Press banca con barra');
  });

  test('listarEntornos devuelve opciones sin MIXTO', () => {
    const lista = PlanGeneradorService.listarEntornos();
    expect(lista.length).toBeGreaterThanOrEqual(5);
    expect(lista.find((e) => e.id === 'CASA')).toBeTruthy();
    expect(lista.find((e) => e.id === 'MIXTO')).toBeFalsy();
  });

  test('combinarPlanConCache mantiene datos frescos del plan actual', () => {
    const actual = {
      dia_hoy: 2,
      personalizacion: { resumen: 'Actualizado', ajustes_aplicados: 2 },
      semana: [
        {
          id_plan_dia: 10,
          dia_semana: 1,
          es_hoy: false,
          completada: false,
          ejercicios: [{ nombre: 'Press banca', series: 4 }]
        },
        {
          id_plan_dia: 11,
          dia_semana: 2,
          es_hoy: true,
          completada: true,
          ejercicios: [{ nombre: 'Dominadas', series: 3 }]
        }
      ]
    };
    const cache = {
      semana: [
        { id_plan_dia: 10, ejercicios: [{ nombre: 'Flexiones', series: 4, tipo_ilustracion: 'PUSH' }] },
        { id_plan_dia: 11, ejercicios: [{ nombre: 'Remo mochila', series: 3, tipo_ilustracion: 'PULL' }] }
      ]
    };
    const merged = PlanGeneradorService.combinarPlanConCache(actual, cache);
    expect(merged.personalizacion.resumen).toBe('Actualizado');
    expect(merged.semana[1].completada).toBe(true);
    expect(merged.semana[0].ejercicios[0].nombre).toBe('Flexiones');
    expect(merged.sesion_hoy?.id_plan_dia).toBe(11);
  });

  test('seed distinto produce variación', async () => {
    db.query.mockResolvedValue([catalogoCasa]);
    const r1 = await PlanGeneradorService.generarSemana(planBase, 1, 'CASA', 1);
    const r2 = await PlanGeneradorService.generarSemana(planBase, 1, 'CASA', 99);
    const n1 = r1.plan.semana[0].ejercicios.map((e) => e.nombre).join('|');
    const n2 = r2.plan.semana[0].ejercicios.map((e) => e.nombre).join('|');
    expect(n1).not.toBe(n2);
  });

  test('resumenDiaDesdeEjercicios actualiza título y descripción', () => {
    const dia = { titulo: 'Viejo', descripcion: 'Vieja desc' };
    const ej = [{ nombre: 'Carrera Z2 30 min' }, { nombre: 'Movilidad cadera' }];
    const r = PlanGeneradorService.resumenDiaDesdeEjercicios(dia, ej);
    // El título indica que hay MÁS ejercicios además del primero — antes
    // "Carrera Z2 30 min" a secas hacía creer que la sesión era de 1 ejercicio.
    expect(r.titulo).toBe('Carrera Z2 30 min +1 más');
    expect(r.descripcion).toContain('Movilidad');
  });

  test('resumenDiaDesdeEjercicios sin "+N" cuando hay un único ejercicio', () => {
    const dia = { titulo: 'Viejo', descripcion: 'Vieja' };
    const r = PlanGeneradorService.resumenDiaDesdeEjercicios(dia, [{ nombre: 'HIIT 4x4' }]);
    expect(r.titulo).toBe('HIIT 4x4');
  });

  test('generarSemana con soloDiaId solo cambia un día', async () => {
    db.query.mockResolvedValue([catalogoCasa]);
    const { plan, sustituciones } = await PlanGeneradorService.generarSemana(
      planBase,
      1,
      'CASA',
      5,
      { soloDiaId: 10 }
    );
    expect(sustituciones).toBeGreaterThan(0);
    expect(plan.semana[0].ejercicios[0].nombre).not.toBe('Press banca con barra');
    expect(plan.semana[1].ejercicios[0].nombre).toBe('Carrera continua Z2 30 min');
  });

  test('no sustituye espalda por pierna al adaptar entorno', async () => {
    const catalogo = [
      ...catalogoCasa,
      {
        id_ejercicio: 201,
        nombre: 'Remo con mochila',
        pilar: 'FUERZA',
        grupo_muscular: 'Espalda',
        equipamiento: 'Mochila',
        entornos: 'CASA',
        tipo_ilustracion: 'PULL'
      },
      {
        id_ejercicio: 202,
        nombre: 'Step-up en escalera',
        pilar: 'FUERZA',
        grupo_muscular: 'Pierna',
        equipamiento: 'Escalera',
        entornos: 'CASA',
        tipo_ilustracion: 'SQUAT'
      }
    ];
    const planDominadas = {
      ...planBase,
      semana: [
        {
          id_plan_dia: 10,
          dia_semana: 1,
          enfoque: 'FUERZA',
          ejercicios: [
            {
              id_ejercicio: 1,
              nombre: 'Fuerza inicial tren superior: Dominadas asistidas con goma',
              pilar: 'FUERZA',
              grupo_muscular: 'General',
              series: 4,
              repeticiones: 8,
              descanso: 90
            }
          ]
        }
      ]
    };
    db.query.mockResolvedValue([catalogo]);
    const { plan } = await PlanGeneradorService.generarSemana(planDominadas, 1, 'CASA', 3);
    const ej = plan.semana[0].ejercicios[0];
    expect(ej.nombre.toLowerCase()).not.toContain('step-up');
    if (ej.sustituido) {
      expect(['Espalda', 'espalda']).toContain(ej.grupo_muscular);
      expect(ej.nombre_original).toBe('Dominadas asistidas con goma');
    }
  });

  test('guardarCache reintenta sin emojis si MySQL rechaza utf8', async () => {
    const planConEmoji = {
      ...planBase,
      personalizacion: { entorno_emoji: '🏠', resumen: 'Casa 🏠' }
    };
    db.query
      .mockRejectedValueOnce(new Error("Incorrect string value: '\\xF0\\x9F\\x8F\\xA0' for column 'plan_json'"))
      .mockResolvedValueOnce([{ affectedRows: 1 }]);
    await PlanGeneradorService.guardarCache(1, 1, 202512, 'CASA', 3, planConEmoji, 'Coaching 🏠');
    expect(db.query).toHaveBeenCalledTimes(2);
    const retryArgs = db.query.mock.calls[1][1];
    expect(retryArgs[5]).not.toContain('🏠');
    expect(retryArgs[6]).not.toContain('🏠');
  });
});
