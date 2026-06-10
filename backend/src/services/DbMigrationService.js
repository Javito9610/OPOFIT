const db = require('../config/db');

/**
 * Aplica migraciones de esquema en Railway/producción si faltan columnas o tablas.
 * Idempotente: solo añade lo que no existe (no borra datos).
 */
class DbMigrationService {
  static async columnExists(table, column) {
    const [rows] = await db.query(
      `SELECT 1 FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
       LIMIT 1`,
      [table, column]
    );
    return rows.length > 0;
  }

  static async tableExists(table) {
    const [rows] = await db.query(
      `SELECT 1 FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
       LIMIT 1`,
      [table]
    );
    return rows.length > 0;
  }

  static async addColumnIfMissing(table, column, definition) {
    if (await DbMigrationService.columnExists(table, column)) return false;
    await db.query(`ALTER TABLE \`${table}\` ADD COLUMN \`${column}\` ${definition}`);
    console.log(`[migrate] ${table}.${column} añadida`);
    return true;
  }

  /** Convierte tablas con JSON/texto a utf8mb4 para soportar emojis (🏠, 💪, etc.). */
  static async ensureUtf8mb4Table(table) {
    if (!(await DbMigrationService.tableExists(table))) return false;
    const [rows] = await db.query(
      `SELECT TABLE_COLLATION FROM information_schema.TABLES
       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? LIMIT 1`,
      [table]
    );
    const coll = rows[0]?.TABLE_COLLATION || '';
    if (coll.startsWith('utf8mb4')) return false;
    await db.query(
      `ALTER TABLE \`${table}\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`
    );
    console.log(`[migrate] ${table} convertida a utf8mb4`);
    return true;
  }

