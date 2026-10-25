const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const table = { innerHTML: '', addEventListener() {} };
const heading = { textContent: '' }, button = { textContent: '' };
const listeners = {};
const elements = Object.fromEntries(['diaSemana', 'horaInicio', 'horaFin', 'duracionCita', 'estado', 'vigenteDesde', 'vigenteHasta']
    .map(name => [name, { value: '', addEventListener() {} }]));
const form = { elements, querySelector() { return button; }, addEventListener(name, callback) { listeners[name] = callback; } };
const context = {
    document: {
        body: { dataset: { id: '12', canManage: 'true', role: 'ADMINISTRADOR' } },
        addEventListener() {}, querySelectorAll() { return []; },
        querySelector(selector) {
            return ({ '#tabla-horarios': table, '#form-horario': form, '.page-heading h1': heading })[selector] || null;
        }
    },
    location: { search: '' }, Intl, Date, Error, URLSearchParams
};
vm.createContext(context);
vm.runInContext(fs.readFileSync(path.join(__dirname, '../src/main/resources/static/js/app.js'), 'utf8'), context);

(async () => {
    // Fechas fijas para cubrir los límites sin depender del día de ejecución.
    context.today = () => '2026-10-24';
    const expired = { idHorario: 12, vigenteDesde: '2026-10-14', vigenteHasta: '2026-10-20',
        medico: { idMedico: 1, nombreCompleto: 'Maya Garcia Rojas' }, diaSemana: 'Lunes',
        horaInicio: '08:30', horaFin: '10:30', duracionCita: 30, estado: 'ACTIVO' };
    assert.equal(context.scheduleExpired(expired), true);
    assert.equal(context.scheduleExpired({ vigenteHasta: '2026-10-24' }), false);
    assert.equal(context.scheduleExpired({ vigenteHasta: '2026-10-25' }), false);
    assert.equal(context.scheduleExpired({ vigenteHasta: null }), false);
    assert.ok(context.scheduleValidity(expired).includes('Vigencia Vencida'));
    assert.ok(!context.scheduleValidity({ ...expired, vigenteHasta: '2026-10-24' }).includes('Vigencia Vencida'));
    const dates = context.scheduleEditDates(expired);
    assert.equal(dates.minimum, '2026-10-24');
    assert.equal(dates.desde, '2026-10-24');
    assert.equal(dates.hasta, '');
    const endsToday = context.scheduleEditDates({ ...expired, vigenteHasta: '2026-10-24' });
    assert.equal(endsToday.minimum, '2026-10-14');
    assert.equal(endsToday.desde, '2026-10-14');
    assert.equal(endsToday.hasta, '2026-10-24');

    context.api = async url => url === '/api/horarios' ? [expired] : [];
    await context.listSchedules();
    assert.ok(table.innerHTML.includes('Vigencia Vencida'));
    assert.ok(!table.innerHTML.includes('Renovar vigencia'));
    assert.ok(!table.innerHTML.includes('/horarios/editar/'));
    assert.ok(table.innerHTML.includes('data-delete-schedule="12"'));
    assert.ok(table.innerHTML.includes('data-historical="true"'));
    assert.equal((context.scheduleActions(expired).match(/<button/g) || []).length, 1);
    assert.ok(context.scheduleActions({ ...expired, vigenteHasta: '2026-10-24' }).includes('/horarios/editar/12'));
    context.document.body.dataset.canManage = 'false';
    assert.ok(!context.scheduleActions(expired).includes('<button'));
    context.document.body.dataset.canManage = 'true';

    const calls = [];
    context.api = async (url, options) => { calls.push({ url, options }); return expired; };
    context.loadDoctorSelect = context.loadOfficeSelect = context.bindScheduleSpecialties = async () => {};
    context.bindScheduleSummary = () => {};
    let message = '';
    context.showMessage = text => { message = text; };
    context.setFlash = () => {};
    await context.editSchedule();
    assert.ok(message.includes('Vigencia Vencida'));
    assert.equal(heading.textContent, 'Renovar vigencia');
    assert.equal(elements.vigenteDesde.value, '2026-10-24');
    assert.equal(elements.vigenteHasta.value, '');
    assert.ok(Object.values(elements).every(field => !field.disabled));
    context.submitForm = async (_, save) => save({ medicoId: '1', diaSemana: 'Lunes',
        horaInicio: '08:30', horaFin: '10:30', duracionCita: '30', estado: 'ACTIVO',
        vigenteDesde: '2026-10-26', vigenteHasta: '2026-11-30' });
    await listeners.submit({ preventDefault() {} });
    // El callback del formulario inicia el guardado asíncrono.
    await Promise.resolve();
    const saved = calls.find(call => call.options?.method === 'PUT');
    assert.equal(saved.url, '/api/horarios/12');
    assert.equal(JSON.parse(saved.options.body).vigenteHasta, '2026-11-30');
    const active = { ...expired, vigenteHasta: '2026-11-30' };
    context.api = async (url, options) => { calls.push({ url, options }); return active; };
    await context.editSchedule();
    assert.equal(elements.vigenteDesde.value, '2026-10-14');
    assert.equal(elements.vigenteHasta.value, '2026-11-30');
    assert.ok(message.includes('este mismo horario'));
    context.submitForm = async (_, save) => save({ medicoId: '1', diaSemana: 'Martes',
        horaInicio: '09:00', horaFin: '12:00', duracionCita: '45', estado: 'ACTIVO',
        vigenteDesde: '2026-10-14', vigenteHasta: '2026-11-30' });
    await listeners.submit({ preventDefault() {} });
    await Promise.resolve();
    const update = calls.at(-1);
    assert.equal(update.url, '/api/horarios/12');
    assert.equal(update.options.method, 'PUT');
    assert.equal(JSON.parse(update.options.body).vigenteDesde, '2026-10-14');
    assert.equal(JSON.parse(update.options.body).diaSemana, 'Martes');
    console.log('OK: vencimiento en Lima, límites de fecha, aviso, renovación habilitada y envío de fechas nuevas.');
})().catch(error => { console.error(error); process.exitCode = 1; });
