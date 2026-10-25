const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

let dialog;
const context = {
    document: {
        addEventListener() {}, body: { append() {} },
        createElement() {
            const events = {}, formEvents = {};
            const field = { value: 'medico.26', focus() {} };
            const error = { textContent: '' }, button = { disabled: false };
            dialog = {
                innerHTML: '', closed: false, field, error,
                setAttribute() {}, showModal() {}, remove() {},
                addEventListener(name, callback) { events[name] = callback; },
                close() { this.closed = true; events.close(); },
                querySelector(selector) {
                    return { form: { addEventListener(name, callback) { formEvents[name] = callback; } },
                        '#access-user': field, '#credentials-error': error, '[type="submit"]': button }[selector];
                },
                async submit() { await formEvents.submit({ preventDefault() {} }); }
            };
            return dialog;
        }
    }, Intl, Date, Error
};
vm.createContext(context);
vm.runInContext(fs.readFileSync(path.join(__dirname, '../src/main/resources/static/js/app.js'), 'utf8'), context);

(async () => {
    const credentials = { usuario: 'medico.26', password: 'GENERATED-SECRET' };
    const calls = [];
    context.api = async (url, options) => {
        calls.push({ url, options });
        return { username: JSON.parse(options.body).username };
    };
    let completed = context.showCredentials(credentials, 26);
    assert.match(dialog.innerHTML, /id="access-password" readonly/);
    assert.doesNotMatch(dialog.innerHTML, /id="access-user"[^>]*readonly/);
    dialog.field.value = '  Maya  ';
    await dialog.submit(); await completed;
    assert.equal(credentials.usuario, 'Maya');
    assert.equal(credentials.password, 'GENERATED-SECRET');
    assert.equal(calls[0].url, '/api/usuarios/medico/26/username');
    assert.deepEqual(JSON.parse(calls[0].options.body), { username: 'Maya' });

    completed = context.showCredentials(credentials, 26);
    dialog.field.value = 'Maya';
    await dialog.submit(); await completed;
    assert.equal(calls.length, 1, 'Sin cambio no se envía ninguna modificación');

    completed = context.showCredentials(credentials, 26);
    dialog.field.value = ' ';
    await dialog.submit();
    assert.ok(!dialog.closed);
    assert.ok(dialog.error.textContent.includes('obligatorio'));
    context.api = async () => { throw new Error('El username ya existe'); };
    dialog.field.value = 'ocupado';
    await dialog.submit();
    assert.ok(!dialog.closed);
    assert.equal(dialog.error.textContent, 'El username ya existe');
    assert.equal(credentials.usuario, 'Maya');
    assert.ok(dialog.innerHTML.includes('GENERATED-SECRET'));
    context.api = async () => ({ username: 'MayaNueva' });
    dialog.field.value = 'MayaNueva';
    await dialog.submit(); await completed;
    assert.equal(credentials.usuario, 'MayaNueva');
    assert.equal(credentials.password, 'GENERATED-SECRET');
    console.log('OK: usuario editable, contraseña fija, guardado, duplicados, reintento y nombre sin cambios.');
})().catch(error => { console.error(error); process.exitCode = 1; });
