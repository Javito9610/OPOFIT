const { parseGpx } = require('../src/utils/GpxImportService');

const SAMPLE_GPX = `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Test">
  <trk><name>Test run</name><type>running</type><trkseg>
    <trkpt lat="40.4168" lon="-3.7038"><ele>650</ele><time>2026-06-01T10:00:00Z</time></trkpt>
    <trkpt lat="40.4178" lon="-3.7028"><ele>655</ele><time>2026-06-01T10:05:00Z</time>
      <extensions><gpxtpx:hr>145</gpxtpx:hr></extensions>
    </trkpt>
    <trkpt lat="40.4188" lon="-3.7018"><ele>660</ele><time>2026-06-01T10:10:00Z</time></trkpt>
  </trkseg></trk>
</gpx>`;

describe('GpxImportService.parseGpx', () => {
  test('parsea GPX valido con distancia y duracion', () => {
    const r = parseGpx(SAMPLE_GPX);
    expect(r.type).toBe('RUN');
    expect(r.points).toHaveLength(3);
    expect(r.distanceM).toBeGreaterThan(25);
    expect(r.durationSec).toBe(600);
    expect(r.id).toMatch(/^gpx_/);
  });

  test('rechaza GPX sin puntos', () => {
    expect(() => parseGpx('<gpx></gpx>')).toThrow(/puntos/);
  });

  test('rechaza ruta demasiado corta', () => {
    const tiny = `<?xml version="1.0"?><gpx><trk><trkseg>
      <trkpt lat="40.0" lon="-3.0"><time>2026-01-01T10:00:00Z</time></trkpt>
      <trkpt lat="40.00001" lon="-3.00001"><time>2026-01-01T10:00:05Z</time></trkpt>
    </trkseg></trk></gpx>`;
    expect(() => parseGpx(tiny)).toThrow(/corta/);
  });
});
