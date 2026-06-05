const MapasService = require('../src/services/MapasService');

const osrmOk = (coords) => ({
  ok: true,
  json: async () => ({
    code: 'Ok',
    routes: [{
      geometry: { coordinates: coords },
      distance: 5000
    }]
  })
});

describe('MapasService', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    delete process.env.GOOGLE_MAPS_API_KEY;
    delete process.env.MAPS_API_KEY;
  });

  test('listarTipos devuelve categorías', () => {
    const tipos = MapasService.listarTipos();
    expect(tipos.length).toBeGreaterThan(0);
    expect(tipos.some((t) => t.id === 'GYM')).toBe(true);
  });

  test('generarRutaSugerida usa calles vía OSRM', async () => {
    global.fetch = jest.fn().mockResolvedValue(
      osrmOk([
        [-3.7038, 40.4168],
        [-3.71, 40.42],
        [-3.715, 40.425],
        [-3.72, 40.43],
        [-3.7038, 40.4168]
      ])
    );

    const ruta = await MapasService.generarRutaSugerida(40.4168, -3.7038, 5);
    expect(ruta.puntos.length).toBeGreaterThan(3);
    expect(ruta.origen).toMatch(/calles|osrm/);
    expect(ruta.nombre).toMatch(/Carrera 5 km/);
    expect(ruta.distanciaKm).toBeGreaterThan(0);
  });

  test('variacion genera otra propuesta distinta', async () => {
    global.fetch = jest.fn()
      .mockResolvedValueOnce(osrmOk([[-3.7038, 40.4168], [-3.71, 40.42], [-3.7038, 40.4168]]))
      .mockResolvedValueOnce(osrmOk([[-3.7038, 40.4168], [-3.72, 40.43], [-3.7038, 40.4168]]));

    const a = await MapasService.generarRutaSugerida(40.4168, -3.7038, 5, 0);
    const b = await MapasService.generarRutaSugerida(40.4168, -3.7038, 5, 3);
    expect(a.puntos[1].lat).not.toBe(b.puntos[1].lat);
    expect(b.variacion).toBe(3);
  });

  test('rutaEntreWaypoints calcula distancia por calles', async () => {
    global.fetch = jest.fn().mockResolvedValue(
      osrmOk([
        [-3.7, 40.41],
        [-3.71, 40.42],
        [-3.72, 40.43]
      ])
    );

    const ruta = await MapasService.rutaEntreWaypoints(
      [
        { lat: 40.41, lng: -3.7 },
        { lat: 40.42, lng: -3.71 },
        { lat: 40.43, lng: -3.72 }
      ],
      'Mi ruta'
    );
    expect(ruta.nombre).toBe('Mi ruta');
    expect(ruta.distanciaKm).toBeGreaterThan(0);
    expect(ruta.origen).toMatch(/osrm|calles/);
  });

  test('aGpx genera XML válido', async () => {
    global.fetch = jest.fn().mockResolvedValue(
      osrmOk([[-3.7, 40.4], [-3.71, 40.41], [-3.7, 40.4]])
    );
    const ruta = await MapasService.generarRutaSugerida(40.4, -3.7, 3);
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

  test('redondearKm evita decimales absurdos', () => {
    expect(MapasService.redondearKm(9.000000953)).toBe(9);
    expect(MapasService.redondearKm(7.55)).toBe(7.6);
  });
});
