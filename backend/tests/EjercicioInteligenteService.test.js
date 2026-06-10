const {
  clasificarPerfil,
  generarPrescripcion,
  generarInstrucciones,
  aplicarInteligencia,
  parsearPrescripcionNombre
} = require('../src/services/EjercicioInteligenteService');

describe('EjercicioInteligenteService.parsearPrescripcionNombre', () => {
  test('lee series en metros del nombre', () => {
    const p = parsearPrescripcionNombre('Series 400 m x 6');
    expect(p).toEqual({ series: 6, repeticiones: 400, unidad: 'm', descanso: 150 });
  });

  test('lee minutos de carrera continua', () => {
    const p = parsearPrescripcionNombre('Carrera continua Z2 30 min');
    expect(p).toMatchObject({ series: 1, repeticiones: 30, unidad: 'min' });
  });
});

describe('EjercicioInteligenteService.generarPrescripcion', () => {
  test('wrist curl tiene reps realistas y distintas por nombre', () => {
    const a = generarPrescripcion(
      { nombre: 'Reverse wrist curl', pilar: 'FUERZA', grupo_muscular: 'Brazos' },
      { seed: 1 }
    );
    const b = generarPrescripcion(
      { nombre: 'Wrist curl con barra', pilar: 'FUERZA', grupo_muscular: 'Brazos' },
      { seed: 2 }
    );
    expect(a.repeticiones).toBeLessThanOrEqual(18);
    expect(a.repeticiones).toBeGreaterThanOrEqual(12);
    expect(b.repeticiones).toBeLessThanOrEqual(18);
    expect(a.repeticiones).not.toBe(b.repeticiones);
  });

  test('dominadas tienen pocas reps', () => {
    const p = generarPrescripcion(
      { nombre: 'Dominadas pronas', pilar: 'FUERZA', grupo_muscular: 'Espalda' },
      { seed: 5 }
    );
    expect(p.repeticiones).toBeLessThanOrEqual(8);
    expect(p.series).toBeGreaterThanOrEqual(3);
  });

  test('press banca tiene reps de fuerza', () => {
    const p = generarPrescripcion(
      { nombre: 'Press banca con mancuernas', pilar: 'FUERZA', grupo_muscular: 'Pecho' },
      { seed: 3 }
    );
    expect(p.repeticiones).toBeGreaterThanOrEqual(6);
    expect(p.repeticiones).toBeLessThanOrEqual(12);
  });
});

