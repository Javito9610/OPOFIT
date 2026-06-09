const EjerciciosService = require('../services/EjerciciosService');

// Catálogo de material soportado. Lo consume la UI de Ajustes para mostrar
// los checkboxes en orden coherente y con etiquetas legibles.
const CATALOGO_MATERIAL = [
  { id: 'NADA',              label: 'Solo peso corporal',     icono: '🧍' },
  { id: 'BARRA_DOMINADAS',   label: 'Barra de dominadas',     icono: '🏋️' },
  { id: 'BARRA_OLIMPICA',    label: 'Barra olímpica + discos', icono: '🏋️‍♀️' },
  { id: 'MANCUERNAS',        label: 'Mancuernas',             icono: '💪' },
  { id: 'KB',                label: 'Kettlebells',            icono: '🔔' },
  { id: 'TRX',               label: 'TRX / suspensión',       icono: '🪢' },
  { id: 'ANILLAS',           label: 'Anillas',                icono: '⭕' },
  { id: 'GOMAS',             label: 'Gomas elásticas',        icono: '🎀' },
  { id: 'COMBA',             label: 'Comba',                  icono: '➰' },
  { id: 'SACO',              label: 'Saco de boxeo',          icono: '🥊' },
  { id: 'FOAM',              label: 'Foam roller',            icono: '🛢️' },
  { id: 'BANCO',             label: 'Banco regulable',        icono: '🪑' },
  { id: 'CAJA',              label: 'Caja pliométrica',       icono: '📦' },
  { id: 'BICI',              label: 'Bicicleta',              icono: '🚴' },
  { id: 'REMO',              label: 'Remoergómetro Concept2', icono: '🚣' },
  { id: 'ECHO_BIKE',         label: 'Echo / Assault bike',    icono: '💨' },
  { id: 'SKI_ERG',           label: 'Ski Erg',                icono: '🎿' },
  { id: 'PISCINA',           label: 'Piscina',                icono: '🏊' },
  { id: 'PISTA',             label: 'Pista de atletismo',     icono: '🏃' },
  { id: 'MONTANA',           label: 'Montaña / trail',        icono: '⛰️' },
  { id: 'GIMNASIO_COMPLETO', label: 'Gimnasio completo',      icono: '🏟️' }
];

const listarEjercicios = async (req, res) => {
  try {
    const { categoria, pilar, busqueda, limite, entorno, grupo_muscular, modalidad, material } = req.query;
    const ejercicios = await EjerciciosService.listarTodos({
      categoria,
      pilar,
      busqueda,
      limite,
      entorno,
      grupo_muscular,
      modalidad,           // wod | calistenia | emom | amrap | tabata | ...
      material             // CSV: KB,MANCUERNAS,GOMAS → filtra por equipamiento
    });
    res.status(200).json({
      ok: true,
      data: ejercicios
    });
  } catch (error) {
    console.error("Error al listar ejercicios:", error);
    res.status(500).json({
      ok: false,
      msg: "Error al obtener los ejercicios"
    });
  }
};

const listarMaterial = async (_req, res) => {
  res.status(200).json({ ok: true, data: CATALOGO_MATERIAL });
};

module.exports = {
  listarEjercicios,
  listarMaterial
};
