const db = require('../config/db');

/** Banco ampliado de ejercicios para rutinas personalizadas (estilo Hevy/Strong). */
const EJERCICIOS_EXTRA = [
  ['Dominadas pronas', 'FUERZA', 'Espalda', 'Barra fija', 'Agarre prono, pecho a la barra sin kipping.'],
  ['Dominadas supinas', 'FUERZA', 'Espalda/Bíceps', 'Barra fija', 'Chin-up controlado, RIR 1-2.'],
  ['Dominadas australianas', 'FUERZA', 'Espalda', 'Barra baja/TRX', 'Cuerpo recto, pecho a la barra.'],
  ['Flexiones diamante', 'FUERZA', 'Pecho/Tríceps', 'Suelo', 'Manos juntas, codos pegados al cuerpo.'],
  ['Flexiones declinadas', 'FUERZA', 'Pecho', 'Banco', 'Pies elevados, core activo.'],
  ['Flexiones con palmada', 'FUERZA', 'Pecho', 'Suelo', 'Explosivas, aterrizaje suave.'],
  ['Press banca con barra', 'FUERZA', 'Pecho', 'Barra/banco', 'Escápulas retraídas, toque pecho controlado.'],
  ['Press inclinado mancuernas', 'FUERZA', 'Pecho', 'Mancuernas/banco', '30-45° inclinación.'],
  ['Press militar con barra', 'FUERZA', 'Hombros', 'Barra/rack', 'Barra frente, sin arqueo lumbar excesivo.'],
  ['Elevaciones laterales', 'FUERZA', 'Hombros', 'Mancuernas', 'Codos ligeramente flexionados.'],
  ['Face pull', 'FUERZA', 'Hombros/Espalda', 'Polea', 'Codos altos, rotación externa.'],
  ['Remo con barra', 'FUERZA', 'Espalda', 'Barra', 'Bisagra de cadera, barra al ombligo.'],
  ['Remo mancuerna unilateral', 'FUERZA', 'Espalda', 'Mancuerna/banco', 'Sin rotar el tronco.'],
  ['Jalón al pecho agarre neutro', 'FUERZA', 'Espalda', 'Polea', 'Pecho alto, codos al bolsillo.'],
  ['Peso muerto convencional', 'FUERZA', 'Cadena posterior', 'Barra', 'Tensión lumbar neutra, empuje de suelo.'],
  ['Peso muerto rumano', 'FUERZA', 'Isquios/Glúteo', 'Barra/mancuernas', 'Bisagra, barra cerca de piernas.'],
  ['Sentadilla trasera', 'FUERZA', 'Pierna', 'Barra/rack', 'Profundidad paralela o más, rodillas alineadas.'],
  ['Sentadilla frontal', 'FUERZA', 'Pierna', 'Barra/rack', 'Codos altos, torso vertical.'],
  ['Sentadilla búlgara', 'FUERZA', 'Pierna', 'Mancuernas/banco', 'Rodilla trasera casi al suelo.'],
  ['Hip thrust con barra', 'FUERZA', 'Glúteo', 'Barra/banco', 'Pausa arriba, mentón al pecho.'],
  ['Zancadas caminando', 'FUERZA', 'Pierna', 'Mancuernas', 'Paso largo, rodilla no sobrepasa mucho el pie.'],
  ['Prensa de piernas', 'FUERZA', 'Pierna', 'Máquina', 'Pies ancho hombros, sin bloquear rodillas.'],
  ['Curl de bíceps con barra Z', 'FUERZA', 'Bíceps', 'Barra Z', 'Sin balanceo del tronco.'],
  ['Extensiones de tríceps en polea', 'FUERZA', 'Tríceps', 'Polea', 'Codos fijos al costado.'],
  ['Fondos en paralelas', 'FUERZA', 'Pecho/Tríceps', 'Paralelas', 'Inclinación ligera, hombros abajo.'],
  ['Nordic curl', 'FUERZA', 'Isquios', 'Suelo/compañero', 'Descenso controlado excéntrico.'],
  ['Plancha frontal', 'CORE', 'Core', 'Suelo', 'Cadera neutra, glúteos activos.'],
  ['Plancha lateral', 'CORE', 'Core', 'Suelo', 'Cadera elevada, línea recta.'],
  ['Hollow body hold', 'CORE', 'Core', 'Suelo', 'Lumbar pegada al suelo.'],
  ['Pallof press', 'CORE', 'Core', 'Polea/banda', 'Anti-rotación, brazos extendidos.'],
  ['Ab wheel rollout', 'CORE', 'Core', 'Rueda abdominal', 'Rango que puedas controlar.'],
  ['Farmer walk', 'FUERZA', 'Agarre/Core', 'Mancuernas/kettlebells', 'Paso firme, hombros cuadrados.'],
  ['Carrera continua Z2 20 min', 'RESISTENCIA', 'Cardio', '—', 'RPE 4-5, conversación posible.'],
  ['Carrera continua Z2 30 min', 'RESISTENCIA', 'Cardio', '—', 'Ritmo aeróbico base.'],
  ['Carrera continua Z2 40 min', 'RESISTENCIA', 'Cardio', '—', 'Rodaje oposición.'],
  ['Fartlek 25 min', 'RESISTENCIA', 'Cardio', '—', 'Alternar Z2 y Z3 libremente.'],
  ['Series 200 m', 'RESISTENCIA', 'Cardio', 'Pista', 'RPE 8, recuperación caminando.'],
  ['Series 400 m', 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo umbral, R 2 min.'],
  ['Series 800 m', 'RESISTENCIA', 'Cardio', 'Pista', 'VO2 desarrollo, R 3 min.'],
  ['HIIT 30:30 x 12', 'RESISTENCIA', 'Cardio', '—', '30 s fuerte / 30 s suave.'],
  ['HIIT 4x4 min', 'RESISTENCIA', 'Cardio', '—', '4 min al 95 % FCmáx, R 3 min.'],
  ['Test 1000 m', 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo de prueba oficial PN.'],
  ['Test 1600 m', 'RESISTENCIA', 'Cardio', 'Pista', 'Ritmo objetivo oposición.'],
  ['Cuestas 30 m x 8', 'RESISTENCIA', 'Cardio', 'Cuesta 6-8 %', 'Potencia resistida.'],
  ['Bicicleta estática Z2 25 min', 'RESISTENCIA', 'Cardio', 'Bici', 'Alternativa rodaje bajo impacto.'],
  ['Elíptica Z2 20 min', 'RESISTENCIA', 'Cardio', 'Elíptica', 'Recuperación activa.'],
  ['Natación técnica 400 m', 'RESISTENCIA', 'Cardio', 'Piscina', 'Series 50 m técnica.'],
  ['Natación 50 m series x 8', 'RESISTENCIA', 'Cardio', 'Piscina', 'Ritmo prueba inspector/GC.'],
  ['Sprint 30 m x 6', 'VELOCIDAD', 'Velocidad', 'Pista', 'Salida explosiva, R 90 s.'],
  ['Sprint 40 m x 6', 'VELOCIDAD', 'Velocidad', 'Pista', 'Aceleración máxima.'],
  ['Sprint 60 m x 4', 'VELOCIDAD', 'Velocidad', 'Pista', 'Velocidad pura GC.'],
  ['Sprint con resistencia paracaídas', 'VELOCIDAD', 'Velocidad', 'Pista/paracaídas', '20-30 m, técnica alta.'],
  ['Skipping A/B 3x20 m', 'VELOCIDAD', 'Velocidad', 'Pista', 'Activación neuromuscular.'],
  ['Carioca 3x20 m', 'VELOCIDAD', 'Agilidad', 'Pista', 'Cadera baja, pies rápidos.'],
  ['Conos en T 4 vueltas', 'VELOCIDAD', 'Agilidad', 'Conos', 'Cambios 90° y 180°.'],
  ['Circuito de agilidad cronometrado', 'VELOCIDAD', 'Agilidad', 'Conos/vallas', 'Ritmo de prueba oficial.'],
  ['Vallas bajas 4x8', 'VELOCIDAD', 'Agilidad', 'Vallas', 'Ritmo de zancada constante.'],
  ['Saltos al cajón 4x5', 'VELOCIDAD', 'Pliometría', 'Cajón', 'Aterrizaje suave, extensión completa.'],
  ['CMJ 4x5', 'VELOCIDAD', 'Pliometría', '—', 'Countermovement jump máximo.'],
  ['Triple salto 3x3', 'VELOCIDAD', 'Pliometría', '—', 'Contactos mínimos entre saltos.'],
  ['Drop jump 4x5', 'VELOCIDAD', 'Pliometría', 'Cajón 30 cm', 'Caída + rebote inmediato.'],
  ['Lanzamiento balón medicinal 4x6', 'VELOCIDAD', 'Potencia', 'Balón 3-5 kg', 'Explosivo desde pecho.'],
  ['Escalera de coordinación 5 min', 'VELOCIDAD', 'Agilidad', 'Escalera', 'Patrones variados sin mirar pies.'],
  ['Movilidad cadera 8 min', 'MOVILIDAD', 'Movilidad', '—', '90/90, frog stretch, activación glúteo.'],
  ['Movilidad hombro 8 min', 'MOVILIDAD', 'Movilidad', 'Banda', 'Dislocaciones, face pull ligero.'],
  ['RAMP activación 10 min', 'MOVILIDAD', 'Movilidad', '—', 'Raise-Activate-Mobilise-Potentiate.'],
  ['Estiramientos post-entreno 8 min', 'MOVILIDAD', 'Movilidad', '—', 'Isquios, cuádriceps, dorsal.'],
  ['Curl martillo', 'FUERZA', 'Bíceps/Antebrazo', 'Mancuernas', 'Agarre neutro.'],
  ['Wrist curl barra', 'FUERZA', 'Antebrazo', 'Barra', 'Agarre oposición.'],
  ['Colgado en barra máx tiempo', 'FUERZA', 'Agarre/Antebrazo', 'Barra', 'Suspensión activa.'],
  ['Remo en máquina sentado', 'FUERZA', 'Espalda', 'Máquina', 'Pecho apoyado, tirón al abdomen.'],
  ['Pullover en polea', 'FUERZA', 'Espalda/Pecho', 'Polea', 'Brazos rectos, foco dorsal.'],
  ['Gemelos en prensa', 'FUERZA', 'Pantorrilla', 'Prensa', 'Rango completo, pausa arriba.'],
  ['Step mill 15 min', 'RESISTENCIA', 'Cardio', 'Step', 'Intensidad moderada-alta.'],
  ['Battle ropes 30:30 x 10', 'RESISTENCIA', 'Cardio', 'Cuerdas', 'Onda alterna, core firme.'],
  ['Burpees AMRAP 8 min', 'RESISTENCIA', 'Cardio', '—', 'Ritmo sostenible, técnica limpia.'],
  ['Kettlebell swing 4x15', 'FUERZA', 'Cadena posterior', 'Kettlebell', 'Impulso de cadera, brazos relajados.'],
  ['Turkish get-up 3x2/lado', 'FUERZA', 'Cuerpo completo', 'Kettlebell', 'Movimiento lento y controlado.'],
  ['Landmine press 4x8', 'FUERZA', 'Hombros/Core', 'Barra/landmine', 'Press angular unilateral.'],
  ['Sled push 4x20 m', 'VELOCIDAD', 'Potencia', 'Trineo', 'Inclinación baja, máxima intención.']
];

class EjerciciosCatalogoService {
  static async seedCatalogoAmpliado() {
    const [[{ total }]] = await db.query('SELECT COUNT(*) AS total FROM ejercicios');
    if (Number(total) >= 120) return;

    for (const [nombre, pilar, grupo, equip, instr] of EJERCICIOS_EXTRA) {
      const [exists] = await db.query('SELECT id_ejercicio FROM ejercicios WHERE nombre = ? LIMIT 1', [nombre]);
      if (exists.length) continue;
      const categoria = pilar === 'MOVILIDAD' ? 'Movilidad' : pilar === 'CORE' ? 'Core' : 'Fuerza';
      await db.query(
        `INSERT INTO ejercicios (nombre, video_url, instrucciones_tecnicas, categoria, pilar, grupo_muscular, equipamiento)
         VALUES (?, NULL, ?, ?, ?, ?, ?)`,
        [nombre, instr, categoria, pilar, grupo, equip]
      );
    }
    console.log('[ejercicios] Catálogo ampliado actualizado');
  }
}

module.exports = EjerciciosCatalogoService;
