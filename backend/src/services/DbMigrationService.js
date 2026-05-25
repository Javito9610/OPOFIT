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

      const EjerciciosCatalogoService = require('./EjerciciosCatalogoService');
      await EjerciciosCatalogoService.seedCatalogoAmpliado();

      const BancoPlanesImportService = require('./BancoPlanesImportService');
      const banco = await BancoPlanesImportService.importarBancoCompleto(false);

      console.log('[migrate] Esquema comprobado OK');
      return { banco };
    } catch (e) {
      console.error('[migrate] Error aplicando migraciones:', e.message);
      throw e;
    }
  }
}

module.exports = DbMigrationService;
