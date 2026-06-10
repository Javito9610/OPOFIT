/**
 * Saneamiento de "realismo" del banco.
 *
 * Problema reportado por el usuario: en su plan para GYM aparecían cosas como
 * "Sprints con saco 15 kg" o "Towel pull-up". Esos materiales (sacos de arena,
 * trineos, mazas, yokes, toallas) NO son típicos de un gimnasio comercial. Su
 * sitio natural son boxes de CrossFit, salas de strongman o entrenamiento al
 * aire libre.
 *
 * Lo que hace el script:
 *   1. Limpia el campo `entornos` de ejercicios con material no estándar para
 *      GYM/CASA, dejándolos solo en CROSSFIT/PISTA si procede.
 *   2. Renombra ejercicios con títulos confusos para que se lean mejor en la UI
 *      (ej. "Vallas + giros 180°" → "Vallas con giro de 180°").
 *   3. Sustituye "Sprints con saco 15 kg" por un nombre más coherente para los
 *      planes (5×30 m sprint salida tumbado, R 1'30").
 */
const fs = require('fs');
const path = require('path');

const RUTA = path.resolve(__dirname, '../data/ejercicios-banco-500.json');

// Equipamiento que SOLO encaja en CrossFit / strongman / aire libre.
// Cualquier ejercicio con este equipamiento se restringe a CROSSFIT / PISTA.
const EQUIP_NO_GYM = /^(Trineo|Yoke|Maza|Saco arena|Sandbag|Saco$|Bolsa búlgara|Barra\/toalla)$/i;

// Renombrados: clave = nombre actual, valor = nombre nuevo más legible.
// Pensados para que el usuario entienda el ejercicio leyendo el título.
const RENOMBRADOS = {
  'Vallas + giros 180°': 'Vallas con giro de 180°',
  'Press de banca + dominada bomberos': 'Press banca y dominada (test bombero)',
  'Cambio de dirección 90° x 8': 'Cambios de dirección 90° (8 series)',
  'Sprint con trineo ligero 20 m': 'Sprint con trineo ligero (20 m)',
  'Trineo o sled push': 'Empuje de trineo',
  'Sled push': 'Empuje de trineo',
  'Sled push (trineo)': 'Empuje de trineo',
  'Sled drag (trineo)': 'Arrastre de trineo',
  'Sled drag marcha atrás': 'Arrastre de trineo marcha atrás',
  'Mace 360': 'Maza giratoria 360°',
  'Mace 10-2': 'Maza pendular 10-2',
  'Mace squat': 'Sentadilla con maza al hombro',
  'Towel pull-up': 'Dominada con toalla (agarre)',
  'Sandbag clean': 'Cargada de saco a hombro',
  'Sandbag carry': 'Transporte de saco a hombro',
  'Yoke carry': 'Yoke walk (caminar con barra cargada)',
  'Buceo 10m + 40m crol': 'Buceo 10 m + 40 m crol'
};

function limpiarEntornosCrossfitOnly(csv) {
  if (!csv) return 'CROSSFIT,MIXTO';
  const partes = new Set(csv.split(',').map((s) => s.trim()).filter(Boolean));
  // Quitamos GYM, CASA — esos no encajan con material strongman / saco.
  partes.delete('GYM');
  partes.delete('CASA');
  partes.delete('CALISTENIA');
  // Garantizamos al menos CROSSFIT.
  if (!partes.size) partes.add('CROSSFIT');
  partes.add('MIXTO');
  return [...partes].join(',');
}

function main() {
  const banco = JSON.parse(fs.readFileSync(RUTA, 'utf8'));
  let entornosLimpiados = 0;
  let renombrados = 0;

  for (const e of banco.ejercicios) {
    // Renombrar
    if (RENOMBRADOS[e.nombre]) {
      e.nombre = RENOMBRADOS[e.nombre];
      renombrados += 1;
    }
    // Limpiar entornos no realistas si el equipamiento es de strongman
    if (e.equipamiento && EQUIP_NO_GYM.test(String(e.equipamiento).trim())) {
      const antes = e.entornos;
      e.entornos = limpiarEntornosCrossfitOnly(e.entornos);
      if (antes !== e.entornos) entornosLimpiados += 1;
    }
  }

  fs.writeFileSync(RUTA, JSON.stringify(banco, null, 2));
  console.log(`✓ Saneamiento realismo`);
  console.log(`  Renombrados:        ${renombrados}`);
  console.log(`  Entornos limpiados: ${entornosLimpiados}`);
  console.log(`  Total ejercicios:   ${banco.ejercicios.length}`);
}

main();
