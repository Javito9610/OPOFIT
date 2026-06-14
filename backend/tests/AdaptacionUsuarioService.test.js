const A = require('../src/services/AdaptacionUsuarioService');

describe('AdaptacionUsuarioService — lesiones', () => {
  test('rodilla bloquea pliometría y sprint', () => {
    const r1 = A.evaluarLesiones(['rodilla'], { nombre: 'Box jump 60 cm', patron_movimiento: 'PLYO' });
    const r2 = A.evaluarLesiones(['rodilla'], { nombre: 'Sprint 60 m x 6', patron_movimiento: 'SPRINT' });
    expect(r1.bloqueado).toBe(true);
    expect(r2.bloqueado).toBe(true);
  });

  test('hombro bloquea press militar y dominada', () => {
    const r1 = A.evaluarLesiones(['hombro'], { nombre: 'Press militar con barra', patron_movimiento: 'PUSH_V' });
    const r2 = A.evaluarLesiones(['hombro'], { nombre: 'Dominadas pronas', patron_movimiento: 'PULL_V' });
    expect(r1.bloqueado).toBe(true);
    expect(r2.bloqueado).toBe(true);
  });

  test('lumbar bloquea peso muerto pesado', () => {
    const r = A.evaluarLesiones(['lumbar'], { nombre: 'Peso muerto convencional', patron_movimiento: 'HINGE' });
    expect(r.bloqueado).toBe(true);
    expect(r.motivos.join(' ')).toMatch(/cadera|cadena|compresión/i);
  });

  test('sin lesión no bloquea nada', () => {
    const r = A.evaluarLesiones([], { nombre: 'Sentadilla con barra', patron_movimiento: 'SQUAT' });
    expect(r.bloqueado).toBe(false);
  });
});

describe('AdaptacionUsuarioService — compresión por tiempo', () => {
  function sesion() {
    return [
      { nombre: 'Sentadilla con barra', series: 4, descanso: 120 },
      { nombre: 'Press banca', series: 4, descanso: 90 },
      { nombre: 'Remo con barra', series: 3, descanso: 75 },
      { nombre: 'Curl mancuernas', series: 3, descanso: 60 },
      { nombre: 'Plancha', series: 3, descanso: 30 }
    ];
  }

  test('sesión cabe en su tiempo original (no recorta)', () => {
    const total = A.estimarMinutosSesion(sesion());
    const r = A.comprimirSesion(sesion(), total + 5);
    expect(r.ajustes.length).toBe(0);
    expect(r.ejercicios.length).toBe(5);
  });

  test('30 min hace dropear accesorios', () => {
    const r = A.comprimirSesion(sesion(), 30);
    expect(r.ejercicios.length).toBeLessThan(5);
    expect(r.ajustes.length).toBeGreaterThan(0);
    // El principal (sentadilla) NO se toca.
    expect(r.ejercicios[0].nombre).toBe('Sentadilla con barra');
  });

  test('45 min recorta solo accesorios sin tocar el principal', () => {
    const r = A.comprimirSesion(sesion(), 45);
    expect(r.ejercicios[0].nombre).toBe('Sentadilla con barra');
    expect(r.ejercicios[0].series).toBe(4);  // intacto
  });

  test('tiempo extremo (15 min) reduce series + descansos', () => {
    const r = A.comprimirSesion(sesion(), 15);
    const hayReduccionSeries = r.ajustes.some((a) => /serie/i.test(a));
    expect(hayReduccionSeries || r.ejercicios.length <= 2).toBe(true);
  });
});

describe('AdaptacionUsuarioService — autoregulación por fatiga', () => {
  function sesion() {
    return [
      { nombre: 'Sentadilla', series: 5, descanso: 120 },
      { nombre: 'Press banca', series: 4, descanso: 90 }
    ];
  }

  test('fatiga 1 (pude hacer mucho más) → sube volumen', () => {
    const r = A.ajustarPorFatiga(sesion(), 1);
    expect(r.ejercicios[0].series).toBeGreaterThanOrEqual(5);
    expect(r.ajuste).toMatch(/\+/);
  });

  test('fatiga 3 (justo) → mantiene', () => {
    const r = A.ajustarPorFatiga(sesion(), 3);
    expect(r.ejercicios[0].series).toBe(5);
    expect(r.ajuste).toBeNull();
  });

  test('fatiga 5 (me destrocé) → reduce volumen 25% y sube descansos', () => {
    const r = A.ajustarPorFatiga(sesion(), 5);
    expect(r.ejercicios[0].series).toBeLessThan(5);
    expect(r.ejercicios[0].descanso).toBeGreaterThan(120);
    expect(r.ajuste).toMatch(/menos|reducido|25/i);
  });
});

describe('AdaptacionUsuarioService — adaptarSesion combinado', () => {
  test('rodilla + 30 min combina filtro + compresión', () => {
    const sesion = [
      { nombre: 'Sentadilla con barra', patron_movimiento: 'SQUAT', series: 4, descanso: 120 },
      { nombre: 'Box jump 60 cm', patron_movimiento: 'PLYO', series: 4, descanso: 90 },
      { nombre: 'Remo con barra', patron_movimiento: 'PULL_H', series: 4, descanso: 75 },
      { nombre: 'Plancha', patron_movimiento: 'ANTI_EXT', series: 3, descanso: 30 }
    ];
    const r = A.adaptarSesion(sesion, { lesiones: ['rodilla'], tiempoDisponibleMin: 30 });
    expect(r.avisos.some((a) => /Box jump/i.test(a))).toBe(true);
    expect(r.ejercicios.find((e) => /Box jump/i.test(e.nombre))).toBeUndefined();
  });

  test('lesión sola sin tiempo no comprime', () => {
    const sesion = [
      { nombre: 'Sentadilla con barra', patron_movimiento: 'SQUAT', series: 4, descanso: 120 },
      { nombre: 'Press militar', patron_movimiento: 'PUSH_V', series: 4, descanso: 90 }
    ];
    const r = A.adaptarSesion(sesion, { lesiones: ['hombro'] });
    expect(r.ejercicios.length).toBe(1);  // solo sentadilla
    expect(r.avisos.length).toBe(1);
  });
});
