jest.mock('../src/config/db', () => ({ query: jest.fn() }));

const fs = require('fs');
const path = require('path');
const db = require('../src/config/db');
const EjerciciosBanco500Service = require('../src/services/EjerciciosBanco500Service');

describe('EjerciciosBanco500Service', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('seedBanco500 inserta ejercicios del JSON', async () => {
    const jsonPath = path.resolve(__dirname, '../data/ejercicios-banco-500.json');
    const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    // Tras la ampliación v4 el banco crece. Lo que importa es que sea ≥500.
    expect(data.ejercicios.length).toBeGreaterThanOrEqual(500);
    const total = data.ejercicios.length;

    db.query
      .mockResolvedValueOnce([[]]) // meta vacía
      .mockResolvedValueOnce([[{ n: 60 }]]) // count inicial
      .mockImplementation(async (sql, params) => {
        if (sql.includes('SELECT id_ejercicio FROM ejercicios')) return [[]];
        if (sql.includes('INSERT INTO ejercicios')) return [{ insertId: 100 }];
        if (sql.includes('app_meta')) return [{ affectedRows: 1 }];
        if (sql.includes('SELECT COUNT(*) AS total')) return [[{ total: total + 60 }]];
        return [[]];
      });

    const r = await EjerciciosBanco500Service.seedBanco500(true);
    expect(r.skipped).toBe(false);
    expect(r.insertados + r.actualizados).toBe(total);
    expect(r.total).toBe(total + 60);
  });

  test('seedBanco500 omite si ya está sembrado y hay >= 500', async () => {
    // Debe coincidir con BANCO_VERSION (actual: 8) para entrar en la rama de skip.
    const BANCO_VERSION_ACTUAL = '8';
    db.query
      .mockResolvedValueOnce([[{ valor: BANCO_VERSION_ACTUAL }]])
      .mockResolvedValueOnce([[{ n: 1036 }]]);

    const r = await EjerciciosBanco500Service.seedBanco500(false);
    expect(r.skipped).toBe(true);
    expect(r.total).toBe(1036);
  });

  test('JSON tiene pilares y entornos válidos', () => {
    const jsonPath = path.resolve(__dirname, '../data/ejercicios-banco-500.json');
    const { ejercicios } = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    const pilares = new Set(ejercicios.map((e) => e.pilar));
    expect(pilares.has('FUERZA')).toBe(true);
    expect(pilares.has('RESISTENCIA')).toBe(true);
    expect(pilares.has('VELOCIDAD')).toBe(true);
    expect(pilares.has('CORE')).toBe(true);
    expect(pilares.has('MOVILIDAD')).toBe(true);
    const conEntorno = ejercicios.filter((e) => e.entornos && e.entornos.includes('MIXTO'));
    expect(conEntorno.length).toBeGreaterThan(400);
  });

  test('JSON tiene modalidad y score_tipo en TODOS los ejercicios', () => {
    const jsonPath = path.resolve(__dirname, '../data/ejercicios-banco-500.json');
    const { ejercicios } = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
    const sinModalidad = ejercicios.filter((e) => !e.modalidad);
    const sinScore = ejercicios.filter((e) => !e.score_tipo);
    expect(sinModalidad.length).toBe(0);
    expect(sinScore.length).toBe(0);
    const modalidades = new Set(ejercicios.map((e) => e.modalidad));
    expect(modalidades.has('wod')).toBe(true);
    expect(modalidades.has('calistenia')).toBe(true);
    expect(modalidades.has('crossfit_lift')).toBe(true);
    expect(modalidades.has('amrap') || modalidades.has('emom')).toBe(true);
  });
});
