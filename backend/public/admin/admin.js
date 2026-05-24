const STORAGE_KEY = 'opofit_admin_config';

function loadConfig() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

function saveConfig(cfg) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
}

function toast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 3000);
}

async function api(path, options = {}) {
  const cfg = loadConfig();
  const base = (cfg.apiBase || '').replace(/\/?$/, '/');
  const res = await fetch(base + 'api/admin' + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Key': cfg.adminKey || '',
      ...(options.headers || {})
    }
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.msg || res.statusText);
  return data;
}

function renderOposiciones() {
  const panel = document.getElementById('panel');
  panel.innerHTML = `
    <div class="card"><h2>Oposiciones</h2><div id="list"></div></div>
    <div class="card"><h3>Nueva / editar</h3>
      <form class="grid" id="formOpo">
        <input type="hidden" name="id_oposicion" />
        <input name="nombre" placeholder="Nombre" required />
        <label><input type="checkbox" name="incluida_gratis" checked /> Tiene contenido gratuito (recomendado: siempre activo)</label>
        <button type="submit" class="primary">Guardar</button>
      </form>
    </div>`;
  api('/oposiciones')
    .then((r) => {
      document.getElementById('list').innerHTML =
        '<table><tr><th>ID</th><th>Nombre</th><th>Gratis</th></tr>' +
        r.data
          .map(
            (o) =>
              `<tr><td>${o.id_oposicion}</td><td>${o.nombre}</td><td>${o.incluida_gratis ? 'Sí' : 'No'}</td></tr>`
          )
          .join('') +
        '</table>';
    })
    .catch((e) => toast(e.message));
  document.getElementById('formOpo').onsubmit = async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    try {
      await api('/oposiciones', {
        method: 'POST',
        body: JSON.stringify({
          id_oposicion: fd.get('id_oposicion') || undefined,
          nombre: fd.get('nombre'),
          incluida_gratis: fd.get('incluida_gratis') === 'on'
        })
      });
      toast('Guardado');
      renderOposiciones();
    } catch (e) {
      toast(e.message);
    }
  };
}

function renderEjercicios() {
  document.getElementById('panel').innerHTML = `
    <div class="card"><h2>Ejercicios</h2><div id="list"></div></div>
    <div class="card"><form class="grid" id="formEj">
      <input name="nombre" placeholder="Nombre" required />
      <input name="video_url" placeholder="URL vídeo" />
      <textarea name="instrucciones_tecnicas" placeholder="Instrucciones" rows="3"></textarea>
      <button type="submit" class="primary">Añadir ejercicio</button>
    </form></div>`;
  api('/ejercicios')
    .then((r) => {
      document.getElementById('list').innerHTML =
        '<table><tr><th>ID</th><th>Nombre</th></tr>' +
        r.data.map((e) => `<tr><td>${e.id_ejercicio}</td><td>${e.nombre}</td></tr>`).join('') +
        '</table>';
    })
    .catch((e) => toast(e.message));
  document.getElementById('formEj').onsubmit = async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    try {
      await api('/ejercicios', {
        method: 'POST',
        body: JSON.stringify({
          nombre: fd.get('nombre'),
          video_url: fd.get('video_url'),
          instrucciones_tecnicas: fd.get('instrucciones_tecnicas')
        })
      });
      toast('Ejercicio creado');
      renderEjercicios();
    } catch (e) {
      toast(e.message);
    }
  };
}

