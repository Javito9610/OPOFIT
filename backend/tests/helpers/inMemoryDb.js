/**
 * Mock en memoria del pool MySQL2 con tablas y SQL parser minimo.
 *
 * Soporta los subconjuntos de SQL que usa OpoFit: SELECT/INSERT/UPDATE/DELETE
 * con expresiones simples ?=, LIKE, JOIN, COUNT, MAX, GROUP BY, ORDER BY, LIMIT, OFFSET.
 *
 * El objetivo no es ser un MySQL completo sino soportar las queries del backend
 * para tests E2E sobre el grafo de objetos del juego (usuarios, oposiciones, etc).
 *
 * Estrategia: para cada SQL conocida del backend la detectamos por palabras clave
 * y ejecutamos el plan especifico contra el estado en memoria.
 */

const state = {
  usuarios: [],
  oposiciones: [],
  pruebas_oficiales: [],
  baremos_puntuacion: [],
  marcas_perfil: [],
  ejercicios: [],
  rutinas_opo: [],
  detalle_rutina_opo: [],
  rutinas_pers: [],
  detalle_rutina_pers: [],
  settings: [],
  historial_sesiones: [],
  registro_resultados: [],
  simulacros: [],
  simulacro_pruebas: [],
  amistades: [],
  mensajes_chat: [],
  grupos_comunidad: [],
  grupo_miembros: [],
  grupo_mensajes: [],
  quedadas: [],
  gps_actividades: [],
  actividad_posts: [],
  post_likes: [],
  post_comentarios: [],
  segmentos: [],
  segmento_esfuerzos: [],
  notif_tokens: [],
  rutinas_compartidas: [],
  plan_dias: [],
  planes_entrenamiento: [],
  plan_dia_ejercicios: [],
  planes_generados_cache: [],
  banco_dias: [],
  fcm_tokens: [],
  bbb_seed: false
};

let nextId = {
  usuarios: 1,
  pruebas_oficiales: 1,
  marcas_perfil: 1,
  ejercicios: 1,
  rutinas_opo: 1,
  detalle_rutina_opo: 1,
  rutinas_pers: 1,
  detalle_rutina_pers: 1,
  settings: 1,
  historial_sesiones: 1,
  registro_resultados: 1,
  simulacros: 1,
  simulacro_pruebas: 1,
  amistades: 1,
  mensajes_chat: 1,
  grupos_comunidad: 1,
  grupo_miembros: 1,
  grupo_mensajes: 1,
  quedadas: 1,
  gps_actividades: 1,
  actividad_posts: 1,
  post_likes: 1,
  post_comentarios: 1,
  segmentos: 1,
  segmento_esfuerzos: 1,
  baremos_puntuacion: 1
};

function reset() {
  for (const k of Object.keys(state)) state[k] = Array.isArray(state[k]) ? [] : false;
  Object.keys(nextId).forEach((k) => (nextId[k] = 1));
}

function makeUser(overrides = {}) {
  const id = nextId.usuarios++;
  const u = {
    id_usuario: id,
    nombre: `User ${id}`,
    email: `u${id}@opofit.test`,
    password: 'hashed',
    genero: 'HOMBRE',
    peso: 70,
    altura: 175,
    imc: 22.86,
    fecha_registro: new Date(),
    oposiciones_id_oposicion: null,
    es_premium: 0,
    premium_hasta: null,
    perfil_publico: 1,
    hora_recordatorio_entreno: '18:00:00',
    recordatorio_entreno_activo: 0,
    entorno_entreno: null,
    plan_variacion_seed: 0,
    avatar_url: null,
    modo_uso: 'OPOSITOR',
    ubicacion_lat: null,
    ubicacion_lng: null,
    ubicacion_visible: 0,
    ubicacion_actualizada: null,
    ...overrides
  };
  state.usuarios.push(u);
  return u;
}

function bytSql(sql) {
  return sql.replace(/\s+/g, ' ').trim();
}

