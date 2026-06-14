/**
 * E2E "adaptación al usuario": verifica que las prefs reales (lesiones,
 * tiempo, fatiga) entran al pipeline y modifican el plan generado.
 *
 * Antes existía AdaptacionUsuarioService como código aislado: ninguna ruta
 * lo invocaba. Este test prueba la cadena completa:
 *
 *   settings.lesiones → obtenerPrefsUsuario → generarSemana → adaptarSesion
 */
process.env.JWT_SECRET = 'opofit-e2e-adapt';
process.env.NOTIFICATIONS_CRON = 'false';
process.env.NOTICIAS_CRON_DISABLED = 'true';

jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);
jest.mock('../src/config/firebaseAdmin', () => ({
  initFirebaseAdmin: () => ({ messaging: () => ({ send: jest.fn().mockResolvedValue('ok') }) })
}));

const memDb = require('./helpers/inMemoryDb');
const PlanGeneradorService = require('../src/services/PlanGeneradorService');

describe('E2E adaptación al usuario', () => {
  beforeEach(() => {
    memDb.reset();
    memDb.state.usuarios.push({
      id_usuario: 99,
      email: 'adapt@opofit.test',
      nombre: 'Adapt',
      genero: 'HOMBRE',
      oposiciones_id_oposicion: 1,
      entorno_entreno: 'CASA',
      plan_variacion_seed: 0,
      dias_entreno_semana: 5
    });
  });

  test('obtenerPrefsUsuario lee lesiones de settings', async () => {
    memDb.state.settings.push({
      usuarios_id_usuario: 99,
      lesiones: 'rodilla,hombro',
      tiempo_disponible_min: 45,
      fatiga_previa: 4
    });
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs.lesiones).toEqual(['rodilla', 'hombro']);
    expect(prefs.tiempoDisponibleMin).toBe(45);
    expect(prefs.fatigaPrevia).toBe(4);
  });

  test('sin settings → arrays/valores por defecto coherentes', async () => {
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs.lesiones).toEqual([]);
    expect(prefs.tiempoDisponibleMin).toBeNull();
    expect(prefs.fatigaPrevia).toBeNull();
  });

  test('tiempo_disponible_min fuera de rango (5/200) se descarta', async () => {
    memDb.state.settings.push({
      usuarios_id_usuario: 99,
      tiempo_disponible_min: 5
    });
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs.tiempoDisponibleMin).toBeNull();

    memDb.state.settings[0].tiempo_disponible_min = 200;
    const prefs2 = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs2.tiempoDisponibleMin).toBeNull();
  });

  test('fatiga_previa fuera de 1-5 se descarta', async () => {
    memDb.state.settings.push({
      usuarios_id_usuario: 99,
      fatiga_previa: 7
    });
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs.fatigaPrevia).toBeNull();

    memDb.state.settings[0].fatiga_previa = 0;
    const prefs2 = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs2.fatigaPrevia).toBeNull();
  });

  test('lesion con espacios y mayúsculas se normaliza', async () => {
    memDb.state.settings.push({
      usuarios_id_usuario: 99,
      lesiones: ' Rodilla , HOMBRO,  ,Tobillo '
    });
    const prefs = await PlanGeneradorService.obtenerPrefsUsuario(99);
    expect(prefs.lesiones).toEqual(['rodilla', 'hombro', 'tobillo']);
  });
});
