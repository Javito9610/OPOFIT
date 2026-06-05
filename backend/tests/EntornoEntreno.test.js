const {
  normalizarEntorno,
  normalizarPilar,
  inferirEntornosDesdeEquipamiento,
  ejercicioCompatible,
  inferirTipoIlustracion,
  seededPick,
  grupoClave
} = require('../src/utils/EntornoEntreno');

describe('EntornoEntreno', () => {
  test('normaliza entornos válidos', () => {
    expect(normalizarEntorno('gym')).toBe('GYM');
    expect(normalizarEntorno('casa')).toBe('CASA');
    expect(normalizarEntorno('invalido')).toBeNull();
  });

  test('infiere entornos desde equipamiento', () => {
    expect(inferirEntornosDesdeEquipamiento('Barra fija', 'FUERZA')).toContain('CALISTENIA');
    expect(inferirEntornosDesdeEquipamiento('Mancuernas', 'FUERZA')).toContain('GYM');
    expect(inferirEntornosDesdeEquipamiento('—', 'RESISTENCIA')).toContain('PISTA');
  });

  test('filtra ejercicios por entorno usuario', () => {
    expect(ejercicioCompatible('CASA,CALISTENIA', 'CASA')).toBe(true);
    expect(ejercicioCompatible('GYM,CROSSFIT', 'CASA')).toBe(false);
    expect(ejercicioCompatible('GYM,MIXTO', 'CASA')).toBe(true);
    expect(ejercicioCompatible(null, 'GYM')).toBe(true);
  });

  test('infiere tipo ilustración', () => {
    expect(inferirTipoIlustracion('Flexiones diamante', 'FUERZA', 'Pecho')).toBe('PUSH');
    expect(inferirTipoIlustracion('Dominadas pronas', 'FUERZA', 'Espalda')).toBe('PULL');
    expect(inferirTipoIlustracion('Sprint 30 m x 6', 'VELOCIDAD', 'Velocidad')).toBe('AGILITY');
    expect(inferirTipoIlustracion('Plancha frontal', 'CORE', 'Core')).toBe('PLANK');
  });

  test('seededPick es determinista', () => {
    const arr = ['a', 'b', 'c', 'd'];
    expect(seededPick(arr, 3, 'key')).toBe(seededPick(arr, 3, 'key'));
    expect(seededPick(arr, 4, 'key')).not.toBe(seededPick(arr, 3, 'key'));
  });

  test('normaliza pilares legacy', () => {
    expect(normalizarPilar('TREN_SUPERIOR')).toBe('FUERZA');
    expect(normalizarPilar('TREN_INFERIOR')).toBe('FUERZA');
    expect(normalizarPilar('CARDIO')).toBe('RESISTENCIA');
    expect(grupoClave('TREN_SUPERIOR', 'Pecho', 'Press banca')).toBe(
      grupoClave('FUERZA', 'Pecho', 'Press banca')
    );
  });

  test('grupoClave agrupa por pilar y patrón', () => {
    expect(grupoClave('FUERZA', 'Pecho', 'Press banca')).toContain('pecho');
    expect(grupoClave('RESISTENCIA', 'Cardio', 'HIIT 30:30')).toContain('intervalos');
    expect(grupoClave('VELOCIDAD', 'Velocidad', 'Sprint 40 m')).toContain('sprint');
  });
});
