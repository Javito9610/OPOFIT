/**
 * Verificación de flujo instrucciones + importación de datos de entreno.
 */
const { generarInstrucciones, aplicarInteligencia } = require('../src/services/EjercicioInteligenteService');
const { enriquecerEjercicio } = require('../src/services/EjercicioMetadataService');

describe('Flujo entreno reloj + instrucciones', () => {
  test('doble pipeline no duplica patada de tríceps', () => {
    const base = {
      nombre: 'Patada de tríceps',
      pilar: 'FUERZA',
      grupo_muscular: 'Brazos',
      equipamiento: 'Mancuernas',
      instrucciones_tecnicas: 'Codo fijo, extensión completa.'
    };
    const ej = aplicarInteligencia(enriquecerEjercicio(base), { seed: 9 });
    expect((ej.instrucciones_tecnicas.match(/Codos apuntan al techo/g) || []).length).toBe(1);
    // Las instrucciones pobres (<60 chars, frase única) ya no se cuelan como
    // "Detalle técnico" — el usuario reportaba que quedaba pobre.
    expect(ej.instrucciones_tecnicas).not.toContain('Detalle técnico:');
  });

  test('tempo run usa unidad tiempo no reps', () => {
    const txt = generarInstrucciones({
      nombre: 'Tempo run 25 min',
      pilar: 'RESISTENCIA',
      grupo_muscular: 'Cardio',
      equipamiento: 'Pista'
    });
    expect(txt.toLowerCase()).toMatch(/ritmo|aeróbic|hablar|tramos|esfuerzo/);
  });

  test('texto corrupto se deduplica al servir', () => {
    const corrupto =
      'Codos apuntan al techo o quedan pegados al cuerpo según variante. Extiende antebrazos sin abrir codos. Controla la fase excéntrica. ' +
      'Codos apuntan al techo o quedan pegados al cuerpo según variante. Extiende antebrazos sin abrir codos. Controla la fase excéntrica.';
    const limpio = generarInstrucciones({
      nombre: 'Patada de tríceps',
      pilar: 'FUERZA',
      instrucciones_tecnicas: corrupto
    });
    expect((limpio.match(/Codos apuntan al techo/g) || []).length).toBe(1);
  });
});
