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

  // Tests del filtrado tipo-específico: el usuario reportaba que al seleccionar
  // CrossFit o Calistenia salían sitios que no eran.
  describe('Filtros must / mustNot por tipo', () => {
    const fakeOverpass = (elements) => ({
      ok: true,
      json: async () => ({ elements })
    });

    test('GYM excluye centros de yoga / pilates aunque OSM los marque', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        fakeOverpass([
          { id: 1, lat: 41.4, lon: 2.16, tags: { name: 'Body & Mind Yoga Studio', amenity: 'gym' } },
          { id: 2, lat: 41.41, lon: 2.17, tags: { name: 'AltaFit', amenity: 'gym' } },
          { id: 3, lat: 41.42, lon: 2.18, tags: { name: 'Centro de Pilates Sant Cugat', amenity: 'gym' } }
        ])
      );
      const res = await MapasService.buscarLugares(41.4, 2.16, 'GYM');
      expect(res.length).toBe(1);
      expect(res[0].nombre).toBe('AltaFit');
    });

    test('CROSSFIT pilla gyms con "Box" o "CrossFit" en el nombre (caso El Espinar / Guadarrama)', async () => {
      // Boxes reales de la sierra de Madrid que solo tienen amenity=gym.
      // El usuario reportaba que no aparecían y exigía que sí lo hicieran.
      global.fetch = jest.fn().mockResolvedValue(
        fakeOverpass([
          { id: 100, lat: 40.68, lon: -4.24, tags: { name: 'CrossFit El Espinar', amenity: 'gym' } },
          { id: 101, lat: 40.67, lon: -4.09, tags: { name: 'Raiz Box', amenity: 'gym' } },
          { id: 102, lat: 40.68, lon: -3.80, tags: { name: 'Colmenar Box', amenity: 'gym' } },
          { id: 103, lat: 40.70, lon: -4.00, tags: { name: 'Gimnasio Boxeo La Roca', amenity: 'gym' } },
          { id: 104, lat: 40.69, lon: -4.10, tags: { name: 'Brooklyn Fitboxing', amenity: 'gym' } },
          { id: 105, lat: 40.71, lon: -4.05, tags: { name: 'Holiday Gym', amenity: 'gym' } }
        ])
      );
      const res = await MapasService.buscarLugares(40.682, -4.241, 'CROSSFIT');
      const nombres = res.map((r) => r.nombre);
      expect(nombres).toContain('CrossFit El Espinar');
      expect(nombres).toContain('Raiz Box');
      expect(nombres).toContain('Colmenar Box');
      // Los falsos positivos quedan fuera.
      expect(nombres).not.toContain('Gimnasio Boxeo La Roca');
      expect(nombres).not.toContain('Brooklyn Fitboxing');
      expect(nombres).not.toContain('Holiday Gym');
    });

    test('CROSSFIT acepta tag explícito sport=crossfit pero descarta yoga', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        fakeOverpass([
          { id: 10, lat: 41.4, lon: 2.16, tags: { name: 'CrossFit Wellness Yoga', sport: 'crossfit' } },
          { id: 11, lat: 41.41, lon: 2.17, tags: { name: 'CrossFit Barcelona', sport: 'crossfit' } },
          { id: 12, lat: 41.42, lon: 2.18, tags: { name: 'CF Box Granollers', sport: 'crossfit' } }
        ])
      );
      const res = await MapasService.buscarLugares(41.4, 2.16, 'CROSSFIT');
      const nombres = res.map((r) => r.nombre);
      expect(nombres).toContain('CrossFit Barcelona');
      expect(nombres).toContain('CF Box Granollers');
      // El yoga tiene tag crossfit por error de OSM → mustNot lo descarta.
      expect(nombres).not.toContain('CrossFit Wellness Yoga');
    });

    test('PISTA descarta pistas de tenis o motor sin sport=athletics', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        fakeOverpass([
          { id: 20, lat: 41.4, lon: 2.16, tags: { name: 'Pista de tenis municipal', leisure: 'track', sport: 'tennis' } },
          { id: 21, lat: 41.41, lon: 2.17, tags: { name: 'Pista de atletismo Joan Serrahima', leisure: 'track', sport: 'athletics' } },
          { id: 22, lat: 41.42, lon: 2.18, tags: { name: 'Hipódromo', leisure: 'track', sport: 'horse_racing' } }
        ])
      );
      const res = await MapasService.buscarLugares(41.4, 2.16, 'PISTA');
      expect(res.map((r) => r.nombre)).toEqual(['Pista de atletismo Joan Serrahima']);
    });

    test('CALISTENIA descarta parques infantiles / skate', async () => {
      global.fetch = jest.fn().mockResolvedValue(
        fakeOverpass([
          { id: 30, lat: 41.4, lon: 2.16, tags: { name: 'Parque infantil', leisure: 'park' } },
          { id: 31, lat: 41.41, lon: 2.17, tags: { name: 'Parque de calistenia', leisure: 'fitness_station' } },
          { id: 32, lat: 41.42, lon: 2.18, tags: { name: 'Outdoor Gym Diagonal Mar', leisure: 'outdoor_gym' } },
          { id: 33, lat: 41.43, lon: 2.19, tags: { name: 'Skatepark Joan Miró', leisure: 'fitness_station' } }
        ])
      );
      const res = await MapasService.buscarLugares(41.4, 2.16, 'CALISTENIA');
      const nombres = res.map((r) => r.nombre);
      expect(nombres).toContain('Parque de calistenia');
      expect(nombres).toContain('Outdoor Gym Diagonal Mar');
      expect(nombres).not.toContain('Parque infantil');
      expect(nombres).not.toContain('Skatepark Joan Miró');
    });
  });
});
