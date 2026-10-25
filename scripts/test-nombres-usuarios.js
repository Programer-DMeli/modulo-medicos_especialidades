const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const body = { innerHTML: '', addEventListener() {} };
const context = {
    document: {
        addEventListener() {},
        querySelector() { return body; },
        body: { dataset: { isAdmin: 'true', canManage: 'true' } }
    },
    Intl, Date, Error
};
vm.createContext(context);
vm.runInContext(fs.readFileSync(path.join(__dirname, '../src/main/resources/static/js/app.js'), 'utf8'), context);

(async () => {
    assert.equal(context.doctorName({ nombreCompleto: 'Maria Jobs', nombres: 'Maria', apellidoPaterno: 'Martin' }), 'Maria Jobs');
    assert.equal(context.doctorName({ nombres: 'María José', apellidoPaterno: 'de la Cruz', apellidoMaterno: 'Rojas' }), 'María José de la Cruz Rojas');
    assert.equal(context.doctorName(null), 'Sin médico');
    context.api = async () => [{ idUsuario: 1, nombres: 'Maria Jobs', username: 'Maria', medicoId: 2, medicoCodigo: 'MED-00002', estado: true }];
    await context.listUsers();
    assert.equal((body.innerHTML.match(/<td(?:\s|>)/g) || []).length, 6);
    assert.ok(!body.innerHTML.includes('MED-00002'));
    assert.ok(!body.innerHTML.includes('data-reset-credentials'));
    assert.ok(!body.innerHTML.includes('Generar credenciales'));
    assert.ok(body.innerHTML.includes('/usuarios/editar/1'));
    context.api = async () => [];
    await context.listUsers();
    assert.ok(body.innerHTML.includes('colspan="6"'));
    context.api = async () => [{ idMedico: 2, nombreCompleto: 'María <Jobs>', nombres: 'Anterior', estado: 'ACTIVO' }];
    await context.listDoctors();
    assert.ok(body.innerHTML.includes('María &lt;Jobs&gt;'));
    assert.ok(!body.innerHTML.includes('Anterior'));
    console.log('OK: nombre de cuenta en Médicos; seis columnas en Usuarios; solo editar en acciones y escape HTML conservado.');
})().catch(error => { console.error(error); process.exitCode = 1; });
