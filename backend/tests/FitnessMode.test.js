const { FITNESS_PLAN_OPOSICION, isFitnessModo, planOposicionId } = require('../src/utils/FitnessMode');

describe('FitnessMode', () => {
  test('detecta modo FITNESS', () => {
    expect(isFitnessModo('FITNESS')).toBe(true);
    expect(isFitnessModo('fitness')).toBe(true);
    expect(isFitnessModo('OPOSITOR')).toBe(false);
  });

  test('planOposicionId usa plantilla genérica para fitness', () => {
    expect(planOposicionId({ modo_uso: 'FITNESS', oposiciones_id_oposicion: null })).toBe(
      FITNESS_PLAN_OPOSICION
    );
  });

  test('planOposicionId respeta oposición del opositor', () => {
    expect(planOposicionId({ modo_uso: 'OPOSITOR', oposiciones_id_oposicion: 2 })).toBe(2);
  });

  test('planOposicionId fallback sin usuario', () => {
    expect(planOposicionId(null)).toBe(FITNESS_PLAN_OPOSICION);
  });
});