function query(sql, params = []) {
  const s = bytSql(sql).toLowerCase();
  // ---------- USUARIOS ----------
  if (s.startsWith('select * from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ ...u }] : [], []]);
  }
  if (s.startsWith("select * from usuarios where lower(trim(email))")) {
    const email = String(params[0] || '').toLowerCase().trim();
    const u = state.usuarios.find((x) => x.email.toLowerCase().trim() === email);
    return Promise.resolve([u ? [{ ...u }] : [], []]);
  }
  if (s.startsWith('select id_usuario from usuarios where lower(trim(email))')) {
    const email = String(params[0] || '').toLowerCase().trim();
    const u = state.usuarios.find((x) => x.email.toLowerCase().trim() === email);
    return Promise.resolve([u ? [{ id_usuario: u.id_usuario }] : [], []]);
  }
  if (s.startsWith('select genero from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ genero: u.genero }] : [], []]);
  }
  if (s.startsWith('select genero, peso, altura from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ genero: u.genero, peso: u.peso, altura: u.altura }] : [], []]);
  }
  if (s.startsWith('select entorno_entreno, plan_variacion_seed from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([
      u ? [{ entorno_entreno: u.entorno_entreno, plan_variacion_seed: u.plan_variacion_seed || 0 }] : [],
      []
    ]);
  }
  if (s.startsWith('update usuarios set entorno_entreno = ? where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.entorno_entreno = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set plan_variacion_seed = coalesce(plan_variacion_seed, 0) + 1')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    if (u) u.plan_variacion_seed = Number(u.plan_variacion_seed || 0) + 1;
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('select id_usuario from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ id_usuario: u.id_usuario }] : [], []]);
  }
  if (s.startsWith('select altura from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ altura: u.altura }] : [], []]);
  }
  if (s.startsWith('select peso from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ peso: u.peso }] : [], []]);
  }
  if (s.startsWith('select es_premium, premium_hasta, oposiciones_id_oposicion from usuarios')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ es_premium: u.es_premium, premium_hasta: u.premium_hasta, oposiciones_id_oposicion: u.oposiciones_id_oposicion }] : [], []]);
  }
  if (s.startsWith('select oposiciones_id_oposicion from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ oposiciones_id_oposicion: u.oposiciones_id_oposicion }] : [], []]);
  }
  if (s.startsWith('select id_usuario, oposiciones_id_oposicion from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ id_usuario: u.id_usuario, oposiciones_id_oposicion: u.oposiciones_id_oposicion }] : [], []]);
  }
  if (s.startsWith('select nombre, perfil_publico, oposiciones_id_oposicion from usuarios')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ nombre: u.nombre, perfil_publico: u.perfil_publico, oposiciones_id_oposicion: u.oposiciones_id_oposicion }] : [], []]);
  }
  if (s.startsWith('select password from usuarios where id_usuario')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ password: u.password }] : [], []]);
  }
  if (s.startsWith('update usuarios set password = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.password = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('select u.nombre, u.email, u.peso, u.altura, u.imc, u.avatar_url, u.modo_uso')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    if (!u) return Promise.resolve([[], []]);
    const o = state.oposiciones.find((x) => x.id_oposicion === u.oposiciones_id_oposicion);
    return Promise.resolve([[{
      nombre: u.nombre,
      email: u.email,
      peso: u.peso,
      altura: u.altura,
      imc: u.imc,
      avatar_url: u.avatar_url,
      modo_uso: u.modo_uso,
      oposicionId: u.oposiciones_id_oposicion,
      oposicionNombre: o?.nombre || null,
      ubicacion_visible: u.ubicacion_visible
    }], []]);
  }
  if (s.startsWith('update usuarios set nombre = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.nombre = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set avatar_url = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.avatar_url = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set modo_uso = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.modo_uso = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set oposiciones_id_oposicion = null')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    if (u) u.oposiciones_id_oposicion = null;
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set ubicacion_visible = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.ubicacion_visible = Number(params[0]);
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set ubicacion_lat = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[3]));
    if (u) {
      u.ubicacion_lat = Number(params[0]);
      u.ubicacion_lng = Number(params[1]);
      u.ubicacion_visible = Number(params[2]);
      u.ubicacion_actualizada = new Date();
    }
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.includes('select u.id_usuario, u.nombre, u.modo_uso, u.avatar_url') && s.includes('distanciam')) {
    const latN = Number(params[0]);
    const lngN = Number(params[1]);
    const userId = Number(params[3]);
    const radioM = Number(params[4]);
    const dist = (a, b, c, d) => {
      const R = 6371000;
      const toRad = (x) => (x * Math.PI) / 180;
      const dLat = toRad(c - a);
      const dLng = toRad(d - b);
      const h = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(a)) * Math.cos(toRad(c)) * Math.sin(dLng / 2) ** 2;
      return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    };
    const rows = state.usuarios
      .filter((u) =>
        u.id_usuario !== userId &&
        u.ubicacion_visible === 1 &&
        u.ubicacion_lat != null &&
        u.ubicacion_lng != null
      )
      .map((u) => {
        const d = dist(latN, lngN, Number(u.ubicacion_lat), Number(u.ubicacion_lng));
        const o = state.oposiciones.find((x) => x.id_oposicion === u.oposiciones_id_oposicion);
        return {
          id_usuario: u.id_usuario,
          nombre: u.nombre,
          modo_uso: u.modo_uso,
          avatar_url: u.avatar_url,
          ubicacion_lat: u.ubicacion_lat,
          ubicacion_lng: u.ubicacion_lng,
          oposicion_nombre: o?.nombre || null,
          distanciaM: d
        };
      })
      .filter((r) => r.distanciaM <= radioM)
      .sort((a, b) => a.distanciaM - b.distanciaM)
      .slice(0, 50);
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select oposiciones_id_oposicion, modo_uso, genero from usuarios')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([
      u
        ? [{
            oposiciones_id_oposicion: u.oposiciones_id_oposicion,
            modo_uso: u.modo_uso,
            genero: u.genero
          }]
        : [],
      []
    ]);
  }
  if (s.startsWith('select id_usuario, oposiciones_id_oposicion, modo_uso from usuarios')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ id_usuario: u.id_usuario, oposiciones_id_oposicion: u.oposiciones_id_oposicion, modo_uso: u.modo_uso }] : [], []]);
  }
  if (s.startsWith('select oposiciones_id_oposicion, modo_uso from usuarios')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[0]));
    return Promise.resolve([u ? [{ oposiciones_id_oposicion: u.oposiciones_id_oposicion, modo_uso: u.modo_uso }] : [], []]);
  }
  if (s.startsWith('insert into usuarios')) {
    const hasModo = params.length >= 9;
    const nombre = params[0];
    const email = params[1];
    const password = params[2];
    const genero = params[3];
    const peso = params[4];
    const altura = params[5];
    const imc = params[6];
    const opo = params[7];
    const modo = hasModo ? params[8] : 'OPOSITOR';
    // Enforzamos UNIQUE en email (igual que la BBDD real).
    if (state.usuarios.some((u) => u.email && String(u.email).toLowerCase().trim() === String(email).toLowerCase().trim())) {
      const err = new Error("ER_DUP_ENTRY: Duplicate entry '" + email + "' for key 'email'");
      err.code = 'ER_DUP_ENTRY';
      return Promise.reject(err);
    }
    const id = nextId.usuarios++;
    state.usuarios.push({
      id_usuario: id,
      nombre,
      email,
      password,
      genero: genero || 'HOMBRE',
      peso: Number(peso || 0),
      altura: Number(altura || 0),
      imc: Number(imc || 0),
      fecha_registro: new Date(),
      oposiciones_id_oposicion: opo == null ? null : Number(opo),
      modo_uso: hasModo ? (modo || 'OPOSITOR') : 'OPOSITOR',
      es_premium: 0,
      premium_hasta: null,
      perfil_publico: 1,
      avatar_url: null,
      ubicacion_lat: null,
      ubicacion_lng: null,
      ubicacion_visible: 0,
      ubicacion_actualizada: null
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('update usuarios set peso = ?, altura = ?, imc = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[3]));
    if (u) { u.peso = params[0]; u.altura = params[1]; u.imc = params[2]; }
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set peso = ?, imc = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[2]));
    if (u) { u.peso = params[0]; u.imc = params[1]; }
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set altura = ?, imc = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[2]));
    if (u) { u.altura = params[0]; u.imc = params[1]; }
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set peso = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.peso = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set altura = ?')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.altura = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set perfil_publico')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.perfil_publico = Number(params[0]);
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set es_premium = 1, premium_hasta')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) { u.es_premium = 1; u.premium_hasta = params[0]; }
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set hora_recordatorio_entreno')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.hora_recordatorio_entreno = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('update usuarios set recordatorio_entreno_activo')) {
    const u = state.usuarios.find((x) => x.id_usuario === Number(params[1]));
    if (u) u.recordatorio_entreno_activo = params[0];
    return Promise.resolve([{ affectedRows: u ? 1 : 0 }, []]);
  }
  if (s.startsWith('delete from usuarios where id_usuario')) {
    const id = Number(params[0]);
    const before = state.usuarios.length;
    state.usuarios = state.usuarios.filter((u) => u.id_usuario !== id);
    return Promise.resolve([{ affectedRows: before - state.usuarios.length }, []]);
  }

  // ---------- OPOSICIONES ----------
  if (s.startsWith('select * from oposiciones')) {
    return Promise.resolve([state.oposiciones.slice(), []]);
  }
  if (s.startsWith('select id_oposicion from oposiciones where id_oposicion')) {
    const o = state.oposiciones.find((x) => x.id_oposicion === Number(params[0]));
    return Promise.resolve([o ? [{ id_oposicion: o.id_oposicion }] : [], []]);
  }
  if (s.startsWith('select nombre from oposiciones where id_oposicion')) {
    const o = state.oposiciones.find((x) => x.id_oposicion === Number(params[0]));
    return Promise.resolve([o ? [{ nombre: o.nombre }] : [], []]);
  }

  // ---------- SETTINGS ----------
  if (s.startsWith('insert into settings')) {
    const id = nextId.settings++;
    state.settings.push({
      id_setting: id,
      unidad_peso: params[0],
      unidad_distancia: params[1],
      usuarios_id_usuario: Number(params[2])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('update settings set unidad_peso')) {
    const st = state.settings.find((x) => x.usuarios_id_usuario === Number(params[2]));
    if (st) { st.unidad_peso = params[0]; st.unidad_distancia = params[1]; }
    return Promise.resolve([{ affectedRows: st ? 1 : 0 }, []]);
  }
  if (s.startsWith('delete from settings where usuarios_id_usuario')) {
    const id = Number(params[0]);
    state.settings = state.settings.filter((x) => x.usuarios_id_usuario !== id);
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }

  // ---------- PRUEBAS OFICIALES ----------
  if (s.startsWith('select count(*) as total from pruebas_oficiales')) {
    const n = state.pruebas_oficiales.filter((p) => p.oposiciones_id_oposicion === Number(params[0])).length;
    return Promise.resolve([[{ total: n }], []]);
  }
  if (s.startsWith('select count(*) as total_pruebas from pruebas_oficiales')) {
    const n = state.pruebas_oficiales.filter((p) => p.oposiciones_id_oposicion === Number(params[0])).length;
    return Promise.resolve([[{ total_pruebas: n }], []]);
  }
  if (s.startsWith('select count(*) as n from pruebas_oficiales')) {
    const n = state.pruebas_oficiales.filter((p) => p.oposiciones_id_oposicion === Number(params[0])).length;
    return Promise.resolve([[{ n }], []]);
  }
  if (s.startsWith('select mejor_si_es_menor from pruebas_oficiales')) {
    const p = state.pruebas_oficiales.find((x) => x.id_pruebas_oficiales === Number(params[0]));
    return Promise.resolve([p ? [{ mejor_si_es_menor: p.mejor_si_es_menor }] : [], []]);
  }
  if (s.startsWith('select nombre_prueba, mejor_si_es_menor, unidad_entrada from pruebas_oficiales')) {
    const p = state.pruebas_oficiales.find((x) => x.id_pruebas_oficiales === Number(params[0]));
    return Promise.resolve([p ? [{ nombre_prueba: p.nombre_prueba, mejor_si_es_menor: p.mejor_si_es_menor, unidad_entrada: p.unidad_entrada }] : [], []]);
  }
  if (s.startsWith('select id_pruebas_oficiales, nombre_prueba, descripcion, mejor_si_es_menor')) {
    const idOpo = Number(params[0]);
    const list = state.pruebas_oficiales
      .filter((p) => p.oposiciones_id_oposicion === idOpo)
      .sort((a, b) => a.id_pruebas_oficiales - b.id_pruebas_oficiales);
    return Promise.resolve([list, []]);
  }

  // ---------- BAREMOS ----------
  if (s.startsWith('select marca_valor, nota from baremos_puntuacion')) {
    const list = state.baremos_puntuacion
      .filter((b) => b.pruebas_oficiales_id_pruebas_oficiales === Number(params[0]) && b.genero === params[1])
      .sort((a, b) => a.marca_valor - b.marca_valor);
    return Promise.resolve([list, []]);
  }

  // ---------- MARCAS PERFIL ----------
  if (s.startsWith('select m.id_marcas_perfil, m.valord_record, m.fecha_logro, p.id_pruebas_oficiales, p.nombre_prueba, p.mejor_si_es_menor, p.unidad_entrada from marcas_perfil')) {
    const userId = Number(params[0]);
    const idOpo = Number(params[1]);
    const filas = state.marcas_perfil
      .filter((m) => m.usuarios_id_usuario === userId)
      .map((m) => {
        const p = state.pruebas_oficiales.find((x) => x.id_pruebas_oficiales === m.pruebas_oficiales_id_pruebas_oficiales);
        return p && p.oposiciones_id_oposicion === idOpo
          ? {
              id_marcas_perfil: m.id_marcas_perfil,
              valord_record: m.valord_record,
              fecha_logro: m.fecha_logro,
              id_pruebas_oficiales: p.id_pruebas_oficiales,
              nombre_prueba: p.nombre_prueba,
              mejor_si_es_menor: p.mejor_si_es_menor,
              unidad_entrada: p.unidad_entrada
            }
          : null;
      })
      .filter(Boolean)
      .sort((a, b) =>
        a.id_pruebas_oficiales - b.id_pruebas_oficiales ||
        new Date(b.fecha_logro) - new Date(a.fecha_logro) ||
        b.id_marcas_perfil - a.id_marcas_perfil
      );
    return Promise.resolve([filas, []]);
  }
  if (s.startsWith('insert into marcas_perfil')) {
    const id = nextId.marcas_perfil++;
    state.marcas_perfil.push({
      id_marcas_perfil: id,
      usuarios_id_usuario: Number(params[0]),
      pruebas_oficiales_id_pruebas_oficiales: Number(params[1]),
      valord_record: Number(params[2]),
      fecha_logro: new Date()
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('update marcas_perfil set valord_record')) {
    const userId = Number(params[1]);
    const idPrueba = Number(params[2]);
    const m = state.marcas_perfil.find(
      (x) => x.usuarios_id_usuario === userId && x.pruebas_oficiales_id_pruebas_oficiales === idPrueba
    );
    if (m) { m.valord_record = Number(params[0]); m.fecha_logro = new Date(); }
    return Promise.resolve([{ affectedRows: m ? 1 : 0 }, []]);
  }
  if (s.startsWith('delete mp from marcas_perfil mp')) {
    // Mantener solo la mas reciente por usuario+prueba
    const userId = Number(params[0]);
    const idPrueba = Number(params[1]);
    const filas = state.marcas_perfil.filter(
      (m) => m.usuarios_id_usuario === userId && m.pruebas_oficiales_id_pruebas_oficiales === idPrueba
    );
    if (filas.length > 1) {
      const keep = filas.reduce((a, b) => (a.id_marcas_perfil > b.id_marcas_perfil ? a : b));
      state.marcas_perfil = state.marcas_perfil.filter(
        (m) => !(m.usuarios_id_usuario === userId && m.pruebas_oficiales_id_pruebas_oficiales === idPrueba && m !== keep)
      );
    }
    return Promise.resolve([{ affectedRows: 0 }, []]);
  }
  if (s.startsWith('delete from marcas_perfil where usuarios_id_usuario')) {
    const id = Number(params[0]);
    state.marcas_perfil = state.marcas_perfil.filter((m) => m.usuarios_id_usuario !== id);
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }

  // ---------- HISTORIAL SESIONES ----------
  if (s.startsWith('insert into historial_sesiones')) {
    const id = nextId.historial_sesiones++;
    const tipoRutina = params[0];
    state.historial_sesiones.push({
      id_historial_sesion: id,
      fecha_entreno: new Date(),
      tipo_rutina: tipoRutina,
      duracion_oficial: Number(params[1]),
      usuarios_id_usuario: Number(params[2]),
      rutinas_opo_id_rutina_opo: tipoRutina === 'OPO' ? Number(params[3]) : null,
      rutinas_pers_id_rutina_pers: tipoRutina === 'PERS' ? Number(params[3]) : null,
      gps_actividad_uuid: params[4] || null
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('insert into registro_resultados')) {
    const id = nextId.registro_resultados++;
    state.registro_resultados.push({
      id_registro: id,
      ejercicios_id_ejercicio: Number(params[0]),
      historial_sesiones_id_historial_sesiones: Number(params[1]),
      valor_conseguido: Number(params[2])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('select id_historial_sesion from historial_sesiones where usuarios_id_usuario')) {
    const rows = state.historial_sesiones
      .filter((h) => h.usuarios_id_usuario === Number(params[0]))
      .map((h) => ({ id_historial_sesion: h.id_historial_sesion }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('delete from historial_sesiones where usuarios_id_usuario')) {
    const id = Number(params[0]);
    state.historial_sesiones = state.historial_sesiones.filter((h) => h.usuarios_id_usuario !== id);
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete from registro_resultados where historial_sesiones_id_historial_sesiones in')) {
    state.registro_resultados = state.registro_resultados.filter(
      (r) => !params.includes(r.historial_sesiones_id_historial_sesiones)
    );
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }
  if (s.startsWith('select count(*) as sesiones, coalesce(sum(duracion_oficial)')) {
    const userId = Number(params[0]);
    const sesiones = state.historial_sesiones.filter((h) => h.usuarios_id_usuario === userId);
    return Promise.resolve([[{ sesiones: sesiones.length, minutos: sesiones.reduce((a, b) => a + Number(b.duracion_oficial || 0), 0) }], []]);
  }
  if (s.startsWith('select count(*) as sesiones from historial_sesiones')) {
    const n = state.historial_sesiones.filter((h) => h.usuarios_id_usuario === Number(params[0])).length;
    return Promise.resolve([[{ sesiones: n }], []]);
  }
  if (s.startsWith('select distinct date(fecha_entreno) as d from historial_sesiones')) {
    const userId = Number(params[0]);
    const set = new Set();
    for (const h of state.historial_sesiones) {
      if (h.usuarios_id_usuario === userId) {
        const d = new Date(h.fecha_entreno);
        d.setHours(0, 0, 0, 0);
        set.add(d.getTime());
      }
    }
    const rows = [...set].sort((a, b) => b - a).slice(0, 90).map((t) => ({ d: new Date(t) }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select fecha_entreno, duracion_oficial, tipo_rutina from historial_sesiones')) {
    const userId = Number(params[0]);
    const arr = state.historial_sesiones
      .filter((h) => h.usuarios_id_usuario === userId)
      .sort((a, b) => new Date(b.fecha_entreno) - new Date(a.fecha_entreno))
      .slice(0, 1);
    return Promise.resolve([arr.map((h) => ({ fecha_entreno: h.fecha_entreno, duracion_oficial: h.duracion_oficial, tipo_rutina: h.tipo_rutina })), []]);
  }
  if (s.startsWith('select h.id_historial_sesion, h.fecha_entreno, h.tipo_rutina')) {
    const userId = Number(params[0]);
    const rows = state.historial_sesiones
      .filter((h) => h.usuarios_id_usuario === userId)
      .sort((a, b) => new Date(b.fecha_entreno) - new Date(a.fecha_entreno))
      .map((h) => ({
        id_historial_sesion: h.id_historial_sesion,
        fecha_entreno: h.fecha_entreno,
        tipo_rutina: h.tipo_rutina,
        duracion_oficial: h.duracion_oficial,
        rutinas_opo_id_rutina_opo: h.rutinas_opo_id_rutina_opo,
        rutinas_pers_id_rutina_pers: h.rutinas_pers_id_rutina_pers
      }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select r.valor_conseguido, e.nombre as nombre_ejercicio from registro_resultados')) {
    const userId = Number(params[0]);
    const idEj = Number(params[1]);
    const ses = state.historial_sesiones
      .filter((h) => h.usuarios_id_usuario === userId)
      .map((h) => h.id_historial_sesion);
    const r = state.registro_resultados
      .filter((x) => x.ejercicios_id_ejercicio === idEj && ses.includes(x.historial_sesiones_id_historial_sesiones))
      .sort((a, b) => b.id_registro - a.id_registro)
      .slice(0, 1)
      .map((x) => ({
        valor_conseguido: x.valor_conseguido,
        nombre_ejercicio: state.ejercicios.find((e) => e.id_ejercicio === idEj)?.nombre
      }));
    return Promise.resolve([r, []]);
  }
  if (s.startsWith('select date(fecha_entreno) as dia, count(*) as sesiones')) {
    return Promise.resolve([[], []]);
  }
  if (s.startsWith('update historial_sesiones set rutinas_pers_id_rutina_pers = null')) {
    state.historial_sesiones.forEach((h) => {
      if (h.rutinas_pers_id_rutina_pers === Number(params[0])) h.rutinas_pers_id_rutina_pers = null;
    });
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }

  // ---------- RUTINAS PERS ----------
  if (s.startsWith('select id_rutina_pers from rutinas_pers')) {
    if (s.includes('usuarios_id_usuario = ? and nombre_personalizado')) {
      const r = state.rutinas_pers.find(
        (x) => x.usuarios_id_usuario === Number(params[0]) && x.nombre_personalizado === params[1]
      );
      return Promise.resolve([r ? [{ id_rutina_pers: r.id_rutina_pers }] : [], []]);
    }
    if (s.includes('id_rutina_pers = ? and usuarios_id_usuario')) {
      const r = state.rutinas_pers.find(
        (x) => x.id_rutina_pers === Number(params[0]) && x.usuarios_id_usuario === Number(params[1])
      );
      return Promise.resolve([r ? [{ id_rutina_pers: r.id_rutina_pers }] : [], []]);
    }
  }
  if (s.startsWith('insert into rutinas_pers')) {
    const id = nextId.rutinas_pers++;
    state.rutinas_pers.push({
      id_rutina_pers: id,
      nombre_personalizado: params[0],
      usuarios_id_usuario: Number(params[1])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('insert into detalle_rutina_pers')) {
    const id = nextId.detalle_rutina_pers++;
    state.detalle_rutina_pers.push({
      id_detalle: id,
      rutinas_pers_id_rutina_pers: Number(params[0]),
      ejercicios_id_ejercicio: Number(params[1]),
      series: params[2],
      repeticiones: params[3]
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('select r.id_rutina_pers, r.nombre_personalizado')) {
    const userId = Number(params[0]);
    const rs = state.rutinas_pers.filter((x) => x.usuarios_id_usuario === userId);
    const rows = [];
    for (const r of rs) {
      const detalles = state.detalle_rutina_pers.filter((d) => d.rutinas_pers_id_rutina_pers === r.id_rutina_pers);
      if (detalles.length === 0) {
        rows.push({
          id_rutina_pers: r.id_rutina_pers,
          nombre_personalizado: r.nombre_personalizado,
          ejercicios_id_ejercicio: null,
          nombre_ejercicio: null,
          series: null,
          repeticiones: null,
          descanso: null
        });
      } else {
        for (const d of detalles) {
          const e = state.ejercicios.find((x) => x.id_ejercicio === d.ejercicios_id_ejercicio);
          rows.push({
            id_rutina_pers: r.id_rutina_pers,
            nombre_personalizado: r.nombre_personalizado,
            ejercicios_id_ejercicio: d.ejercicios_id_ejercicio,
            nombre_ejercicio: e?.nombre,
            series: d.series,
            repeticiones: d.repeticiones,
            descanso: null
          });
        }
      }
    }
    return Promise.resolve([rows.sort((a, b) => a.id_rutina_pers - b.id_rutina_pers), []]);
  }
  if (s.startsWith('delete from rutinas_compartidas')) {
    return Promise.resolve([{ affectedRows: 0 }, []]);
  }
  if (s.startsWith('delete from detalle_rutina_pers where rutinas_pers_id_rutina_pers')) {
    state.detalle_rutina_pers = state.detalle_rutina_pers.filter((d) => d.rutinas_pers_id_rutina_pers !== Number(params[0]));
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete drp from detalle_rutina_pers drp')) {
    const userId = Number(params[0]);
    const rIds = state.rutinas_pers.filter((r) => r.usuarios_id_usuario === userId).map((r) => r.id_rutina_pers);
    state.detalle_rutina_pers = state.detalle_rutina_pers.filter((d) => !rIds.includes(d.rutinas_pers_id_rutina_pers));
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete from rutinas_pers where id_rutina_pers = ? and usuarios_id_usuario')) {
    state.rutinas_pers = state.rutinas_pers.filter(
      (r) => !(r.id_rutina_pers === Number(params[0]) && r.usuarios_id_usuario === Number(params[1]))
    );
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete from rutinas_pers where usuarios_id_usuario')) {
    state.rutinas_pers = state.rutinas_pers.filter((r) => r.usuarios_id_usuario !== Number(params[0]));
    return Promise.resolve([{ affectedRows: 1 }, []]);
  }

  // ---------- RUTINAS OPO ----------
  if (s.startsWith('select id_rutina_opo, enfoque_tipo, nivel from rutinas_opo')) {
    const filas = state.rutinas_opo.filter(
      (r) =>
        r.oposiciones_id_oposicion === Number(params[0]) &&
        r.nivel === params[1] &&
        r.genero === params[2]
    );
    return Promise.resolve([filas, []]);
  }
  if (s.startsWith('select e.id_ejercicio, e.nombre, e.video_url')) {
    const detalles = state.detalle_rutina_opo.filter((d) => d.rutinas_opo_id_rutina_opo === Number(params[0]));
    const ej = detalles.map((d) => {
      const e = state.ejercicios.find((x) => x.id_ejercicio === d.ejercicios_id_ejercicio);
      return { ...e, series: d.series, repeticiones: d.repeticiones, descanso: d.descanso };
    });
    return Promise.resolve([ej, []]);
  }

  // ---------- EJERCICIOS ----------
  if (s.startsWith('select id_ejercicio, nombre, video_url, instrucciones_tecnicas')) {
    let res = state.ejercicios.slice();
    return Promise.resolve([res, []]);
  }
  if (s.startsWith('select * from ejercicios')) {
    return Promise.resolve([state.ejercicios.slice(), []]);
  }
  if (s.includes('from ejercicios') && s.includes('entornos')) {
    const rows = state.ejercicios.map((e) => ({
      id_ejercicio: e.id_ejercicio,
      nombre: e.nombre,
      pilar: e.pilar,
      grupo_muscular: e.grupo_muscular || 'General',
      equipamiento: e.equipamiento || '—',
      entornos: e.entornos,
      tipo_ilustracion: e.tipo_ilustracion,
      video_url: e.video_url,
      animacion_url: e.animacion_url,
      instrucciones_tecnicas: e.instrucciones_tecnicas,
      categoria: e.categoria
    }));
    return Promise.resolve([rows, []]);
  }

  // ---------- PLANES ENTRENAMIENTO ----------
  if (s.startsWith('select id_plan from planes_entrenamiento')) {
    const row = state.planes_entrenamiento.find(
      (p) =>
        p.oposiciones_id_oposicion === Number(params[0]) &&
        p.nivel === params[1] &&
        p.genero === params[2] &&
        p.fuente === 'opofit_banco_planes'
    );
    return Promise.resolve([row ? [{ id_plan: row.id_plan }] : [], []]);
  }
  if (s.startsWith('select pd.id_plan_dia, pd.dia_semana')) {
    const rows = state.plan_dias
      .filter((d) => d.planes_id_plan === Number(params[0]))
      .sort((a, b) => a.orden - b.orden);
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select pde.orden, pde.nombre_prescripcion')) {
    const rows = state.plan_dia_ejercicios
      .filter((p) => p.plan_dias_id === Number(params[0]))
      .sort((a, b) => a.orden - b.orden)
      .map((p) => {
        const e = state.ejercicios.find((x) => x.id_ejercicio === p.ejercicios_id_ejercicio);
        return {
          orden: p.orden,
          nombre: p.nombre_prescripcion,
          series: p.series,
          repeticiones: p.repeticiones,
          descanso: p.descanso,
          unidad: p.notas,
          id_ejercicio: e?.id_ejercicio,
          video_url: e?.video_url,
          animacion_url: e?.animacion_url,
          instrucciones_tecnicas: e?.instrucciones_tecnicas,
          tipo_ilustracion: e?.tipo_ilustracion,
          categoria: e?.categoria,
          pilar: e?.pilar,
          grupo_muscular: e?.grupo_muscular,
          equipamiento: e?.equipamiento
        };
      });
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select 1 from historial_sesiones') && s.includes('date(fecha_entreno)')) {
    const hit = state.historial_sesiones.some(
      (h) =>
        h.usuarios_id_usuario === Number(params[0]) &&
        h.rutinas_opo_id_rutina_opo === Number(params[1]) &&
        new Date(h.fecha_entreno).toISOString().slice(0, 10) === String(params[2])
    );
    return Promise.resolve([hit ? [{ ok: 1 }] : [], []]);
  }
  if (s.startsWith('select distinct dayofweek(fecha_entreno)')) {
    const rows = state.historial_sesiones
      .filter((h) => h.usuarios_id_usuario === Number(params[0]) && h.tipo_rutina === 'OPO')
      .map((h) => {
        const d = new Date(h.fecha_entreno);
        const dow = d.getDay() === 0 ? 7 : d.getDay();
        return { dow, rutinas_opo_id_rutina_opo: h.rutinas_opo_id_rutina_opo, f: d.toISOString().slice(0, 10) };
      });
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select count(*) as sesionessemana from historial_sesiones')) {
    const n = state.historial_sesiones.filter(
      (h) => h.usuarios_id_usuario === Number(params[0]) && h.tipo_rutina === 'OPO'
    ).length;
    return Promise.resolve([[{ sesionesSemana: n }], []]);
  }

  // ---------- PLANES GENERADOS CACHE ----------
  if (s.startsWith('select plan_json, explicacion_ia, variacion_seed, entorno_entreno from planes_generados_cache')) {
    const row = state.planes_generados_cache.find(
      (c) =>
        c.usuarios_id_usuario === Number(params[0]) &&
        c.oposiciones_id_oposicion === Number(params[1]) &&
        c.yearweek === Number(params[2])
    );
    return Promise.resolve([row ? [row] : [], []]);
  }
  if (s.startsWith('insert into planes_generados_cache')) {
    const existing = state.planes_generados_cache.findIndex(
      (c) =>
        c.usuarios_id_usuario === Number(params[0]) &&
        c.oposiciones_id_oposicion === Number(params[1]) &&
        c.yearweek === Number(params[2])
    );
    const row = {
      usuarios_id_usuario: Number(params[0]),
      oposiciones_id_oposicion: Number(params[1]),
      yearweek: Number(params[2]),
      variacion_seed: Number(params[3]),
      entorno_entreno: params[4],
      plan_json: params[5],
      explicacion_ia: params[6]
    };
    if (existing >= 0) state.planes_generados_cache[existing] = row;
    else state.planes_generados_cache.push(row);
    return Promise.resolve([{ insertId: 1, affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete from planes_generados_cache')) {
    const before = state.planes_generados_cache.length;
    if (params.length === 1) {
      state.planes_generados_cache = state.planes_generados_cache.filter(
        (c) => c.usuarios_id_usuario !== Number(params[0])
      );
    } else {
      state.planes_generados_cache = state.planes_generados_cache.filter(
        (c) =>
          !(
            c.usuarios_id_usuario === Number(params[0]) &&
            c.oposiciones_id_oposicion === Number(params[1]) &&
            c.yearweek === Number(params[2])
          )
      );
    }
    return Promise.resolve([{ affectedRows: before - state.planes_generados_cache.length }, []]);
  }

  // ---------- SIMULACROS ----------
  if (s.startsWith('insert into simulacros')) {
    const id = nextId.simulacros++;
    state.simulacros.push({
      id_simulacro: id,
      fecha: new Date(),
      nota_media: params[0],
      usuarios_id_usuario: Number(params[1]),
      oposiciones_id_oposicion: Number(params[2])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('insert into simulacro_pruebas')) {
    const id = nextId.simulacro_pruebas++;
    state.simulacro_pruebas.push({
      id_simulacro_prueba: id,
      valor_registrado: params[0],
      nota_obtenida: params[1],
      simulacros_id_simulacro: Number(params[2]),
      pruebas_oficiales_id_pruebas_oficiales: Number(params[3])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('select s.id_simulacro, s.fecha, s.nota_media from simulacros')) {
    const rows = state.simulacros
      .filter((x) => x.usuarios_id_usuario === Number(params[0]) && x.oposiciones_id_oposicion === Number(params[1]))
      .sort((a, b) => new Date(b.fecha) - new Date(a.fecha))
      .slice(0, 20)
      .map((s) => ({ id_simulacro: s.id_simulacro, fecha: s.fecha, nota_media: s.nota_media }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select nota_media, fecha from simulacros')) {
    const arr = state.simulacros
      .filter((x) => x.usuarios_id_usuario === Number(params[0]) && x.oposiciones_id_oposicion === Number(params[1]))
      .sort((a, b) => new Date(b.fecha) - new Date(a.fecha))
      .slice(0, 1);
    return Promise.resolve([arr.map((s) => ({ nota_media: s.nota_media, fecha: s.fecha })), []]);
  }

  // ---------- GRUPOS COMUNIDAD ----------
  if (s.startsWith('select rol from grupo_miembros where id_grupo')) {
    const m = state.grupo_miembros.find(
      (x) => x.id_grupo === Number(params[0]) && x.id_usuario === Number(params[1])
    );
    return Promise.resolve([m ? [{ rol: m.rol }] : [], []]);
  }
  if (s.startsWith('select g.id_grupo, g.nombre, g.descripcion, g.id_oposicion')) {
    const userId = Number(params[0]);
    const fitness = s.includes('g.id_oposicion is null');
    const idOpo = fitness ? null : Number(params[1]);
    const grupos = state.grupos_comunidad.filter((g) =>
      fitness ? g.id_oposicion == null : g.id_oposicion === idOpo
    );
    const rows = grupos.map((g) => {
      const miembros = state.grupo_miembros.filter((m) => m.id_grupo === g.id_grupo);
      const self = miembros.find((m) => m.id_usuario === userId);
      return {
        id_grupo: g.id_grupo,
        nombre: g.nombre,
        descripcion: g.descripcion,
        id_oposicion: g.id_oposicion,
        creador_id: g.creador_id,
        creado_en: g.creado_en,
        num_miembros: miembros.length,
        mi_rol: self?.rol || null
      };
    }).sort((a, b) => new Date(b.creado_en) - new Date(a.creado_en));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select id_grupo from grupos_comunidad where id_grupo')) {
    const g = state.grupos_comunidad.find((x) => x.id_grupo === Number(params[0]));
    return Promise.resolve([g ? [{ id_grupo: g.id_grupo }] : [], []]);
  }
  if (s.startsWith('insert into grupos_comunidad')) {
    const id = nextId.grupos_comunidad++;
    state.grupos_comunidad.push({
      id_grupo: id,
      nombre: params[0],
      descripcion: params[1],
      id_oposicion: params[2] == null ? null : Number(params[2]),
      creador_id: Number(params[3]),
      creado_en: new Date()
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('insert into grupo_miembros')) {
    const id = nextId.grupo_miembros++;
    const rolLiteral = sql.match(/'ADMIN'|'MIEMBRO'/i);
    state.grupo_miembros.push({
      id_miembro: id,
      id_grupo: Number(params[0]),
      id_usuario: Number(params[1]),
      rol: params[2] || (rolLiteral ? rolLiteral[0].replace(/'/g, '') : 'MIEMBRO'),
      unido_en: new Date()
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('delete from grupo_miembros where id_grupo')) {
    const before = state.grupo_miembros.length;
    state.grupo_miembros = state.grupo_miembros.filter(
      (m) => !(m.id_grupo === Number(params[0]) && m.id_usuario === Number(params[1]))
    );
    return Promise.resolve([{ affectedRows: before - state.grupo_miembros.length }, []]);
  }
  if (s.startsWith('select m.id_mensaje, m.id_usuario, u.nombre as usuario_nombre')) {
    const rows = state.grupo_mensajes
      .filter((m) => m.id_grupo === Number(params[0]))
      .sort((a, b) => new Date(a.enviado_en) - new Date(b.enviado_en))
      .slice(0, Number(params[1]))
      .map((m) => ({
        id_mensaje: m.id_mensaje,
        id_usuario: m.id_usuario,
        usuario_nombre: state.usuarios.find((u) => u.id_usuario === m.id_usuario)?.nombre,
        texto: m.texto,
        enviado_en: m.enviado_en
      }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('insert into grupo_mensajes')) {
    const id = nextId.grupo_mensajes++;
    state.grupo_mensajes.push({
      id_mensaje: id,
      id_grupo: Number(params[0]),
      id_usuario: Number(params[1]),
      texto: params[2],
      enviado_en: new Date()
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('insert into quedadas')) {
    const id = nextId.quedadas++;
    state.quedadas.push({
      id_quedada: id,
      id_grupo: Number(params[0]),
      creador_id: Number(params[1]),
      titulo: params[2],
      descripcion: params[3],
      fecha_hora: params[4],
      ubicacion_lat: params[5],
      ubicacion_lng: params[6],
      creado_en: new Date()
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('select q.id_quedada, q.titulo, q.descripcion, q.fecha_hora')) {
    const rows = state.quedadas
      .filter((q) => q.id_grupo === Number(params[0]))
      .sort((a, b) => new Date(a.fecha_hora) - new Date(b.fecha_hora))
      .map((q) => ({
        id_quedada: q.id_quedada,
        titulo: q.titulo,
        descripcion: q.descripcion,
        fecha_hora: q.fecha_hora,
        ubicacion_lat: q.ubicacion_lat,
        ubicacion_lng: q.ubicacion_lng,
        creador_id: q.creador_id,
        creador_nombre: state.usuarios.find((u) => u.id_usuario === q.creador_id)?.nombre,
        creado_en: q.creado_en
      }));
    return Promise.resolve([rows, []]);
  }

  // ---------- AMISTADES ----------
  if (s.startsWith('select a.id_amistad, a.estado')) {
    const userId = Number(params[0]);
    const rows = state.amistades
      .filter((a) => (a.id_usuario_a === userId || a.id_usuario_b === userId) && a.estado === 'ACEPTADA')
      .map((a) => {
        const otroId = a.id_usuario_a === userId ? a.id_usuario_b : a.id_usuario_a;
        const otro = state.usuarios.find((u) => u.id_usuario === otroId);
        return {
          id_amistad: a.id_amistad,
          estado: a.estado,
          amigo_id: otroId,
          amigo_nombre: otro?.nombre,
          oposicion_id: otro?.oposiciones_id_oposicion
        };
      });
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select a.id_amistad, a.solicitante_id')) {
    const userId = Number(params[0]);
    const rows = state.amistades
      .filter((a) =>
        (a.id_usuario_a === userId || a.id_usuario_b === userId) &&
        a.estado === 'PENDIENTE' &&
        a.solicitante_id !== userId
      )
      .map((a) => ({
        id_amistad: a.id_amistad,
        solicitante_id: a.solicitante_id,
        solicitante_nombre: state.usuarios.find((u) => u.id_usuario === a.solicitante_id)?.nombre
      }));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('insert into amistades')) {
    const id = nextId.amistades++;
    state.amistades.push({
      id_amistad: id,
      id_usuario_a: Number(params[0]),
      id_usuario_b: Number(params[1]),
      estado: 'PENDIENTE',
      solicitante_id: Number(params[2])
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.includes('select u.id_usuario, u.nombre, u.perfil_publico, u.modo_uso') && s.includes("u.modo_uso = 'fitness'")) {
    const userId = Number(params[0]);
    const term = String(params[1] || '').replace(/%/g, '').toLowerCase();
    const limite = Number(params[2] || 20);
    const rows = state.usuarios
      .filter(
        (u) =>
          u.modo_uso === 'FITNESS' &&
          u.id_usuario !== userId &&
          String(u.nombre || '').toLowerCase().includes(term)
      )
      .slice(0, limite)
      .map((u) => ({
        id_usuario: u.id_usuario,
        nombre: u.nombre,
        perfil_publico: u.perfil_publico,
        modo_uso: u.modo_uso
      }));
    return Promise.resolve([rows, []]);
  }
  if (s.includes('select u.id_usuario, u.nombre, u.perfil_publico, u.modo_uso') && s.includes('u.oposiciones_id_oposicion = ?')) {
    const idOpo = Number(params[0]);
    const userId = Number(params[1]);
    const term = String(params[2] || '').replace(/%/g, '').toLowerCase();
    const limite = Number(params[3] || 20);
    const rows = state.usuarios
      .filter(
        (u) =>
          u.oposiciones_id_oposicion === idOpo &&
          u.id_usuario !== userId &&
          String(u.nombre || '').toLowerCase().includes(term)
      )
      .slice(0, limite)
      .map((u) => ({
        id_usuario: u.id_usuario,
        nombre: u.nombre,
        perfil_publico: u.perfil_publico,
        modo_uso: u.modo_uso
      }));
    return Promise.resolve([rows, []]);
  }

  // ---------- GPS ----------
  if (s.startsWith('insert into gps_actividades')) {
    const id = nextId.gps_actividades++;
    state.gps_actividades.push({
      id_actividad: id,
      uuid_local: params[0],
      usuarios_id_usuario: Number(params[1]),
      tipo: params[2],
      iniciada_en: Number(params[3]),
      finalizada_en: Number(params[4]),
      duracion_seg: Number(params[5]),
      distancia_m: Number(params[7]),
      polyline_json: params[17],
      splits_json: params[18]
    });
    return Promise.resolve([{ insertId: id, affectedRows: 1 }, []]);
  }
  if (s.startsWith('select id_actividad from gps_actividades')) {
    const a = state.gps_actividades.find(
      (x) => x.usuarios_id_usuario === Number(params[0]) && x.uuid_local === params[1]
    );
    return Promise.resolve([a ? [{ id_actividad: a.id_actividad }] : [], []]);
  }
  if (s.startsWith('select id_actividad, uuid_local, tipo, iniciada_en')) {
    const userId = Number(params[0]);
    const rows = state.gps_actividades
      .filter((a) => a.usuarios_id_usuario === userId)
      .sort((a, b) => b.iniciada_en - a.iniciada_en)
      .slice(0, Number(params[1] || 50));
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('delete from gps_actividades')) {
    const before = state.gps_actividades.length;
    state.gps_actividades = state.gps_actividades.filter(
      (a) => !(a.usuarios_id_usuario === Number(params[0]) && a.uuid_local === params[1])
    );
    return Promise.resolve([{ affectedRows: before - state.gps_actividades.length }, []]);
  }

  if (s.startsWith('insert into actividad_posts')) {
    const id = nextId.actividad_posts++;
    state.actividad_posts.push({
      id_post: id,
      id_usuario: Number(params[0]),
      titulo: params[1],
      texto: params[2],
      foto_url: params[3],
      visibilidad: params[4],
      fuente: params[5],
      gps_uuid: params[6],
      id_historial_sesion: params[7],
      id_simulacro: params[8],
      stats_json: params[9],
      creado_en: new Date().toISOString()
    });
    return Promise.resolve([{ insertId: id }, []]);
  }
  if (s.includes('from actividad_posts p') && s.includes('join usuarios u')) {
    const viewerId = Number(params[0]);
    const idPostFilter = s.includes('where p.id_post = ?') ? Number(params[1]) : null;
    const esFeed = s.includes('or p.visibilidad');
    const targetUserFilter = !idPostFilter && !esFeed && s.includes('where p.id_usuario = ?')
      ? Number(params[1])
      : null;
    const limite = Number(params[params.length - 1]) || 30;
    const amigoIds = state.amistades
      .filter((a) => a.estado === 'ACEPTADA' && (a.id_usuario_a === viewerId || a.id_usuario_b === viewerId))
      .map((a) => (a.id_usuario_a === viewerId ? a.id_usuario_b : a.id_usuario_a));
    let posts = state.actividad_posts;
    if (idPostFilter) posts = posts.filter((p) => p.id_post === idPostFilter);
    if (targetUserFilter) posts = posts.filter((p) => p.id_usuario === targetUserFilter);
    const rows = posts
      .map((p) => {
        const u = state.usuarios.find((x) => x.id_usuario === p.id_usuario);
        const likes = state.post_likes.filter((l) => l.id_post === p.id_post).length;
        const comentarios = state.post_comentarios.filter((c) => c.id_post === p.id_post).length;
        const yoLike = state.post_likes.some((l) => l.id_post === p.id_post && l.id_usuario === viewerId);
        return {
          ...p,
          usuario_nombre: u?.nombre,
          avatar_url: u?.avatar_url,
          likes,
          comentarios,
          yo_like: yoLike ? 1 : 0
        };
      })
      .filter((p) => {
        if (idPostFilter) return true;
        if (p.id_usuario === viewerId) return true;
        if (p.visibilidad === 'PUBLICO') return true;
        return amigoIds.includes(p.id_usuario);
      })
      .sort((a, b) => new Date(b.creado_en) - new Date(a.creado_en))
      .slice(0, idPostFilter ? 1 : limite);
    if (idPostFilter) return Promise.resolve([[rows[0] || undefined].filter(Boolean), []]);
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select c.id_comentario, c.id_usuario, c.texto')) {
    const idPost = Number(params[0]);
    const rows = state.post_comentarios
      .filter((c) => c.id_post === idPost)
      .map((c) => {
        const u = state.usuarios.find((x) => x.id_usuario === c.id_usuario);
        return {
          ...c,
          usuario_nombre: u?.nombre,
          avatar_url: u?.avatar_url
        };
      });
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select duracion_ms from segmento_esfuerzos')) {
    const rows = state.segmento_esfuerzos
      .filter((e) => e.id_usuario === Number(params[0]) && e.id_segmento === Number(params[1]))
      .sort((a, b) => a.duracion_ms - b.duracion_ms)
      .slice(0, 1);
    return Promise.resolve([rows, []]);
  }
  if (s.includes('from segmento_esfuerzos e') && s.includes('join usuarios u')) {
    const idSeg = Number(params[0]);
    const rows = state.segmento_esfuerzos
      .filter((e) => e.id_segmento === idSeg)
      .sort((a, b) => a.duracion_ms - b.duracion_ms)
      .slice(0, Number(params[1] || 50))
      .map((e) => {
        const u = state.usuarios.find((x) => x.id_usuario === e.id_usuario);
        return {
          ...e,
          usuario_nombre: u?.nombre,
          avatar_url: u?.avatar_url
        };
      });
    return Promise.resolve([rows, []]);
  }
  if (s.startsWith('select id_like from post_likes')) {
    const row = state.post_likes.find(
      (l) => l.id_post === Number(params[0]) && l.id_usuario === Number(params[1])
    );
    return Promise.resolve([[row || undefined].filter(Boolean), []]);
  }
  if (s.startsWith('delete from post_likes')) {
    const before = state.post_likes.length;
    state.post_likes = state.post_likes.filter(
      (l) => !(l.id_post === Number(params[0]) && l.id_usuario === Number(params[1]))
    );
    return Promise.resolve([{ affectedRows: before - state.post_likes.length }, []]);
  }
  if (s.startsWith('insert into post_likes')) {
    const id = nextId.post_likes++;
    state.post_likes.push({ id_like: id, id_post: Number(params[0]), id_usuario: Number(params[1]) });
    return Promise.resolve([{ insertId: id }, []]);
  }
  if (s.startsWith('insert into post_comentarios')) {
    const id = nextId.post_comentarios++;
    state.post_comentarios.push({
      id_comentario: id,
      id_post: Number(params[0]),
      id_usuario: Number(params[1]),
      texto: params[2],
      creado_en: new Date().toISOString()
    });
    return Promise.resolve([{ insertId: id }, []]);
  }
  if (s.startsWith('insert into segmentos')) {
    const id = nextId.segmentos++;
    state.segmentos.push({
      id_segmento: id,
      slug: params[0],
      nombre: params[1],
      tipo: params[2],
      distancia_m: Number(params[3]),
      mejor_si_menor: Number(params[4]),
      categoria: params[5],
      activo: 1
    });
    return Promise.resolve([{ insertId: id }, []]);
  }
  if (s.startsWith('select id_segmento, slug, nombre, tipo, distancia_m')) {
    return Promise.resolve([
      state.segmentos.filter((s) => s.activo).sort((a, b) => a.distancia_m - b.distancia_m),
      []
    ]);
  }
  if (s.startsWith('select id_segmento, mejor_si_menor from segmentos where id_segmento')) {
    const row = state.segmentos.find((x) => x.id_segmento === Number(params[0]) && x.activo);
    return Promise.resolve([
      row ? [{ id_segmento: row.id_segmento, mejor_si_menor: row.mejor_si_menor }] : [],
      []
    ]);
  }
  if (s.startsWith('select * from segmentos where id_segmento')) {
    const row = state.segmentos.find((x) => x.id_segmento === Number(params[0]) && x.activo);
    return Promise.resolve([[row || undefined].filter(Boolean), []]);
  }
  if (s.startsWith('select id_segmento from segmentos where slug')) {
    const row = state.segmentos.find((x) => x.slug === params[0] && x.activo);
    return Promise.resolve([[row || undefined].filter(Boolean), []]);
  }
  if (s.startsWith('insert into segmento_esfuerzos')) {
    const id = nextId.segmento_esfuerzos++;
    state.segmento_esfuerzos.push({
      id_esfuerzo: id,
      id_segmento: Number(params[0]),
      id_usuario: Number(params[1]),
      duracion_ms: Number(params[2]),
      gps_uuid: params[3],
      creado_en: new Date().toISOString()
    });
    return Promise.resolve([{ insertId: id }, []]);
  }

  // Por defecto no encontrada
  return Promise.resolve([[], []]);
}

async function getConnection() {
  return {
    query,
    beginTransaction: async () => undefined,
    commit: async () => undefined,
    rollback: async () => undefined,
    release: () => undefined
  };
}

const pool = { query, getConnection };

module.exports = { pool, state, reset, makeUser, nextId };
