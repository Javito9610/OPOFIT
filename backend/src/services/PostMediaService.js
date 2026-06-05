const fs = require('fs');
const path = require('path');

const UPLOAD_DIR = path.join(__dirname, '../../uploads/posts');
const MAX_BYTES = 2_500_000;

function ensureDir() {
  if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true });
  }
}

function guardarFotoPost(userId, imagenBase64) {
  ensureDir();
  const raw = String(imagenBase64 || '').trim();
  if (!raw) throw new Error('Imagen vacia');

  let b64 = raw;
  const dataMatch = raw.match(/^data:image\/[a-z+]+;base64,(.+)$/i);
  if (dataMatch) b64 = dataMatch[1];

  const buf = Buffer.from(b64, 'base64');
  if (!buf.length || buf.length > MAX_BYTES) {
    throw new Error('Imagen no valida (max 2.5 MB)');
  }

  const filename = `${Number(userId)}_${Date.now()}.jpg`;
  fs.writeFileSync(path.join(UPLOAD_DIR, filename), buf);
  return `/uploads/posts/${filename}`;
}

module.exports = { guardarFotoPost, UPLOAD_DIR };
