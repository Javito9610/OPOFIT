jest.mock('../src/config/db', () => require('./helpers/inMemoryDb').pool);

const memDb = require('./helpers/inMemoryDb');
const GruposService = require('../src/services/GruposService');

describe('GruposService', () => {
  beforeEach(() => {
    memDb.reset();
    memDb.state.oposiciones.push({ id_oposicion: 1, nombre: 'Policia Nacional' });
    memDb.makeUser({ id_usuario: 1, nombre: 'Admin' });
    memDb.makeUser({ id_usuario: 2, nombre: 'Otro' });
  });

  test('listarGrupos vacio si no hay grupos', async () => {
    const r = await GruposService.listarGrupos(1, 1);
    expect(r).toEqual([]);
  });

  test('crearGrupo crea grupo y asigna ADMIN al creador', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, {
      nombre: 'Grupo PN',
      descripcion: 'Entreno',
      idOposicion: 1
    });
    expect(idGrupo).toBe(1);
    const miembro = await GruposService.esMiembro(1, idGrupo);
    expect(miembro.rol).toBe('ADMIN');
  });

  test('crearGrupo fitness sin oposicion', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, {
      nombre: 'Fitness Madrid',
      idOposicion: null
    });
    const grupos = await GruposService.listarGrupos(1, null);
    expect(grupos).toHaveLength(1);
    expect(grupos[0].idGrupo).toBe(idGrupo);
    expect(grupos[0].idOposicion).toBeNull();
  });

  test('crearGrupo rechaza nombre vacio', async () => {
    await expect(GruposService.crearGrupo(1, { nombre: '  ' })).rejects.toThrow('NOMBRE_OBLIGATORIO');
  });

  test('crearGrupo rechaza oposicion invalida', async () => {
    await expect(
      GruposService.crearGrupo(1, { nombre: 'X', idOposicion: 999 })
    ).rejects.toThrow('OPOSICION_NO_VALIDA');
  });

  test('unirse agrega miembro', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await GruposService.unirse(2, idGrupo);
    const miembro = await GruposService.esMiembro(2, idGrupo);
    expect(miembro.rol).toBe('MIEMBRO');
  });

  test('unirse rechaza si ya es miembro', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await expect(GruposService.unirse(1, idGrupo)).rejects.toThrow('YA_ERES_MIEMBRO');
  });

  test('unirse rechaza grupo inexistente', async () => {
    await expect(GruposService.unirse(2, 99)).rejects.toThrow('GRUPO_NO_ENCONTRADO');
  });

  test('salir elimina membresia', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await GruposService.unirse(2, idGrupo);
    await GruposService.salir(2, idGrupo);
    expect(await GruposService.esMiembro(2, idGrupo)).toBeNull();
  });

  test('salir rechaza si no es miembro', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await expect(GruposService.salir(2, idGrupo)).rejects.toThrow('NO_ERES_MIEMBRO');
  });

  test('enviarMensaje y mensajes solo para miembros', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await expect(GruposService.enviarMensaje(2, idGrupo, 'hola')).rejects.toThrow('NO_ERES_MIEMBRO');
    const { idMensaje } = await GruposService.enviarMensaje(1, idGrupo, 'hola grupo');
    expect(idMensaje).toBe(1);
    const msgs = await GruposService.mensajes(1, idGrupo);
    expect(msgs).toHaveLength(1);
    expect(msgs[0].texto).toBe('hola grupo');
  });

  test('enviarMensaje rechaza texto vacio', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await expect(GruposService.enviarMensaje(1, idGrupo, '   ')).rejects.toThrow('MENSAJE_VACIO');
  });

  test('crearQuedada y listarQuedadas', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    const { idQuedada } = await GruposService.crearQuedada(1, idGrupo, {
      titulo: 'Rodaje',
      fechaHora: '2026-06-10 18:00:00',
      lat: 40.4,
      lng: -3.7
    });
    expect(idQuedada).toBe(1);
    const quedadas = await GruposService.listarQuedadas(1, idGrupo);
    expect(quedadas).toHaveLength(1);
    expect(quedadas[0].titulo).toBe('Rodaje');
  });

  test('crearQuedada rechaza sin titulo', async () => {
    const { idGrupo } = await GruposService.crearGrupo(1, { nombre: 'G', idOposicion: 1 });
    await expect(
      GruposService.crearQuedada(1, idGrupo, { fechaHora: '2026-06-10 18:00:00' })
    ).rejects.toThrow('TITULO_OBLIGATORIO');
  });
});