function renderPruebas() {
  document.getElementById('panel').innerHTML = `
    <div class="card">
      <label>Oposición ID <input id="opoFilter" type="number" value="1" /></label>
      <button id="btnLoadPruebas" class="primary">Cargar</button>
      <div id="list"></div>
    </div>
    <div class="card"><form class="grid" id="formPrueba">
      <input name="nombre_prueba" placeholder="Nombre prueba" required />
      <textarea name="descripcion" placeholder="Descripción" required rows="2"></textarea>
      <input name="oposiciones_id_oposicion" type="number" placeholder="ID oposición" required />
      <label><input type="checkbox" name="mejor_si_es_menor" /> Mejor si es menor (tiempo)</label>
      <button type="submit" class="primary">Añadir prueba</button>
    </form></div>`;
  document.getElementById('btnLoadPruebas').onclick = () => {
    const id = document.getElementById('opoFilter').value;
    api('/pruebas?oposicion=' + id)
      .then((r) => {
        document.getElementById('list').innerHTML =
          '<table><tr><th>ID</th><th>Nombre</th></tr>' +
          r.data.map((p) => `<tr><td>${p.id_pruebas_oficiales}</td><td>${p.nombre_prueba}</td></tr>`).join('') +
          '</table>';
      })
      .catch((e) => toast(e.message));
  };
  document.getElementById('formPrueba').onsubmit = async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    try {
      await api('/pruebas', {
        method: 'POST',
        body: JSON.stringify({
          nombre_prueba: fd.get('nombre_prueba'),
          descripcion: fd.get('descripcion'),
          oposiciones_id_oposicion: Number(fd.get('oposiciones_id_oposicion')),
          mejor_si_es_menor: fd.get('mejor_si_es_menor') === 'on'
        })
      });
      toast('Prueba creada');
    } catch (e) {
      toast(e.message);
    }
  };
}

function renderBaremos() {
  document.getElementById('panel').innerHTML = `
    <div class="card"><form class="grid" id="formBaremo">
      <input name="pruebas_oficiales_id_pruebas_oficiales" type="number" placeholder="ID prueba" required />
      <select name="genero"><option>HOMBRE</option><option>MUJER</option></select>
      <input name="marca_valor" type="number" step="0.01" placeholder="Marca valor" required />
      <input name="nota" type="number" placeholder="Nota" required />
      <button type="submit" class="primary">Añadir baremo</button>
    </form></div>`;
  document.getElementById('formBaremo').onsubmit = async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    try {
      await api('/baremos', {
        method: 'POST',
        body: JSON.stringify({
          pruebas_oficiales_id_pruebas_oficiales: Number(fd.get('pruebas_oficiales_id_pruebas_oficiales')),
          genero: fd.get('genero'),
          marca_valor: Number(fd.get('marca_valor')),
          nota: Number(fd.get('nota'))
        })
      });
      toast('Baremo guardado');
    } catch (e) {
      toast(e.message);
    }
  };
}

function renderPush() {
  document.getElementById('panel').innerHTML = `
    <div class="card">
      <h2>Recordatorios de entreno</h2>
      <button id="btnRecordatorio" class="primary">Enviar a todos los usuarios con token FCM</button>
    </div>
    <div class="card">
      <h2>Noticia por oposición</h2>
      <form class="grid" id="formNoticia">
        <input name="idOposicion" type="number" placeholder="ID oposición" required />
        <input name="titulo" placeholder="Título" required />
        <textarea name="cuerpo" placeholder="Mensaje" required rows="3"></textarea>
        <button type="submit" class="primary">Enviar push</button>
      </form>
    </div>`;
  document.getElementById('btnRecordatorio').onclick = async () => {
    try {
      const r = await api('/notificaciones/recordatorios', { method: 'POST', body: '{}' });
      toast(`Enviados: ${r.enviados}/${r.total}`);
    } catch (e) {
      toast(e.message);
    }
  };
  document.getElementById('formNoticia').onsubmit = async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    try {
      const r = await api('/notificaciones/noticia', {
        method: 'POST',
        body: JSON.stringify({
          idOposicion: Number(fd.get('idOposicion')),
          titulo: fd.get('titulo'),
          cuerpo: fd.get('cuerpo')
        })
      });
      toast(`Enviados: ${r.enviados}`);
    } catch (e) {
      toast(e.message);
    }
  };
}

const tabs = {
  oposiciones: renderOposiciones,
  ejercicios: renderEjercicios,
  pruebas: renderPruebas,
  baremos: renderBaremos,
  push: renderPush
};

document.querySelectorAll('.tabs button').forEach((btn) => {
  btn.onclick = () => {
    document.querySelectorAll('.tabs button').forEach((b) => b.classList.remove('active'));
    btn.classList.add('active');
    tabs[btn.dataset.tab]();
  };
});

document.getElementById('btnSaveConfig').onclick = () => {
  saveConfig({
    apiBase: document.getElementById('apiBase').value,
    adminKey: document.getElementById('adminKey').value
  });
  toast('Configuración guardada');
};

const cfg = loadConfig();
document.getElementById('apiBase').value = cfg.apiBase || '';
document.getElementById('adminKey').value = cfg.adminKey || '';
renderOposiciones();
