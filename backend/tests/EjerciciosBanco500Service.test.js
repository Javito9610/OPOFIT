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
    // Debe coincidir con BANCO_VERSION (actual: 6) para entrar en la rama de skip.
    const BANCO_VERSION_ACTUAL = '6';
    db.query
      .mockResolvedValueOnce([[{ valor: BANCO_VERSION_ACTUAL }]])
      .mockResolvedValueOnce([[{ n: 739 }]]);

    const r = await EjerciciosBanco500Service.seedBanco500(false);
    expect(r.skipped).toBe(true);
    expect(r.total).toBe(739);
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
});
