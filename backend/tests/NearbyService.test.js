jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const NearbyService = require('../src/services/NearbyService');

describe('NearbyService', () => {
  beforeEach(() => memDb.reset());

  test('distanciaM calcula metros entre dos puntos', () => {
    const d = NearbyService.distanciaM(40.4168, -3.7038, 40.42, -3.71);
    expect(d).toBeGreaterThan(300);
    expect(d).toBeLessThan(1200);
  });

  test('actualizarUbicacion guarda coordenadas y visibilidad', async () => {
    memDb.makeUser({ id_usuario: 1 });
    const r = await NearbyService.actualizarUbicacion(1, 40.4168, -3.7038, true);
    expect(r).toEqual({ ok: true, visible: true });
    const u = memDb.state.usuarios[0];
    expect(u.ubicacion_lat).toBe(40.4168);
    expect(u.ubicacion_visible).toBe(1);
  });

  test('actualizarUbicacion rechaza coordenadas invalidas', async () => {
    memDb.makeUser({ id_usuario: 1 });
    await expect(NearbyService.actualizarUbicacion(1, 'x', 1, true)).rejects.toThrow('COORDENADAS_INVALIDAS');
    await expect(NearbyService.actualizarUbicacion(1, 95, 1, true)).rejects.toThrow('COORDENADAS_INVALIDAS');
  });

  test('listarCerca excluye al propio usuario', async () => {
    memDb.makeUser({
      id_usuario: 1,
      ubicacion_lat: 40.4168,
      ubicacion_lng: -3.7038,
      ubicacion_visible: 1
    });
    memDb.makeUser({
      id_usuario: 2,
      nombre: 'Cerca',
      ubicacion_lat: 40.417,
      ubicacion_lng: -3.704,
      ubicacion_visible: 1
    });
    const r = await NearbyService.listarCerca(1, 40.4168, -3.7038, 15);
    expect(r).toHaveLength(1);
    expect(r[0].id_usuario).toBe(2);
    expect(r[0].distancia_m).toBeGreaterThan(0);
  });

  test('listarCerca solo usuarios visibles', async () => {
    memDb.makeUser({ id_usuario: 1 });
    memDb.makeUser({
      id_usuario: 2,
      ubicacion_lat: 40.417,
      ubicacion_lng: -3.704,
      ubicacion_visible: 0
    });
    const r = await NearbyService.listarCerca(1, 40.4168, -3.7038, 15);
    expect(r).toEqual([]);
  });

  test('listarCerca incluye modo_uso y oposicion', async () => {
    memDb.state.oposiciones.push({ id_oposicion: 1, nombre: 'PN' });
    memDb.makeUser({ id_usuario: 1 });
    memDb.makeUser({
      id_usuario: 2,
      nombre: 'Opositor',
      modo_uso: 'OPOSITOR',
      oposiciones_id_oposicion: 1,
      ubicacion_lat: 40.417,
      ubicacion_lng: -3.704,
      ubicacion_visible: 1
    });
    const r = await NearbyService.listarCerca(1, 40.4168, -3.7038, 15);
    expect(r[0].modo_uso).toBe('OPOSITOR');
    expect(r[0].oposicion_nombre).toBe('PN');
  });

  test('listarCerca respeta radio en km', async () => {
    memDb.makeUser({ id_usuario: 1 });
    memDb.makeUser({
      id_usuario: 2,
      ubicacion_lat: 40.5,
      ubicacion_lng: -3.7038,
      ubicacion_visible: 1
    });
    const cerca = await NearbyService.listarCerca(1, 40.4168, -3.7038, 15);
    const lejos = await NearbyService.listarCerca(1, 40.4168, -3.7038, 1);
    expect(cerca).toHaveLength(1);
    expect(lejos).toHaveLength(0);
  });

  test('listarCerca rechaza coordenadas invalidas', async () => {
    memDb.makeUser({ id_usuario: 1 });
    await expect(NearbyService.listarCerca(1, null, -3.7)).rejects.toThrow('COORDENADAS_INVALIDAS');
  });
});
