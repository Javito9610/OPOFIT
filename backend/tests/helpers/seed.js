/**
 * Siembra de datos para los tests E2E: oposiciones, pruebas, baremos, ejercicios,
 * rutinas y muchos usuarios sinteticos.
 */
const bcrypt = require('bcryptjs');

async function seedAll(memDb) {
  const s = memDb.state;
  // 3 oposiciones
  s.oposiciones.push(
    { id_oposicion: 1, nombre: 'Policia Nacional - Escala Basica', incluida_gratis: 1 },
    { id_oposicion: 2, nombre: 'Guardia Civil - Acceso Libre', incluida_gratis: 1 },
    { id_oposicion: 3, nombre: 'Bomberos Madrid', incluida_gratis: 1 }
  );

  // Pruebas oficiales: 100m y dominadas para opo 1, 100m y suspension para opo 2
  s.pruebas_oficiales.push(
    {
      id_pruebas_oficiales: 1,
      nombre_prueba: 'Carrera 100m',
      descripcion: 'Sprint',
      trucos: '',
      mejor_si_es_menor: 1,
      unidad_entrada: 's',
      oposiciones_id_oposicion: 1,
      tipo_baremo: 'PUNTUACION'
    },
    {
      id_pruebas_oficiales: 2,
      nombre_prueba: 'Dominadas',
      descripcion: 'Repeticiones',
      trucos: '',
      mejor_si_es_menor: 0,
      unidad_entrada: 'reps',
      oposiciones_id_oposicion: 1,
      tipo_baremo: 'PUNTUACION'
    },
    {
      id_pruebas_oficiales: 3,
      nombre_prueba: 'Circuito Agilidad',
      descripcion: 'Tiempo',
      trucos: '',
      mejor_si_es_menor: 1,
      unidad_entrada: 's',
      oposiciones_id_oposicion: 1,
      tipo_baremo: 'PUNTUACION'
    },
    {
      id_pruebas_oficiales: 4,
      nombre_prueba: 'Carrera 50m',
      descripcion: 'Sprint corto',
      trucos: '',
      mejor_si_es_menor: 1,
      unidad_entrada: 's',
      oposiciones_id_oposicion: 2,
      tipo_baremo: 'PUNTUACION'
    },
    {
      id_pruebas_oficiales: 5,
      nombre_prueba: 'Suspension barra',
      descripcion: 'Tiempo',
      trucos: '',
      mejor_si_es_menor: 0,
      unidad_entrada: 's',
      oposiciones_id_oposicion: 2,
      tipo_baremo: 'PUNTUACION'
    }
  );
  memDb.nextId.pruebas_oficiales = 6;

  // Baremos HOMBRE y MUJER
  const baremos = [];
  // 100m HOMBRE (1)
  [[11, 10], [12, 8], [13, 6], [14, 4], [15, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 1, genero: 'HOMBRE', marca_valor: v, nota: n })
  );
  [[12, 10], [13, 8], [14, 6], [15, 4], [16, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 1, genero: 'MUJER', marca_valor: v, nota: n })
  );
  // Dominadas HOMBRE (2)
  [[5, 2], [10, 4], [15, 6], [20, 8], [25, 10]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 2, genero: 'HOMBRE', marca_valor: v, nota: n })
  );
  [[3, 2], [6, 4], [10, 6], [14, 8], [18, 10]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 2, genero: 'MUJER', marca_valor: v, nota: n })
  );
  // Circuito (3)
  [[14, 10], [15, 8], [16, 6], [17, 4], [18, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 3, genero: 'HOMBRE', marca_valor: v, nota: n })
  );
  [[15, 10], [16, 8], [17, 6], [18, 4], [19, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 3, genero: 'MUJER', marca_valor: v, nota: n })
  );
  // 50m (4)
  [[6, 10], [7, 8], [8, 6], [9, 4], [10, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 4, genero: 'HOMBRE', marca_valor: v, nota: n })
  );
  [[7, 10], [8, 8], [9, 6], [10, 4], [11, 2]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 4, genero: 'MUJER', marca_valor: v, nota: n })
  );
  // Suspension (5)
  [[20, 2], [30, 4], [40, 6], [50, 8], [60, 10]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 5, genero: 'HOMBRE', marca_valor: v, nota: n })
  );
  [[15, 2], [22, 4], [30, 6], [38, 8], [45, 10]].forEach(([v, n]) =>
    baremos.push({ pruebas_oficiales_id_pruebas_oficiales: 5, genero: 'MUJER', marca_valor: v, nota: n })
  );
  baremos.forEach((b, i) => s.baremos_puntuacion.push({ id_baremo: i + 1, ...b }));
  memDb.nextId.baremos_puntuacion = baremos.length + 1;

  // Ejercicios
  const ejs = [
    { nombre: 'Sprint 30s', categoria: 'CARDIO', pilar: 'VELOCIDAD' },
    { nombre: 'Dominadas pronas', categoria: 'FUERZA', pilar: 'TREN_SUPERIOR' },
    { nombre: 'Press banca', categoria: 'FUERZA', pilar: 'TREN_SUPERIOR' },
    { nombre: 'Carrera continua 5km', categoria: 'CARDIO', pilar: 'RESISTENCIA' },
    { nombre: 'Burpees 1 min', categoria: 'MIXTO', pilar: 'POTENCIA' },
    { nombre: 'Sentadillas', categoria: 'FUERZA', pilar: 'TREN_INFERIOR' }
  ];
  ejs.forEach((e, i) => s.ejercicios.push({ id_ejercicio: i + 1, video_url: null, instrucciones_tecnicas: null, ...e }));
  memDb.nextId.ejercicios = ejs.length + 1;

  // Rutina basica opo 1 HOMBRE
  s.rutinas_opo.push({
    id_rutina_opo: 1,
    enfoque_tipo: 'FUERZA',
    nivel: 'BASICO',
    genero: 'HOMBRE',
    oposiciones_id_oposicion: 1
  });
  memDb.nextId.rutinas_opo = 2;
  s.detalle_rutina_opo.push(
    { id_detalle: 1, rutinas_opo_id_rutina_opo: 1, ejercicios_id_ejercicio: 2, series: 3, repeticiones: 8, descanso: 60 },
    { id_detalle: 2, rutinas_opo_id_rutina_opo: 1, ejercicios_id_ejercicio: 3, series: 4, repeticiones: 10, descanso: 90 }
  );

  // Generar muchos usuarios
  const nombres = ['Sofia', 'Lucas', 'Marta', 'Hugo', 'Ines', 'Pablo', 'Sara', 'Carlos', 'Aitana', 'Alvaro'];
  const apellidos = ['Garcia', 'Lopez', 'Martin', 'Sanchez', 'Perez', 'Gonzalez', 'Rodriguez', 'Fernandez'];
  const hash = await bcrypt.hash('Password123!', 4);
  for (let i = 0; i < 30; i++) {
    const id = memDb.nextId.usuarios++;
    const nombre = `${nombres[i % nombres.length]} ${apellidos[(i * 3) % apellidos.length]}`;
    const genero = i % 2 === 0 ? 'HOMBRE' : 'MUJER';
    const peso = 60 + (i % 30);
    const altura = 160 + (i % 30);
    const imc = Number((peso / (altura / 100) ** 2).toFixed(2));
    s.usuarios.push({
      id_usuario: id,
      nombre,
      email: `user${id}@opofit.test`,
      password: hash,
      genero,
      peso,
      altura,
      imc,
      fecha_registro: new Date(),
      oposiciones_id_oposicion: (i % 3) + 1,
      es_premium: i % 5 === 0 ? 1 : 0,
      premium_hasta: null,
      perfil_publico: i % 2 === 0 ? 1 : 0,
      hora_recordatorio_entreno: '18:00:00',
      recordatorio_entreno_activo: 0
    });
    s.settings.push({
      id_setting: memDb.nextId.settings++,
      unidad_peso: 'kg',
      unidad_distancia: 'km',
      usuarios_id_usuario: id
    });
  }
}

module.exports = { seedAll };
