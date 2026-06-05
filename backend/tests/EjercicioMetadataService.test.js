const {
  normalizarNombreEjercicio,
  inferirGrupoMuscular,
  enriquecerInstrucciones,
  motivoAjusteLegible,
  enriquecerEjercicio,
  motivoSustitucion
} = require('../src/services/EjercicioMetadataService');

describe('EjercicioMetadataService', () => {
  test('limpia prefijos de sesión en el nombre', () => {
    expect(
      normalizarNombreEjercicio('Fuerza inicial tren superior: Dominadas asistidas con goma')
    ).toBe('Dominadas asistidas con goma');
  });

  test('infiere grupo muscular pierna en step-up', () => {
    expect(inferirGrupoMuscular('General', 'Step-up en escalera', 'FUERZA')).toBe('Pierna');
  });

  test('infiere grupo espalda en dominadas', () => {
    expect(inferirGrupoMuscular(null, 'Dominadas asistidas con goma', 'FUERZA')).toBe('Espalda');
  });

  test('reemplaza instrucciones genéricas por texto real', () => {
    const txt = enriquecerInstrucciones(
      'Step-up en escalera',
      'FUERZA',
      'Técnica controlada, carga moderada.'
    );
    expect(txt).toContain('escalón');
    expect(txt).not.toContain('Técnica controlada');
  });

  test('motivo ajuste legible sin mantenimiento/recuperacion crudos', () => {
    const m = motivoAjusteLegible('mantenimiento · recuperacion');
    expect(m).toContain('recuperarte');
    expect(m).not.toContain('mantenimiento');
  });

  test('enriquecer ejercicio sustituido conserva nombre original limpio', () => {
    const ej = enriquecerEjercicio({
      nombre: 'Step-up en escalera',
      nombre_original: 'Fuerza inicial tren superior: Dominadas asistidas con goma',
      sustituido: true,
      pilar: 'FUERZA',
      instrucciones_tecnicas: 'Técnica controlada, carga moderada.'
    });
    expect(ej.nombre_original).toBe('Dominadas asistidas con goma');
    expect(ej.grupo_muscular).toBe('Pierna');
  });

  test('motivo sustitución explica entorno y grupo', () => {
    expect(motivoSustitucion('CASA', 'Pierna')).toContain('casa');
    expect(motivoSustitucion('CASA', 'Pierna')).toContain('pierna');
  });
});