  static async runOnStartup() {
    try {
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'es_premium',
        'TINYINT(1) NOT NULL DEFAULT 0'
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'premium_hasta',
        'DATETIME NULL'
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'perfil_publico',
        'TINYINT(1) NOT NULL DEFAULT 0'
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'fcm_token',
        'VARCHAR(512) NULL'
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'es_admin',
        'TINYINT(1) NOT NULL DEFAULT 0'
      );

      // v7-doctorado: el material disponible del usuario (CSV) lo usan tanto la
      // IA generadora de planes como el filtro de ejercicios libres para no
      // proponer dominadas a alguien sin barra ni KB swings a alguien sin KB.
      // Catálogo soportado: BARRA_DOMINADAS, BARRA_OLIMPICA, MANCUERNAS, KB,
      // TRX, ANILLAS, GOMAS, COMBA, SACO, FOAM, BANCO, CAJA, BICI, REMO,
      // ECHO_BIKE, SKI_ERG, PISCINA, PISTA, MONTAÑA, NADA.
      await DbMigrationService.addColumnIfMissing(
        'settings',
        'material_disponible',
        "VARCHAR(500) NULL DEFAULT 'NADA'"
      );

      // v8: comunidad — grupos con tipo (PRIVADO estilo WhatsApp, COMUNIDAD
      // estilo Strava clubs). Antes solo había un tipo y los privados no
      // existían como concepto.
      await DbMigrationService.addColumnIfMissing(
        'grupos_comunidad',
        'tipo',
        "VARCHAR(16) NOT NULL DEFAULT 'COMUNIDAD'"
      );

      // v7-doctorado: campos modalidad + score_tipo en cada ejercicio para que
      // la UI sepa qué input mostrar (timer para AMRAP, contador rondas para
      // EMOM, peso×reps para crossfit_lift, etc.) y el historial pueda
      // graficar PR por tipo de ejercicio.
      await DbMigrationService.addColumnIfMissing(
        'ejercicios',
        'modalidad',
        "VARCHAR(32) NOT NULL DEFAULT 'convencional'"
      );
      await DbMigrationService.addColumnIfMissing(
        'ejercicios',
        'score_tipo',
        "VARCHAR(32) NOT NULL DEFAULT 'reps'"
      );

      await DbMigrationService.addColumnIfMissing(
        'oposiciones',
        'incluida_gratis',
        'TINYINT(1) NOT NULL DEFAULT 1'
      );
      await DbMigrationService.addColumnIfMissing(
        'oposiciones',
        'convocatoria_ref',
        'VARCHAR(500) NULL'
      );
      await DbMigrationService.addColumnIfMissing(
        'oposiciones',
        'notas_usuario',
        'TEXT NULL'
      );

      await DbMigrationService.addColumnIfMissing(
        'pruebas_oficiales',
        'convocatoria_ref',
        'VARCHAR(500) NULL'
      );
      await DbMigrationService.addColumnIfMissing(
        'pruebas_oficiales',
        'fuente_legal',
        'VARCHAR(500) NULL'
      );
      if (!(await DbMigrationService.columnExists('pruebas_oficiales', 'tipo_baremo'))) {
        await db.query(
          `ALTER TABLE pruebas_oficiales
           ADD COLUMN tipo_baremo ENUM('PUNTUACION','APTO_NO_APTO') NOT NULL DEFAULT 'PUNTUACION'`
        );
        console.log('[migrate] pruebas_oficiales.tipo_baremo añadida');
      }
      await DbMigrationService.addColumnIfMissing(
        'pruebas_oficiales',
        'unidad_entrada',
        "VARCHAR(10) NOT NULL DEFAULT 'reps'"
      );

      if (!(await DbMigrationService.tableExists('simulacros'))) {
        await db.query(`
          CREATE TABLE simulacros (
            id_simulacro INT NOT NULL AUTO_INCREMENT,
            fecha DATETIME NOT NULL,
            nota_media DECIMAL(5,2) NULL,
            usuarios_id_usuario INT NOT NULL,
            oposiciones_id_oposicion INT NOT NULL,
            PRIMARY KEY (id_simulacro),
            INDEX fk_sim_usuario_idx (usuarios_id_usuario),
            INDEX fk_sim_opo_idx (oposiciones_id_oposicion),
            CONSTRAINT fk_sim_usuario FOREIGN KEY (usuarios_id_usuario) REFERENCES usuarios (id_usuario),
            CONSTRAINT fk_sim_opo FOREIGN KEY (oposiciones_id_oposicion) REFERENCES oposiciones (id_oposicion)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla simulacros creada');
      }

      if (!(await DbMigrationService.tableExists('simulacro_pruebas'))) {
        await db.query(`
          CREATE TABLE simulacro_pruebas (
            id_simulacro_prueba INT NOT NULL AUTO_INCREMENT,
            valor_registrado DECIMAL(10,2) NOT NULL,
            nota_obtenida INT NULL,
            simulacros_id_simulacro INT NOT NULL,
            pruebas_oficiales_id_pruebas_oficiales INT NOT NULL,
            PRIMARY KEY (id_simulacro_prueba),
            CONSTRAINT fk_sp_sim FOREIGN KEY (simulacros_id_simulacro) REFERENCES simulacros (id_simulacro) ON DELETE CASCADE,
            CONSTRAINT fk_sp_prueba FOREIGN KEY (pruebas_oficiales_id_pruebas_oficiales) REFERENCES pruebas_oficiales (id_pruebas_oficiales)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla simulacro_pruebas creada');
      }

      await db.query('UPDATE oposiciones SET incluida_gratis = 1 WHERE incluida_gratis = 0 OR incluida_gratis IS NULL');

      // v7.1-doctorado: fix one-shot del bug de "0 minutos" en el historial.
      // El frontend enviaba MINUTOS en `historial_sesiones.duracion_oficial`
      // pero el resto del backend la trata como SEGUNDOS (la divide entre 60
      // para mostrar). Resultado: 5 min → guardado como 5 s → resumen "0 min".
      // Corregimos las filas afectadas (duración < 5 min = sospechosamente
      // pequeña) multiplicándolas por 60. Se ejecuta una sola vez gracias al
      // marcador en app_meta.
      try {
        const [marcador] = await db.query(
          `SELECT valor FROM app_meta WHERE clave = 'duracion_oficial_fix_v1' LIMIT 1`
        );
        if (!marcador?.[0]?.valor) {
          const [r] = await db.query(
            `UPDATE historial_sesiones
                SET duracion_oficial = duracion_oficial * 60
              WHERE duracion_oficial > 0 AND duracion_oficial < 300`
          );
          await db.query(
            `INSERT INTO app_meta (clave, valor) VALUES ('duracion_oficial_fix_v1', '1')
             ON DUPLICATE KEY UPDATE valor = VALUES(valor)`
          );
          if (r.affectedRows > 0) {
            console.log(`[migrate] historial_sesiones.duracion_oficial: ${r.affectedRows} filas reescaladas (min → seg)`);
          }
        }
      } catch (e) {
        console.warn('[migrate] duracion_oficial_fix_v1 saltado:', e.message);
      }

      await db.query(
        `UPDATE pruebas_oficiales SET unidad_entrada = 's'
         WHERE mejor_si_es_menor = 1 AND (unidad_entrada IS NULL OR unidad_entrada = 'reps')`
      );
      await db.query(
        `UPDATE pruebas_oficiales SET unidad_entrada = 's'
         WHERE id_pruebas_oficiales IN (1, 3, 4, 5, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18, 21, 22)`
      );
      await db.query(
        `UPDATE pruebas_oficiales SET unidad_entrada = 'reps'
         WHERE id_pruebas_oficiales IN (2, 6, 10, 19) AND mejor_si_es_menor = 0`
      );
      await db.query(`UPDATE pruebas_oficiales SET unidad_entrada = 's' WHERE id_pruebas_oficiales = 20`);

      if (!(await DbMigrationService.tableExists('amistades'))) {
        await db.query(`
          CREATE TABLE amistades (
            id_amistad INT NOT NULL AUTO_INCREMENT,
            id_usuario_a INT NOT NULL,
            id_usuario_b INT NOT NULL,
            estado ENUM('PENDIENTE','ACEPTADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
            solicitante_id INT NOT NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_amistad),
            UNIQUE KEY uk_par (id_usuario_a, id_usuario_b),
            CONSTRAINT fk_am_a FOREIGN KEY (id_usuario_a) REFERENCES usuarios (id_usuario),
            CONSTRAINT fk_am_b FOREIGN KEY (id_usuario_b) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla amistades creada');
      }

      if (!(await DbMigrationService.tableExists('mensajes_chat'))) {
        await db.query(`
          CREATE TABLE mensajes_chat (
            id_mensaje INT NOT NULL AUTO_INCREMENT,
            id_remitente INT NOT NULL,
            id_destinatario INT NOT NULL,
            texto VARCHAR(1000) NOT NULL,
            leido TINYINT(1) NOT NULL DEFAULT 0,
            enviado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_mensaje),
            INDEX idx_chat_par (id_remitente, id_destinatario),
            CONSTRAINT fk_msg_rem FOREIGN KEY (id_remitente) REFERENCES usuarios (id_usuario),
            CONSTRAINT fk_msg_des FOREIGN KEY (id_destinatario) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla mensajes_chat creada');
      }

      if (!(await DbMigrationService.tableExists('rutinas_compartidas'))) {
        await db.query(`
          CREATE TABLE rutinas_compartidas (
            id_compartida INT NOT NULL AUTO_INCREMENT,
            id_rutina_pers INT NOT NULL,
            id_propietario INT NOT NULL,
            id_destinatario INT NOT NULL,
            mensaje VARCHAR(255) NULL,
            compartido_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_compartida),
            CONSTRAINT fk_rc_rutina FOREIGN KEY (id_rutina_pers) REFERENCES rutinas_pers (id_rutina_pers),
            CONSTRAINT fk_rc_prop FOREIGN KEY (id_propietario) REFERENCES usuarios (id_usuario),
            CONSTRAINT fk_rc_dest FOREIGN KEY (id_destinatario) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla rutinas_compartidas creada');
      }

      if (!(await DbMigrationService.tableExists('planes_entrenamiento'))) {
        await db.query(`
          CREATE TABLE planes_entrenamiento (
            id_plan INT NOT NULL AUTO_INCREMENT,
            oposiciones_id_oposicion INT NOT NULL,
            nivel ENUM('BASICO','INTERMEDIO','AVANZADO') NOT NULL,
            genero ENUM('HOMBRE','MUJER') NOT NULL,
            nombre VARCHAR(200) NOT NULL,
            dias_por_semana TINYINT NOT NULL DEFAULT 5,
            fuente VARCHAR(80) NULL DEFAULT 'opofit_banco_planes',
            PRIMARY KEY (id_plan),
            UNIQUE KEY uniq_plan_opo_nivel_gen (oposiciones_id_oposicion, nivel, genero),
            CONSTRAINT fk_plan_opo FOREIGN KEY (oposiciones_id_oposicion) REFERENCES oposiciones (id_oposicion)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla planes_entrenamiento creada');
      }

      if (!(await DbMigrationService.tableExists('plan_dias'))) {
        await db.query(`
          CREATE TABLE plan_dias (
            id_plan_dia INT NOT NULL AUTO_INCREMENT,
            planes_id_plan INT NOT NULL,
            dia_semana TINYINT NOT NULL COMMENT '1=Lunes..7=Domingo',
            orden TINYINT NOT NULL,
            enfoque_tipo ENUM('FUERZA','RESISTENCIA','VELOCIDAD') NOT NULL,
            rutinas_opo_id INT NOT NULL,
            titulo_sesion VARCHAR(200) NOT NULL,
            descripcion_sesion TEXT NULL,
            PRIMARY KEY (id_plan_dia),
            UNIQUE KEY uniq_plan_dia (planes_id_plan, dia_semana),
            CONSTRAINT fk_pd_plan FOREIGN KEY (planes_id_plan) REFERENCES planes_entrenamiento (id_plan) ON DELETE CASCADE,
            CONSTRAINT fk_pd_rutina FOREIGN KEY (rutinas_opo_id) REFERENCES rutinas_opo (id_rutina_opo)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla plan_dias creada');
      }

      await DbMigrationService.addColumnIfMissing('ejercicios', 'categoria', "VARCHAR(60) NULL");
      await DbMigrationService.addColumnIfMissing('ejercicios', 'pilar', "ENUM('FUERZA','RESISTENCIA','VELOCIDAD','MOVILIDAD','CORE') NULL");
      await DbMigrationService.addColumnIfMissing('ejercicios', 'grupo_muscular', 'VARCHAR(80) NULL');
      await DbMigrationService.addColumnIfMissing('ejercicios', 'equipamiento', 'VARCHAR(120) NULL');
      await DbMigrationService.addColumnIfMissing('ejercicios', 'entornos', 'VARCHAR(200) NULL');
      await DbMigrationService.addColumnIfMissing(
        'rutinas_pers',
        'entorno_entreno',
        "VARCHAR(20) NULL"
      );
      await DbMigrationService.addColumnIfMissing('ejercicios', 'tipo_ilustracion', 'VARCHAR(24) NULL');
      await DbMigrationService.addColumnIfMissing('ejercicios', 'animacion_url', 'VARCHAR(500) NULL');

      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'entorno_entreno',
        "VARCHAR(20) NULL"
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'plan_variacion_seed',
        'INT NOT NULL DEFAULT 0'
      );

      if (!(await DbMigrationService.tableExists('planes_generados_cache'))) {
        await db.query(`
          CREATE TABLE planes_generados_cache (
            id_cache INT NOT NULL AUTO_INCREMENT,
            usuarios_id_usuario INT NOT NULL,
            oposiciones_id_oposicion INT NOT NULL,
            yearweek INT NOT NULL,
            variacion_seed INT NOT NULL DEFAULT 0,
            entorno_entreno VARCHAR(20) NOT NULL,
            plan_json LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
            explicacion_ia TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_cache),
            UNIQUE KEY uniq_plan_gen_semana (usuarios_id_usuario, oposiciones_id_oposicion, yearweek),
            CONSTRAINT fk_pgc_usuario FOREIGN KEY (usuarios_id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('[migrate] tabla planes_generados_cache creada');
      }

      if (!(await DbMigrationService.tableExists('plan_dia_ejercicios'))) {
        await db.query(`
          CREATE TABLE plan_dia_ejercicios (
            id_plan_dia_ejercicio INT NOT NULL AUTO_INCREMENT,
            plan_dias_id INT NOT NULL,
            orden TINYINT NOT NULL,
            nombre_prescripcion VARCHAR(255) NOT NULL,
            ejercicios_id_ejercicio INT NULL,
            series INT NOT NULL DEFAULT 1,
            repeticiones INT NOT NULL DEFAULT 10,
            descanso INT NOT NULL DEFAULT 90,
            notas VARCHAR(40) NULL,
            PRIMARY KEY (id_plan_dia_ejercicio),
            INDEX idx_pde_plan_dia (plan_dias_id),
            CONSTRAINT fk_pde_plan_dia FOREIGN KEY (plan_dias_id) REFERENCES plan_dias (id_plan_dia) ON DELETE CASCADE,
            CONSTRAINT fk_pde_ejercicio FOREIGN KEY (ejercicios_id_ejercicio) REFERENCES ejercicios (id_ejercicio)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla plan_dia_ejercicios creada');
      }

      if (!(await DbMigrationService.tableExists('app_meta'))) {
        await db.query(`
          CREATE TABLE app_meta (
            clave VARCHAR(64) PRIMARY KEY,
            valor VARCHAR(32) NOT NULL
          ) ENGINE=InnoDB
        `);
      }

      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'hora_recordatorio_entreno',
        "TIME NULL DEFAULT '18:00:00'"
      );
      await DbMigrationService.addColumnIfMissing(
        'usuarios',
        'recordatorio_entreno_activo',
        'TINYINT(1) NOT NULL DEFAULT 1'
      );

      await DbMigrationService.addColumnIfMissing(
        'historial_sesiones',
        'gps_actividad_uuid',
        'VARCHAR(64) NULL'
      );

      await DbMigrationService.addColumnIfMissing(
        'historial_sesiones',
        'gps_actividad_uuid',
        'VARCHAR(64) NULL'
      );

      if (!(await DbMigrationService.tableExists('integraciones_oauth'))) {
        await db.query(`
          CREATE TABLE integraciones_oauth (
            id_integracion INT NOT NULL AUTO_INCREMENT,
            usuarios_id_usuario INT NOT NULL,
            provider ENUM('STRAVA','POLAR') NOT NULL,
            external_user_id VARCHAR(64) NULL,
            access_token VARCHAR(255) NOT NULL,
            refresh_token VARCHAR(255) NULL,
            expires_at BIGINT NULL,
            scope VARCHAR(255) NULL,
            last_sync_at TIMESTAMP NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (id_integracion),
            UNIQUE KEY uniq_user_provider (usuarios_id_usuario, provider),
            CONSTRAINT fk_integ_usuario FOREIGN KEY (usuarios_id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla integraciones_oauth creada');
      }

      if (!(await DbMigrationService.tableExists('gps_actividades'))) {
        await db.query(`
          CREATE TABLE gps_actividades (
            id_actividad INT NOT NULL AUTO_INCREMENT,
            uuid_local VARCHAR(64) NOT NULL,
            usuarios_id_usuario INT NOT NULL,
            tipo ENUM('RUN','WALK','BIKE') NOT NULL,
            iniciada_en BIGINT NOT NULL,
            finalizada_en BIGINT NOT NULL,
            duracion_seg INT NOT NULL,
            movimiento_seg INT NOT NULL,
            distancia_m DOUBLE NOT NULL,
            velocidad_media_mps DOUBLE NOT NULL,
            velocidad_max_mps DOUBLE NOT NULL,
            ritmo_medio_spkm DOUBLE NOT NULL,
            ritmo_min_spkm DOUBLE NOT NULL,
            ritmo_max_spkm DOUBLE NOT NULL,
            desnivel_pos_m DOUBLE NOT NULL DEFAULT 0,
            altitud_min_m DOUBLE NULL,
            altitud_max_m DOUBLE NULL,
            cadencia_media_ppm DOUBLE NULL,
            polyline_json LONGTEXT NULL,
            splits_json TEXT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_actividad),
            UNIQUE KEY uniq_gps_user_uuid (usuarios_id_usuario, uuid_local),
            INDEX idx_gps_usuario (usuarios_id_usuario),
            CONSTRAINT fk_gps_usuario FOREIGN KEY (usuarios_id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla gps_actividades creada');
      }

      await DbMigrationService.addColumnIfMissing(
        'gps_actividades',
        'origen',
        "VARCHAR(24) NOT NULL DEFAULT 'LOCAL'"
      );
      await DbMigrationService.addColumnIfMissing(
        'gps_actividades',
        'external_id',
        'VARCHAR(64) NULL'
      );
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'desnivel_neg_m', 'DOUBLE NOT NULL DEFAULT 0');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'ritmo_cardiaco_medio', 'INT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'ritmo_cardiaco_max', 'INT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'ritmo_cardiaco_min', 'INT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'kcal', 'INT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'cadencia_max_ppm', 'INT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'zancada_media_m', 'DOUBLE NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'pendiente_media_pct', 'DOUBLE NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'splits_milla_json', 'TEXT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'splits_tiempo_json', 'TEXT NULL');
      await DbMigrationService.addColumnIfMissing('gps_actividades', 'mejores_segmentos_json', 'TEXT NULL');

      await DbMigrationService.addColumnIfMissing('usuarios', 'avatar_url', 'VARCHAR(512) NULL');
      if (!(await DbMigrationService.columnExists('usuarios', 'modo_uso'))) {
        await db.query(
          `ALTER TABLE usuarios ADD COLUMN modo_uso ENUM('OPOSITOR','FITNESS') NOT NULL DEFAULT 'OPOSITOR'`
        );
        console.log('[migrate] usuarios.modo_uso añadida');
      }
      await DbMigrationService.addColumnIfMissing('usuarios', 'ubicacion_lat', 'DECIMAL(10,7) NULL');
      await DbMigrationService.addColumnIfMissing('usuarios', 'ubicacion_lng', 'DECIMAL(10,7) NULL');
      await DbMigrationService.addColumnIfMissing('usuarios', 'ubicacion_visible', 'TINYINT(1) NOT NULL DEFAULT 0');
      await DbMigrationService.addColumnIfMissing('usuarios', 'ubicacion_actualizada', 'DATETIME NULL');

      if (!(await DbMigrationService.tableExists('grupos_comunidad'))) {
        await db.query(`
          CREATE TABLE grupos_comunidad (
            id_grupo INT NOT NULL AUTO_INCREMENT,
            nombre VARCHAR(120) NOT NULL,
            descripcion VARCHAR(500) NULL,
            id_oposicion INT NULL,
            creador_id INT NOT NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_grupo),
            INDEX idx_grupo_opo (id_oposicion),
            CONSTRAINT fk_gc_opo FOREIGN KEY (id_oposicion) REFERENCES oposiciones (id_oposicion),
            CONSTRAINT fk_gc_creador FOREIGN KEY (creador_id) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla grupos_comunidad creada');
      }

      if (!(await DbMigrationService.tableExists('grupo_miembros'))) {
        await db.query(`
          CREATE TABLE grupo_miembros (
            id_miembro INT NOT NULL AUTO_INCREMENT,
            id_grupo INT NOT NULL,
            id_usuario INT NOT NULL,
            rol ENUM('ADMIN','MIEMBRO') NOT NULL DEFAULT 'MIEMBRO',
            unido_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_miembro),
            UNIQUE KEY uk_grupo_usuario (id_grupo, id_usuario),
            CONSTRAINT fk_gm_grupo FOREIGN KEY (id_grupo) REFERENCES grupos_comunidad (id_grupo) ON DELETE CASCADE,
            CONSTRAINT fk_gm_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla grupo_miembros creada');
      }

      if (!(await DbMigrationService.tableExists('grupo_mensajes'))) {
        await db.query(`
          CREATE TABLE grupo_mensajes (
            id_mensaje INT NOT NULL AUTO_INCREMENT,
            id_grupo INT NOT NULL,
            id_usuario INT NOT NULL,
            texto VARCHAR(1000) NOT NULL,
            enviado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_mensaje),
            INDEX idx_gmsg_grupo (id_grupo),
            CONSTRAINT fk_gmsg_grupo FOREIGN KEY (id_grupo) REFERENCES grupos_comunidad (id_grupo) ON DELETE CASCADE,
            CONSTRAINT fk_gmsg_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla grupo_mensajes creada');
      }

      if (!(await DbMigrationService.tableExists('quedadas'))) {
        await db.query(`
          CREATE TABLE quedadas (
            id_quedada INT NOT NULL AUTO_INCREMENT,
            id_grupo INT NOT NULL,
            creador_id INT NOT NULL,
            titulo VARCHAR(120) NOT NULL,
            descripcion VARCHAR(500) NULL,
            fecha_hora DATETIME NOT NULL,
            ubicacion_lat DECIMAL(10,7) NULL,
            ubicacion_lng DECIMAL(10,7) NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_quedada),
            INDEX idx_quedada_grupo (id_grupo),
            CONSTRAINT fk_q_grupo FOREIGN KEY (id_grupo) REFERENCES grupos_comunidad (id_grupo) ON DELETE CASCADE,
            CONSTRAINT fk_q_creador FOREIGN KEY (creador_id) REFERENCES usuarios (id_usuario)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla quedadas creada');
      }

      if (!(await DbMigrationService.tableExists('actividad_posts'))) {
        await db.query(`
          CREATE TABLE actividad_posts (
            id_post INT NOT NULL AUTO_INCREMENT,
            id_usuario INT NOT NULL,
            titulo VARCHAR(120) NOT NULL,
            texto VARCHAR(2000) NULL,
            foto_url VARCHAR(512) NULL,
            visibilidad ENUM('AMIGOS','PUBLICO') NOT NULL DEFAULT 'AMIGOS',
            fuente ENUM('GPS','ENTRENO','SIMULACRO','MANUAL') NOT NULL DEFAULT 'MANUAL',
            gps_uuid VARCHAR(64) NULL,
            id_historial_sesion INT NULL,
            id_simulacro INT NULL,
            stats_json TEXT NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_post),
            INDEX idx_post_usuario (id_usuario),
            INDEX idx_post_creado (creado_en),
            CONSTRAINT fk_post_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla actividad_posts creada');
      }

      if (!(await DbMigrationService.tableExists('post_likes'))) {
        await db.query(`
          CREATE TABLE post_likes (
            id_like INT NOT NULL AUTO_INCREMENT,
            id_post INT NOT NULL,
            id_usuario INT NOT NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_like),
            UNIQUE KEY uk_post_like (id_post, id_usuario),
            CONSTRAINT fk_pl_post FOREIGN KEY (id_post) REFERENCES actividad_posts (id_post) ON DELETE CASCADE,
            CONSTRAINT fk_pl_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla post_likes creada');
      }

      if (!(await DbMigrationService.tableExists('post_comentarios'))) {
        await db.query(`
          CREATE TABLE post_comentarios (
            id_comentario INT NOT NULL AUTO_INCREMENT,
            id_post INT NOT NULL,
            id_usuario INT NOT NULL,
            texto VARCHAR(500) NOT NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_comentario),
            INDEX idx_pc_post (id_post),
            CONSTRAINT fk_pc_post FOREIGN KEY (id_post) REFERENCES actividad_posts (id_post) ON DELETE CASCADE,
            CONSTRAINT fk_pc_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla post_comentarios creada');
      }

      // Item 25: notificaciones in-app (centro de notificaciones para social/comunidad)
      if (!(await DbMigrationService.tableExists('notificaciones_app'))) {
        await db.query(`
          CREATE TABLE notificaciones_app (
            id_notificacion INT NOT NULL AUTO_INCREMENT,
            id_usuario INT NOT NULL,
            tipo ENUM('LIKE','COMENTARIO','SOLICITUD_AMISTAD','AMISTAD_ACEPTADA','POST_REPORTE','SISTEMA') NOT NULL,
            titulo VARCHAR(160) NOT NULL,
            cuerpo VARCHAR(400) NULL,
            ref_id INT NULL,
            actor_id INT NULL,
            leida TINYINT(1) NOT NULL DEFAULT 0,
            creada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_notificacion),
            INDEX idx_notif_usuario (id_usuario, leida),
            INDEX idx_notif_creada (creada_en),
            CONSTRAINT fk_notif_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
            CONSTRAINT fk_notif_actor FOREIGN KEY (actor_id) REFERENCES usuarios (id_usuario) ON DELETE SET NULL
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('[migrate] tabla notificaciones_app creada');
      }

      // Item 25: moderación de posts y comentarios reportados
      if (!(await DbMigrationService.tableExists('post_reportes'))) {
        await db.query(`
          CREATE TABLE post_reportes (
            id_reporte INT NOT NULL AUTO_INCREMENT,
            id_post INT NULL,
            id_comentario INT NULL,
            id_usuario_reporta INT NOT NULL,
            motivo ENUM('SPAM','OFENSIVO','VIOLENCIA','FALSA_INFO','OTRO') NOT NULL DEFAULT 'OTRO',
            detalle VARCHAR(400) NULL,
            estado ENUM('PENDIENTE','REVISADO','OCULTADO','DESESTIMADO') NOT NULL DEFAULT 'PENDIENTE',
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            revisado_en DATETIME NULL,
            PRIMARY KEY (id_reporte),
            INDEX idx_rep_post (id_post),
            INDEX idx_rep_comentario (id_comentario),
            INDEX idx_rep_estado (estado),
            CONSTRAINT fk_rep_post FOREIGN KEY (id_post) REFERENCES actividad_posts (id_post) ON DELETE CASCADE,
            CONSTRAINT fk_rep_comentario FOREIGN KEY (id_comentario) REFERENCES post_comentarios (id_comentario) ON DELETE CASCADE,
            CONSTRAINT fk_rep_user FOREIGN KEY (id_usuario_reporta) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        console.log('[migrate] tabla post_reportes creada');
      }

      // Columna para ocultar posts/comentarios moderados sin borrar histórico
      await DbMigrationService.addColumnIfMissing(
        'actividad_posts',
        'oculto',
        'TINYINT(1) NOT NULL DEFAULT 0'
      );
      await DbMigrationService.addColumnIfMissing(
        'post_comentarios',
        'oculto',
        'TINYINT(1) NOT NULL DEFAULT 0'
      );

      if (!(await DbMigrationService.tableExists('segmentos'))) {
        await db.query(`
          CREATE TABLE segmentos (
            id_segmento INT NOT NULL AUTO_INCREMENT,
            slug VARCHAR(32) NOT NULL,
            nombre VARCHAR(120) NOT NULL,
            tipo ENUM('VIRTUAL','GPS') NOT NULL DEFAULT 'VIRTUAL',
            distancia_m DECIMAL(10,2) NOT NULL,
            mejor_si_menor TINYINT(1) NOT NULL DEFAULT 1,
            categoria VARCHAR(32) NOT NULL DEFAULT 'CARRERA',
            lat_inicio DECIMAL(10,7) NULL,
            lng_inicio DECIMAL(10,7) NULL,
            lat_fin DECIMAL(10,7) NULL,
            lng_fin DECIMAL(10,7) NULL,
            activo TINYINT(1) NOT NULL DEFAULT 1,
            PRIMARY KEY (id_segmento),
            UNIQUE KEY uk_segmento_slug (slug)
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla segmentos creada');
      }

      if (!(await DbMigrationService.tableExists('segmento_esfuerzos'))) {
        await db.query(`
          CREATE TABLE segmento_esfuerzos (
            id_esfuerzo INT NOT NULL AUTO_INCREMENT,
            id_segmento INT NOT NULL,
            id_usuario INT NOT NULL,
            duracion_ms INT NOT NULL,
            gps_uuid VARCHAR(64) NULL,
            creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (id_esfuerzo),
            INDEX idx_se_segmento (id_segmento),
            INDEX idx_se_usuario (id_usuario),
            CONSTRAINT fk_se_segmento FOREIGN KEY (id_segmento) REFERENCES segmentos (id_segmento) ON DELETE CASCADE,
            CONSTRAINT fk_se_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON DELETE CASCADE
          ) ENGINE=InnoDB
        `);
        console.log('[migrate] tabla segmento_esfuerzos creada');
      }

      const SegmentosService = require('./SegmentosService');
      await SegmentosService.seedVirtuales();

      const EjerciciosCatalogoService = require('./EjerciciosCatalogoService');
      await EjerciciosCatalogoService.seedCatalogoAmpliado();
      const EjerciciosEntornoCatalogo = require('./EjerciciosEntornoCatalogo');
      await EjerciciosEntornoCatalogo.seedEjerciciosEntorno();
      await EjerciciosEntornoCatalogo.seedMetadatosExistentes();
      const EjerciciosBanco500Service = require('./EjerciciosBanco500Service');
      await EjerciciosBanco500Service.seedBanco500(false);

      const BancoPlanesImportService = require('./BancoPlanesImportService');
      const banco = await BancoPlanesImportService.importarBancoCompleto(false);

      for (const tbl of [
        'planes_generados_cache',
        'actividad_posts',
        'post_comentarios',
        'gps_actividades',
        'grupo_mensajes',
        'mensajes_chat',
        'usuarios'
      ]) {
        await DbMigrationService.ensureUtf8mb4Table(tbl);
      }

      console.log('[migrate] Esquema comprobado OK');
      return { banco };
    } catch (e) {
      console.error('[migrate] Error aplicando migraciones:', e.message);
      throw e;
    }
  }
}

module.exports = DbMigrationService;