describe('EjercicioInteligenteService.generarInstrucciones', () => {
  test('reverse wrist curl tiene texto técnico propio', () => {
    const txt = generarInstrucciones({
      nombre: 'Reverse wrist curl',
      pilar: 'FUERZA',
      grupo_muscular: 'Brazos',
      equipamiento: 'Barra',
      instrucciones_tecnicas: 'Extensores de muñeca.'
    });
    expect(txt.length).toBeGreaterThan(60);
    expect(txt.toLowerCase()).toMatch(/muñeca|muneca/);
    expect(txt).not.toBe('Extensores de muñeca.');
  });

  test('press banca y flexiones no comparten el mismo texto', () => {
    const press = generarInstrucciones({
      nombre: 'Press banca con mancuernas',
      pilar: 'FUERZA',
      grupo_muscular: 'Pecho',
      equipamiento: 'Mancuernas/banco'
    });
    const flex = generarInstrucciones({
      nombre: 'Flexiones estándar',
      pilar: 'FUERZA',
      grupo_muscular: 'Pecho',
      equipamiento: 'Suelo'
    });
    expect(press).not.toBe(flex);
    expect(press.toLowerCase()).toContain('banco');
    expect(flex.toLowerCase()).toContain('suelo');
  });

  test('carrera continua tiene instrucciones de ritmo', () => {
    const txt = generarInstrucciones({
      nombre: 'Trote suave 25 min',
      pilar: 'RESISTENCIA',
      grupo_muscular: 'Cardio',
      equipamiento: 'Pista'
    });
    expect(txt.toLowerCase()).toMatch(/ritmo|aeróbic|rpe|hablar/);
  });

  test('patada de tríceps no duplica texto al regenerar', () => {
    const base = {
      nombre: 'Patada de tríceps',
      pilar: 'FUERZA',
      grupo_muscular: 'Brazos',
      equipamiento: 'Mancuernas',
      instrucciones_tecnicas: 'Codo fijo, extensión completa.'
    };
    const primera = generarInstrucciones(base);
    const segunda = generarInstrucciones({
      ...base,
      instrucciones_tecnicas: primera,
      prescripcion_inteligente: true
    });
    expect(segunda).toBe(primera);
    expect((segunda.match(/Codos apuntan al techo/g) || []).length).toBe(1);
    expect((segunda.match(/Mueve cada lado con simetría/g) || []).length).toBe(1);
  });

  test('limpia instrucciones corruptas con frases repetidas', () => {
    const corrupto =
      'Codos apuntan al techo o quedan pegados al cuerpo según variante. Extiende antebrazos sin abrir codos. Controla la fase excéntrica. ' +
      'Codos apuntan al techo o quedan pegados al cuerpo según variante. Extiende antebrazos sin abrir codos. Controla la fase excéntrica. ' +
      'Detalle técnico: Codo fijo, extensión completa en cada repetición de Patada de tríceps. ' +
      'Mueve cada lado con simetría; evita impulso con la espalda. Mueve cada lado con simetría; evita impulso con la espalda.';
    const limpio = generarInstrucciones({
      nombre: 'Patada de tríceps',
      pilar: 'FUERZA',
      grupo_muscular: 'Brazos',
      equipamiento: 'Mancuernas',
      instrucciones_tecnicas: corrupto
    });
    expect((limpio.match(/Codos apuntan al techo/g) || []).length).toBe(1);
    expect((limpio.match(/Mueve cada lado con simetría/g) || []).length).toBe(1);
    expect((limpio.match(/Detalle técnico:/g) || []).length).toBe(1);
  });
});

describe('EjercicioInteligenteService.aplicarInteligencia', () => {
  test('marca prescripcion inteligente y corrige reps absurdas', () => {
    const ej = aplicarInteligencia(
      {
        nombre: 'Reverse wrist curl',
        pilar: 'FUERZA',
        series: 3,
        repeticiones: 77,
        unidad: 'reps'
      },
      { seed: 10 }
    );
    expect(ej.prescripcion_inteligente).toBe(true);
    expect(ej.repeticiones).toBeLessThanOrEqual(18);
    expect(ej.instrucciones_tecnicas.length).toBeGreaterThan(50);
  });

  test('enriquecer + aplicarInteligencia no duplica instrucciones', () => {
    const { enriquecerEjercicio } = require('../src/services/EjercicioMetadataService');
    const ej = aplicarInteligencia(
      enriquecerEjercicio({
        nombre: 'Patada de tríceps',
        pilar: 'FUERZA',
        grupo_muscular: 'Brazos',
        equipamiento: 'Mancuernas',
        instrucciones_tecnicas: 'Codo fijo, extensión completa.'
      }),
      { seed: 3 }
    );
    // Instrucción técnica del banco (textoMovimiento) siempre presente.
    expect((ej.instrucciones_tecnicas.match(/Codos apuntan al techo/g) || []).length).toBe(1);
    // La instrucción original "Codo fijo, extensión completa." es ahora
    // considerada GENÉRICA (<60 chars, una sola frase): se descarta para no
    // contaminar el texto con pistas pobres. El usuario lo reportó.
    expect(ej.instrucciones_tecnicas).not.toContain('Detalle técnico:');
  });

  test('clasifica sprint como velocidad', () => {
    const c = clasificarPerfil({ nombre: 'Sprint 60 m x 6', pilar: 'VELOCIDAD' });
    expect(c.perfil).toBe('PARSED');
    expect(c.unidad).toBe('m');
  });
});
