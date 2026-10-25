const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

// Ejecuta la validación real del formulario sin cargar el navegador.
const context = { document: { addEventListener() {} }, Intl, Date, Error };
vm.createContext(context);
vm.runInContext(fs.readFileSync(path.join(__dirname, '../src/main/resources/static/js/app.js'), 'utf8'), context);
const today = context.today();
const future = context.addDays(today, 14);
const monday = context.addDays(future, (8 - new Date(`${future}T12:00:00`).getDay()) % 7);
const data = {
    medicoId: '1', especialidadId: '2', consultorioId: '3', diaSemana: 'Lunes',
    horaInicio: '08:00', horaFin: '12:00', duracionCita: '45', estado: 'ACTIVO',
    vigenteDesde: monday, vigenteHasta: context.addDays(monday, 7)
};
const valid = context.schedulePayload(data);
assert.equal(valid.duracionCita, 45);
assert.equal(valid.vigenteHasta, data.vigenteHasta);
assert.equal(valid.medico.idMedico, 1);
assert.throws(() => context.schedulePayload({ ...data, vigenteDesde: context.addDays(today, -1) }),
    error => error.field === 'vigenteDesde' && error.message.includes('fecha futura'));
assert.throws(() => context.schedulePayload({ ...data, vigenteHasta: context.addDays(monday, -1) }),
    error => error.field === 'vigenteHasta' && error.message.includes('anterior'));
assert.throws(() => context.schedulePayload({ ...data,
    vigenteDesde: context.addDays(monday, 1), vigenteHasta: context.addDays(monday, 2) }),
    error => error.field === 'vigenteHasta' && error.message.includes('al menos un lunes'));
assert.throws(() => context.schedulePayload({ ...data, duracionCita: '300' }),
    error => error.field === 'duracionCita');
const corrected = context.schedulePayload({ ...data, vigenteDesde: context.addDays(monday, 1) });
assert.equal(corrected.duracionCita, 45);
const originalStart = context.addDays(today, -14);
const edited = context.schedulePayload({ ...data, vigenteDesde: originalStart }, originalStart);
assert.equal(edited.vigenteDesde, originalStart);
assert.throws(() => context.schedulePayload({ ...data, vigenteDesde: context.addDays(today, -15) }, originalStart),
    error => error.field === 'vigenteDesde');
console.log('OK: duración y vigencia válidas; errores en el campo correspondiente; rango corregido aceptado.');
