jest.mock('../src/config/db');
const db = require('../src/config/db');
const PremiumService = require('../src/services/PremiumService');

describe('PremiumService', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('getEstadoPremium', () => {
    test('false si usuario no existe', async () => {
      db.query.mockResolvedValueOnce([[]]);
      const r = await PremiumService.getEstadoPremium(999);
      expect(r).toEqual({ esPremium: false, premiumHasta: null });
    });

    test('false si es_premium = 0', async () => {
      db.query.mockResolvedValueOnce([[{ es_premium: 0, premium_hasta: null, oposiciones_id_oposicion: 1 }]]);
      const r = await PremiumService.getEstadoPremium(1);
      expect(r.esPremium).toBe(false);
    });

    test('true si es_premium=1 sin fecha', async () => {
      db.query.mockResolvedValueOnce([[{ es_premium: 1, premium_hasta: null, oposiciones_id_oposicion: 1 }]]);
      const r = await PremiumService.getEstadoPremium(1);
      expect(r.esPremium).toBe(true);
    });

    test('true si es_premium=1 y premium_hasta futuro', async () => {
      const futuro = new Date();
      futuro.setDate(futuro.getDate() + 30);
      db.query.mockResolvedValueOnce([[{ es_premium: 1, premium_hasta: futuro, oposiciones_id_oposicion: 1 }]]);
      const r = await PremiumService.getEstadoPremium(1);
      expect(r.esPremium).toBe(true);
    });

    test('false si es_premium=1 pero premium_hasta caducado', async () => {
      const pasado = new Date();
      pasado.setDate(pasado.getDate() - 10);
      db.query.mockResolvedValueOnce([[{ es_premium: 1, premium_hasta: pasado, oposiciones_id_oposicion: 1 }]]);
      const r = await PremiumService.getEstadoPremium(1);
      expect(r.esPremium).toBe(false);
    });
  });

  describe('activarPremium', () => {
    test('activa por N dias', async () => {
      db.query.mockResolvedValueOnce([{ affectedRows: 1 }]);
      const r = await PremiumService.activarPremium(1, 15);
      expect(r.esPremium).toBe(true);
      const ms = r.premiumHasta.getTime() - Date.now();
      expect(ms).toBeGreaterThan(14 * 24 * 3600 * 1000);
    });
  });

  describe('puedeAccederOposicion', () => {
    test('true si existe', async () => {
      db.query.mockResolvedValueOnce([[{ id_oposicion: 1 }]]);
      expect(await PremiumService.puedeAccederOposicion(1, 1)).toBe(true);
    });

    test('false si no existe', async () => {
      db.query.mockResolvedValueOnce([[]]);
      expect(await PremiumService.puedeAccederOposicion(1, 999)).toBe(false);
    });
  });

  describe('puedeVerHistorialSimulacros', () => {
    test('refleja estado premium', async () => {
      db.query.mockResolvedValueOnce([[{ es_premium: 1, premium_hasta: null }]]);
      expect(await PremiumService.puedeVerHistorialSimulacros(1)).toBe(true);
      db.query.mockResolvedValueOnce([[{ es_premium: 0, premium_hasta: null }]]);
      expect(await PremiumService.puedeVerHistorialSimulacros(2)).toBe(false);
    });
  });

  describe('limitarBaremos / filtros', () => {
    test('limitarBaremos no recorta (politica actual: visible para todos)', () => {
      const lista = [1, 2, 3, 4, 5];
      expect(PremiumService.limitarBaremos(false, lista)).toEqual(lista);
      expect(PremiumService.limitarBaremos(true, lista)).toEqual(lista);
    });

    test('filtrarRutinasOpoPorNivel respeta premium', () => {
      const plan = [
        { nivel: 'BASICO' },
        { nivel: 'INTERMEDIO' },
        { nivel: 'AVANZADO' }
      ];
      expect(PremiumService.filtrarRutinasOpoPorNivel(true, plan)).toEqual(plan);
      expect(PremiumService.filtrarRutinasOpoPorNivel(false, plan)).toEqual([{ nivel: 'BASICO' }]);
    });
  });
});
