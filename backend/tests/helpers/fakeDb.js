/**
 * Mock en memoria del pool MySQL2/promise.
 *
 * Compatible con db.query(...) y db.getConnection() -> conn.query/begin/commit/rollback/release.
 *
 * No reimplementa SQL real; en su lugar permite registrar respuestas por patrón regex
 * sobre la SQL, en orden FIFO. Si no encuentra patrón devuelve [[]] vacío.
 */

const queue = [];
let log = [];

function reset() {
  queue.length = 0;
  log = [];
}

function whenSql(pattern, response) {
  queue.push({ pattern, response, used: false, once: true });
}

function whenSqlMany(pattern, response) {
  queue.push({ pattern, response, used: false, once: false });
}

function logQuery(sql, params) {
  log.push({ sql, params });
}

function findResponse(sql) {
  for (const entry of queue) {
    if (entry.used && entry.once) continue;
    if (entry.pattern instanceof RegExp ? entry.pattern.test(sql) : sql.includes(entry.pattern)) {
      entry.used = true;
      return entry.response;
    }
  }
  return null;
}

async function query(sql, params) {
  logQuery(sql, params || []);
  const resp = findResponse(sql);
  if (resp === null) {
    // Por defecto: sin filas / sin filas afectadas
    return [[], []];
  }
  if (typeof resp === 'function') return resp(sql, params);
  return resp;
}

async function getConnection() {
  return {
    query,
    beginTransaction: jest.fn().mockResolvedValue(undefined),
    commit: jest.fn().mockResolvedValue(undefined),
    rollback: jest.fn().mockResolvedValue(undefined),
    release: jest.fn()
  };
}

module.exports = {
  query: jest.fn(query),
  getConnection: jest.fn(getConnection),
  whenSql,
  whenSqlMany,
  reset,
  _log: () => log
};
