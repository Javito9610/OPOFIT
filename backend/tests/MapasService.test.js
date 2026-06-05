const MapasService = require('../src/services/MapasService');

describe('MapasService', () => {
  test('listarTipos devuelve categorías', () => {
    const tipos = MapasService.listarTipos();
    expect(tipos.length).toBeGreaterThan(0);
    expect(tipos.some((t) => t.id === 'GYM')).toBe(true);
  });

  test('generarRutaSugerida crea circuito con distancia aproximada', () => {
    const ruta = MapasService.generarRutaSugerida(40.4168, -3.7038, 5);
    expect(ruta.puntos.length).toBeGreaterThan(10);
    expect(ruta.distanciaKm).toBeGreaterThan(3);
    expect(ruta.distanciaKm).toBeLessThan(7);
  });

  test('variacion genera otra propuesta distinta', () => {
    const a = MapasService.generarRutaSugerida(40.4168, -3.7038, 5, 0);
    const b = MapasService.generarRutaSugerida(40.4168, -3.7038, 5, 3);
    expect(a.puntos[0].lat).not.toBe(b.puntos[0].lat);
    expect(b.variacion).toBe(3);
  });

  test('rutaEntreWaypoints calcula distancia', () => {
    const ruta = MapasService.rutaEntreWaypoints(
      [
        { lat: 40.41, lng: -3.7 },
        { lat: 40.42, lng: -3.71 },
        { lat: 40.43, lng: -3.72 }
      ],
      'Mi ruta'
    );
    expect(ruta.nombre).toBe('Mi ruta');
    expect(ruta.distanciaKm).toBeGreaterThan(0);
  });

  test('aGpx genera XML válido', () => {
    const ruta = MapasService.generarRutaSugerida(40.4, -3.7, 3);
    const gpx = MapasService.aGpx(ruta);
    expect(gpx).toContain('<gpx');
    expect(gpx).toContain('<trkpt');
  });

  test('lugaresFallback sin API key', () => {
    const lugares = MapasService.lugaresFallback(40.4, -3.7, 'GYM');
    expect(lugares.length).toBeGreaterThan(0);
    expect(lugares[0].demo).toBe(true);
  });

  test('lugaresFallback distingue calistenia y pista', () => {
    const calistenia = MapasService.lugaresFallback(40.4, -3.7, 'CALISTENIA');
    const pista = MapasService.lugaresFallback(40.4, -3.7, 'PISTA');
    expect(calistenia[0].nombre).not.toBe(pista[0].nombre);
    expect(calistenia[0].tipo).toBe('CALISTENIA');
    expect(pista[0].tipo).toBe('PISTA');
  });
});
