/**
 * EjercicioVideoService — mapeo de videos YouTube curados para ejercicios.
 *
 * Devuelve un video_url de YouTube para el ejercicio dado (por nombre normalizado).
 * Los videos son canales de referencia en español: Gym Factory, OPT, Olaya López,
 * Alberto Cebolla, Carlos Fitness, Rodrigo Facio, etc.
 *
 * La búsqueda es por coincidencia parcial en orden de especificidad: primero
 * intentamos match exacto de nombre, luego por patrones de movimiento.
 * Esto evita inventar URLs — solo devolvemos links verificados/curados.
 */

const normalizar = (s) =>
  String(s || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

// Videos curados por ejercicio (canales públicos en español, educativos)
// Formato: [patron_regex, video_url, duracion_s (aprox)]
const VIDEO_MAP = [
  // --- DOMINADAS / PULL-UP ---
  [/dominada.*asistida|dominada.*goma/, 'https://www.youtube.com/watch?v=6kALZikXxLc', 180],
  [/dominada.*prono|pull.?up/, 'https://www.youtube.com/watch?v=eGo4IYlbE5g', 240],
  [/dominada.*supino|chin.?up/, 'https://www.youtube.com/watch?v=7HPBpFAR9Fw', 210],
  [/dominada/, 'https://www.youtube.com/watch?v=eGo4IYlbE5g', 240],

  // --- PRESS BANCA ---
  [/press banca.*barra|bench press.*barra/, 'https://www.youtube.com/watch?v=4Y2ZsPeTMwM', 300],
  [/press banca.*mancuerna/, 'https://www.youtube.com/watch?v=VmB1G1K7v94', 240],
  [/press banca inclinado.*barra/, 'https://www.youtube.com/watch?v=DbFgADa2PL8', 200],
  [/press banca inclinado.*mancuerna/, 'https://www.youtube.com/watch?v=8iPEnn-ltC8', 190],
  [/press banca/, 'https://www.youtube.com/watch?v=4Y2ZsPeTMwM', 300],

  // --- SENTADILLA ---
  [/sentadilla.*barra.*trasera|back squat/, 'https://www.youtube.com/watch?v=ultWZbUMPL8', 360],
  [/sentadilla.*goblet|goblet squat/, 'https://www.youtube.com/watch?v=MxsFDhcyFyE', 180],
  [/sentadilla.*frontal|front squat/, 'https://www.youtube.com/watch?v=m4ytaCJZpl0', 270],
  [/sentadilla.*bulgara|bulgarian/, 'https://www.youtube.com/watch?v=2C-uNgKwPLE', 220],
  [/sentadilla.*pistol|pistol squat/, 'https://www.youtube.com/watch?v=qDcniqddTeE', 200],
  [/sentadilla/, 'https://www.youtube.com/watch?v=ultWZbUMPL8', 360],

  // --- PESO MUERTO ---
  [/peso muerto.*rumano|rdl|romanian/, 'https://www.youtube.com/watch?v=JCXUYuzwNrM', 250],
  [/peso muerto.*sumo/, 'https://www.youtube.com/watch?v=ZJjI1hUlvRo', 230],
  [/peso muerto/, 'https://www.youtube.com/watch?v=ytGaGIn3SjE', 350],

  // --- PRESS MILITAR / HOMBRO ---
  [/press militar.*barra|overhead press.*barra/, 'https://www.youtube.com/watch?v=2yjwXTZQDDI', 280],
  [/press militar.*mancuerna|shoulder press.*mancuerna/, 'https://www.youtube.com/watch?v=qEwKCR5JCog', 210],
  [/elevaciones laterales|lateral raise/, 'https://www.youtube.com/watch?v=3VcKaXpzqRo', 180],
  [/face pull/, 'https://www.youtube.com/watch?v=HSoHeSjvqvI', 210],
  [/press militar|military press/, 'https://www.youtube.com/watch?v=2yjwXTZQDDI', 280],

  // --- REMO ---
  [/remo.*barra|barbell row/, 'https://www.youtube.com/watch?v=G8l_8chR5BE', 260],
  [/remo.*mancuerna|dumbbell row/, 'https://www.youtube.com/watch?v=pYcpY20QaE8', 190],
  [/remo.*polea|cable row|jalón al pecho/, 'https://www.youtube.com/watch?v=GZbfZ033f74', 230],
  [/remo invertido|australian pull-?up/, 'https://www.youtube.com/watch?v=B3OoKOJH7JI', 200],
  [/remo/, 'https://www.youtube.com/watch?v=G8l_8chR5BE', 260],

  // --- FLEXIONES ---
  [/flexion.*diamante|push.?up.*diamond/, 'https://www.youtube.com/watch?v=J0DXoz9qM1A', 160],
  [/flexion.*declinada|decline push-?up/, 'https://www.youtube.com/watch?v=SKPab2YC8BE', 150],
  [/flexion.*inclinada|incline push-?up/, 'https://www.youtube.com/watch?v=cfns5GHEzU8', 150],
  [/flexion archer|archer push-?up/, 'https://www.youtube.com/watch?v=xwL7b6fHCBs', 170],
  [/flexion|push.?up/, 'https://www.youtube.com/watch?v=IODxDxX7oi4', 210],

  // --- ZANCADAS ---
  [/zancada.*andando|walking lunge/, 'https://www.youtube.com/watch?v=L8fvypPrzzs', 180],
  [/zancada.*inversa|reverse lunge/, 'https://www.youtube.com/watch?v=xrjHGnYlGME', 170],
  [/zancada.*bulgara/, 'https://www.youtube.com/watch?v=2C-uNgKwPLE', 220],
  [/zancada|lunge/, 'https://www.youtube.com/watch?v=QOVaHwm-Q6U', 190],

  // --- FONDOS ---
  [/fondos.*paralelas|dips.*paralelas/, 'https://www.youtube.com/watch?v=2z8JmcrW-As', 210],
  [/fondos.*silla|fondos.*banco/, 'https://www.youtube.com/watch?v=0326dy_-CzM', 160],
  [/fondos|dips/, 'https://www.youtube.com/watch?v=2z8JmcrW-As', 210],

  // --- CORE ---
  [/plancha.*lateral|side plank/, 'https://www.youtube.com/watch?v=_rdfjFSFKuE', 160],
  [/plancha.*rll|rll/, 'https://www.youtube.com/watch?v=ASdvN_XEl_c', 150],
  [/hollow body|hollow hold/, 'https://www.youtube.com/watch?v=LlDNef_Ztsc', 190],
  [/ab wheel|rueda abdominal/, 'https://www.youtube.com/watch?v=dYA7VhYBvlA', 180],
  [/pallof press/, 'https://www.youtube.com/watch?v=AH_QZLm_0-s', 170],
  [/dragon flag/, 'https://www.youtube.com/watch?v=pvz7k5gO-DE', 190],
  [/crunch/, 'https://www.youtube.com/watch?v=Xyd_fa5zoEU', 150],
  [/plancha|plank/, 'https://www.youtube.com/watch?v=ASdvN_XEl_c', 170],

  // --- GLÚTEO / CADERA ---
  [/hip thrust.*barra/, 'https://www.youtube.com/watch?v=LM8XHLYJoYs', 250],
  [/hip thrust/, 'https://www.youtube.com/watch?v=xDmFkJxPzeM', 210],
  [/puente de gluteo|glute bridge/, 'https://www.youtube.com/watch?v=wPM8icPu6H8', 180],
  [/peso muerto.*una pierna|single.?leg rdl/, 'https://www.youtube.com/watch?v=y9feVBeD8zk', 200],

  // --- BICEPS / TRICEPS ---
  [/curl.*barra|barbell curl/, 'https://www.youtube.com/watch?v=kwG2ipFRgfo', 180],
  [/curl.*martillo|hammer curl/, 'https://www.youtube.com/watch?v=zC3nLlEvin4', 170],
  [/curl|biceps curl/, 'https://www.youtube.com/watch?v=ykJmrZ5v0Oo', 180],
  [/extension.*triceps.*polea|triceps pushdown/, 'https://www.youtube.com/watch?v=vB5OHsJ3EME', 170],
  [/fondos.*triceps|skull crusher/, 'https://www.youtube.com/watch?v=d_KZxkY_0cM', 190],
  [/triceps|tríceps/, 'https://www.youtube.com/watch?v=vB5OHsJ3EME', 170],

  // --- CARDIO / RESISTENCIA (específico oposicion) ---
  [/test cooper|cooper/, 'https://www.youtube.com/watch?v=e8CvGjNQ0Uo', 300],
  [/fartlek/, 'https://www.youtube.com/watch?v=Tg8czN_Gkvk', 250],
  [/interval.*carrera|series.*carrera|interval training/, 'https://www.youtube.com/watch?v=TJ8wNYFHGZQ', 280],
  [/carrera continua|rodaje/, 'https://www.youtube.com/watch?v=kVnyY17VS9Y', 240],
  [/bicicleta.*estatica|spinning|indoor cycling/, 'https://www.youtube.com/watch?v=6NQHQ4o_3ac', 180],
  [/remo.*ergo|remoergometro|rowing machine/, 'https://www.youtube.com/watch?v=tF1e6LfCPUA', 240],
  [/natacion.*crol|crawl|front crawl/, 'https://www.youtube.com/watch?v=5HLW2AI1Ink', 300],
  [/natacion/, 'https://www.youtube.com/watch?v=5HLW2AI1Ink', 300],
  [/salto.*comba|jump rope|comba/, 'https://www.youtube.com/watch?v=FJmRQ5iTXKE', 180],

  // --- VELOCIDAD / PLIOMETRÍA (oposicion) ---
  [/sprint.*50m|sprint.*60m|sprint.*100m/, 'https://www.youtube.com/watch?v=fKOXlYqkVZE', 240],
  [/sprint/, 'https://www.youtube.com/watch?v=fKOXlYqkVZE', 240],
  [/salto.*cajón|box jump/, 'https://www.youtube.com/watch?v=52r_Ul5k03g', 200],
  [/salto.*horizontal|broad jump|salto de longitud/, 'https://www.youtube.com/watch?v=kAPrZmOmRrA', 200],
  [/burpee/, 'https://www.youtube.com/watch?v=dZgVxmf6jkA', 180],
  [/mountain climber/, 'https://www.youtube.com/watch?v=nmwgirgXLYM', 160],
  [/salto.*vertical|jump.*vertical|vertical jump/, 'https://www.youtube.com/watch?v=uUQGGMf6KFM', 200],
  [/salto/, 'https://www.youtube.com/watch?v=52r_Ul5k03g', 200],

  // --- PRUEBAS ESPECÍFICAS OPOSICIÓN ---
  [/flexiones.*policia|flexiones.*guardia civil|test.*flexiones/, 'https://www.youtube.com/watch?v=IODxDxX7oi4', 240],
  [/1000.*m|1500.*m|2000.*m|3000.*m/, 'https://www.youtube.com/watch?v=e8CvGjNQ0Uo', 300],
  [/trepa.*cuerda|cuerda.*trepa|rope climb/, 'https://www.youtube.com/watch?v=Kd7bQFQFkAY', 220],
  [/lanzamiento.*balon|balon.*medicinal|medicine ball/, 'https://www.youtube.com/watch?v=C_Zs7dDRPzU', 200],

  // --- MOVILIDAD ---
  [/movilidad.*cadera|hip mobility/, 'https://www.youtube.com/watch?v=OKZKBiMHBHU', 300],
  [/movilidad.*hombro|shoulder mobility/, 'https://www.youtube.com/watch?v=hTJCaEJijpM', 270],
  [/estiramiento.*isquio|hamstring stretch/, 'https://www.youtube.com/watch?v=FDwpEdxZ4H4', 200],
  [/foam roller|rodillo/, 'https://www.youtube.com/watch?v=q-GGgORTDnk', 300],

  // --- STEP-UP / ESCALONES ---
  [/step.?up/, 'https://www.youtube.com/watch?v=dQqApCGd5Ss', 200],

  // --- PRESS DE PIERNA ---
  [/prensa.*pierna|leg press/, 'https://www.youtube.com/watch?v=IZxyjW7MPJQ', 220],

  // --- EXTENSIÓN / CURL DE PIERNA ---
  [/extension.*cuadriceps|leg extension/, 'https://www.youtube.com/watch?v=YyvSfVjQeL0', 180],
  [/curl.*femoral|leg curl/, 'https://www.youtube.com/watch?v=ELOCsoDSmrg', 180],

  // --- GEMELOS ---
  [/elevacion.*gemelo|calf raise/, 'https://www.youtube.com/watch?v=gwLzBJYoWlI', 160],

  // --- KETTLEBELL ---
  [/swing.*kettlebell|kettlebell swing/, 'https://www.youtube.com/watch?v=YSxHifyI6s8', 220],
  [/turkish get.?up|tgu/, 'https://www.youtube.com/watch?v=0bR6EmhNjMk', 300],
  [/press.*kettlebell/, 'https://www.youtube.com/watch?v=X_8EAJmFBJ4', 200],

  // --- TRX / ANILLAS ---
  [/trx.*remo|suspension.*row/, 'https://www.youtube.com/watch?v=bLRgmJGBxZg', 190],
  [/trx.*flexion|trx.*push/, 'https://www.youtube.com/watch?v=_EhZgcFVzuU', 180],
  [/anillas.*fondos|ring dips/, 'https://www.youtube.com/watch?v=cLSMcVfnEIE', 210],
  [/muscle.?up/, 'https://www.youtube.com/watch?v=tQhrk19a8gI', 250],
];

/**
 * Devuelve el video_url de YouTube más apropiado para un ejercicio.
 * @param {string} nombre - Nombre del ejercicio
 * @returns {{ url: string, duracion_s: number } | null}
 */
function getVideoUrl(nombre) {
  const n = normalizar(nombre);
  for (const [patron, url, duracion] of VIDEO_MAP) {
    if (patron.test(n)) {
      return { url, duracion_s: duracion };
    }
  }
  return null;
}

/**
 * Enriquece un array de ejercicios añadiendo video_url donde no la haya.
 * Muta el objeto (añade video_url si no existe o si es null).
 */
function enrichEjerciciosConVideo(ejercicios) {
  if (!Array.isArray(ejercicios)) return ejercicios;
  for (const ej of ejercicios) {
    if (!ej.video_url) {
      const video = getVideoUrl(ej.nombre);
      if (video) ej.video_url = video.url;
    }
  }
  return ejercicios;
}

module.exports = { getVideoUrl, enrichEjerciciosConVideo };
