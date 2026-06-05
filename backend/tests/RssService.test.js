const RssService = require('../src/services/RssService');

describe('RssService pro', () => {
  test('_clasificar detecta convocatoria', () => {
    expect(RssService._clasificar('Convocatoria ingreso Policía Nacional 2026', '')).toBe(
      'convocatoria'
    );
  });

  test('_clasificar detecta plazo', () => {
    expect(RssService._clasificar('Ampliación plazo inscripción', 'hasta el día 15')).toBe('plazo');
  });

  test('_resumen trunca con sentido', () => {
    const r = RssService._resumen('Primera frase corta. Segunda frase muy larga que no debería aparecer.');
    expect(r.length).toBeLessThanOrEqual(141);
    expect(r).toContain('Primera');
  });

  test('_enriquecerNoticia añade campos', () => {
    const n = RssService._enriquecerNoticia(
      {
        titulo: 'Convocatoria Guardia Civil',
        descripcion: 'Se abre el proceso selectivo.',
        tipo: 'rss'
      },
      2
    );
    expect(n.categoria).toBe('convocatoria');
    expect(n.urgente).toBe(true);
    expect(n.resumen.length).toBeGreaterThan(0);
  });
});
