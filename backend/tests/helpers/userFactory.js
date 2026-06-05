/**
 * Generador de muchos usuarios sintéticos para las pruebas masivas.
 */
const NOMBRES = [
  'Lucia', 'Marta', 'Sofia', 'Andrea', 'Paula', 'Elena', 'Clara', 'Sara', 'Ines', 'Maria',
  'Javier', 'Daniel', 'Pablo', 'Alvaro', 'Hugo', 'Mario', 'Carlos', 'David', 'Adrian', 'Rafael',
  'Nuria', 'Ainhoa', 'Yaiza', 'Itziar', 'Aitana', 'Vega', 'Carmen', 'Beatriz', 'Cristina', 'Rocio',
  'Diego', 'Bruno', 'Marc', 'Iker', 'Oscar', 'Sergio', 'Victor', 'Joel', 'Aaron', 'Manuel'
];
const APELLIDOS = [
  'Garcia', 'Lopez', 'Martin', 'Sanchez', 'Perez', 'Gonzalez', 'Rodriguez', 'Fernandez',
  'Ruiz', 'Hernandez', 'Diaz', 'Moreno', 'Munoz', 'Alvarez', 'Romero', 'Alonso',
  'Gutierrez', 'Navarro', 'Torres', 'Dominguez'
];
const OPOSICIONES = [1, 2, 3, 4, 5];

function generarUsuarios(n) {
  const usuarios = [];
  for (let i = 0; i < n; i++) {
    const nombre = NOMBRES[i % NOMBRES.length];
    const apellido = APELLIDOS[(i * 7) % APELLIDOS.length];
    const genero = nombre.endsWith('a') || nombre.endsWith('en') || nombre === 'Carmen' ? 'MUJER' : 'HOMBRE';
    const peso = 55 + (i % 35);
    const altura = 155 + (i % 40);
    const idOpo = OPOSICIONES[i % OPOSICIONES.length];
    usuarios.push({
      id_usuario: i + 1,
      nombre: `${nombre} ${apellido}`,
      email: `user${i + 1}@opofit.test`,
      password: 'Password123!',
      genero,
      peso,
      altura,
      imc: Number((peso / (altura / 100) ** 2).toFixed(2)),
      oposiciones_id_oposicion: idOpo,
      es_premium: i % 5 === 0 ? 1 : 0,
      perfil_publico: i % 2 === 0 ? 1 : 0
    });
  }
  return usuarios;
}

module.exports = { generarUsuarios };
