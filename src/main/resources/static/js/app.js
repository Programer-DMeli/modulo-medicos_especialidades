const jsonHeaders = { "Content-Type": "application/json" };

async function api(url, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const tokenHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (token && tokenHeader && !["GET", "HEAD", "OPTIONS"].includes((options.method || "GET").toUpperCase())) {
        headers.set(tokenHeader, token);
    }
    let response;
    try {
        response = await fetch(url, { ...options, headers });
    } catch (_) {
        throw new Error("No se pudo conectar con el servidor. Revisa tu conexión y vuelve a intentarlo.");
    }
    if (response.redirected && new URL(response.url).pathname === "/login") {
        throw new Error("Tu sesión terminó. Vuelve a iniciar sesión para continuar.");
    }
    if (!response.ok) {
        let message = `No se pudo completar la operación (${response.status})`;
        try {
            const error = await response.json();
            message = error.detail || error.message || error.error || message;
        } catch (_) {
            // Keep the HTTP status message when the server does not return JSON.
        }
        const error = new Error(message);
        error.status = response.status;
        throw error;
    }
    if (response.status === 204) return null;
    const body = await response.text();
    return body ? JSON.parse(body) : null;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function showMessage(message, type = "danger") {
    const container = document.querySelector("#mensaje");
    if (container) {
        container.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">${escapeHtml(message)}<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button></div>`;
    }
}

function setFlash(message) {
    sessionStorage.setItem("medical-flash", message);
}

function showStoredFlash() {
    const message = sessionStorage.getItem("medical-flash");
    if (message) {
        sessionStorage.removeItem("medical-flash");
        showMessage(message, "success");
    }
}

function statusBadge(status) {
    const normalized = String(status ?? "").toUpperCase();
    const positive = ["ACTIVO", "DISPONIBLE", "TRUE"].includes(normalized);
    const warning = ["VACACIONES", "OCUPADO"].includes(normalized);
    const label = normalized === "TRUE" ? "ACTIVA" : normalized === "FALSE" ? "INACTIVA" : normalized;
    const color = positive ? "success" : warning ? "warning text-dark" : "secondary";
    return `<span class="badge rounded-pill text-bg-${color}">${escapeHtml(label || "SIN ESTADO")}</span>`;
}

function doctorName(doctor) {
    if (!doctor) return "Sin médico";
    return [doctor.nombres, doctor.apellidoPaterno, doctor.apellidoMaterno].filter(Boolean).join(" ");
}

function fillSelect(select, items, value, label, placeholder, selectedValue) {
    select.innerHTML = "";
    if (placeholder !== null) select.add(new Option(placeholder, ""));
    items.forEach(item => select.add(new Option(label(item), value(item), false, String(value(item)) === String(selectedValue ?? ""))));
}

function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
}

async function submitForm(form, action) {
    const button = form.querySelector('button[type="submit"]');
    const originalLabel = button.innerHTML;
    form.querySelectorAll("[aria-invalid]").forEach(field => {
        field.removeAttribute("aria-invalid");
        if (field.dataset.originalDescription) field.setAttribute("aria-describedby", field.dataset.originalDescription);
        else field.removeAttribute("aria-describedby");
    });
    form.querySelectorAll(".field-error").forEach(error => error.remove());
    button.disabled = true;
    button.textContent = "Guardando…";
    form.setAttribute("aria-busy", "true");
    try {
        await action(formData(form));
    } catch (error) {
        showMessage(error.message);
        if (error.field && form.elements[error.field]) {
            const field = form.elements[error.field];
            field.setAttribute("aria-invalid", "true");
            const note = document.createElement("div");
            note.className = "field-error";
            note.id = `${error.field}-error`;
            note.textContent = error.message;
            field.dataset.originalDescription = field.getAttribute("aria-describedby") || "";
            field.setAttribute("aria-describedby", note.id);
            field.parentElement.append(note);
            field.focus();
        }
    } finally {
        button.disabled = false;
        button.innerHTML = originalLabel;
        form.removeAttribute("aria-busy");
    }
}

function invalidField(field, message) {
    const error = new Error(message);
    error.field = field;
    throw error;
}

function canProgram() { return document.body.dataset.canManage === "true"; }
function isAdmin() { return document.body.dataset.isAdmin === "true"; }
function canMaintainDoctors() { return isAdmin() || document.body.dataset.role === "MEDICO"; }
function today() { return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Lima" }).format(new Date()); }
function nowLocal() { return `${today()}T${new Intl.DateTimeFormat("en-GB", { timeZone: "America/Lima", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).format(new Date())}`; }
function addDays(value, days) {
    const date = new Date(`${value}T12:00:00`);
    date.setDate(date.getDate() + days);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
function formatDate(value, options = {}) {
    if (!value) return "Sin definir";
    return new Intl.DateTimeFormat("es-PE", { day: "numeric", month: "short", year: "numeric", ...options }).format(new Date(`${String(value).slice(0, 10)}T12:00:00`));
}

function emptyRow(columns, message) {
    return `<tr><td colspan="${columns}" class="empty-cell"><i class="bi bi-inbox"></i><span>${escapeHtml(message)}</span></td></tr>`;
}

async function listDoctors() {
    const doctors = await api("/api/medicos");
    const body = document.querySelector("#tabla-medicos");
    body.innerHTML = doctors.length ? doctors.map(doctor => `
        <tr>
            <td><strong>${escapeHtml(doctor.codigoMedico)}</strong></td>
            <td><div class="table-title">${escapeHtml(doctorName(doctor))}</div><small>Ingreso: ${escapeHtml(doctor.fechaIngreso)}</small></td>
            <td>${escapeHtml(doctor.tipoDocumento)} ${escapeHtml(doctor.numeroDocumento)}</td>
            <td>${escapeHtml(doctor.cmp)}<small>${escapeHtml((doctor.especialidades || []).map(e => e.nombre).join(", ") || "Pendiente de especialidad")}</small></td>
            <td><div>${escapeHtml(doctor.telefono || "-")}</div><small>${escapeHtml(doctor.correo || "Sin correo")}</small></td>
            <td>${statusBadge(doctor.estado)}</td>
            <td class="text-end"><div class="d-flex gap-2 justify-content-end"><a class="btn btn-sm btn-outline-primary" href="/horarios?medicoId=${doctor.idMedico}">Ver horario</a>${canMaintainDoctors() ? `<a class="btn btn-sm btn-outline-secondary" href="/medicos/registrar?id=${doctor.idMedico}">Editar</a><button class="btn btn-sm btn-outline-danger" data-delete-doctor="${doctor.idMedico}" aria-label="Eliminar a ${escapeHtml(doctorName(doctor))}"><i class="bi bi-trash" aria-hidden="true"></i></button>` : ""}</div></td>
        </tr>`).join("") : emptyRow(7, "No hay médicos registrados");

    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-doctor]");
        if (!button || !confirm("¿Eliminar este médico?")) return;
        try {
            await api(`/api/medicos/${button.dataset.deleteDoctor}`, { method: "DELETE" });
            setFlash("Médico eliminado correctamente");
            location.reload();
        } catch (error) {
            showMessage(error.message);
        }
    });
}

async function registerDoctor() {
    const form = document.querySelector("#form-medico");
    const id = new URLSearchParams(location.search).get("id");
    const [specialties, doctor] = await Promise.all([api("/api/especialidades"), id ? api(`/api/medicos/${id}`) : null]);
    const assigned = doctor?.especialidades?.map(e => e.idEspecialidad) || [];
    const choices = specialties.filter(e => e.estado || assigned.includes(e.idEspecialidad));
    document.querySelector("#especialidades-medico").innerHTML = choices.length ? choices.map(e => `<div class="form-check"><input class="form-check-input" type="checkbox" id="especialidad-${e.idEspecialidad}" name="especialidadIds" value="${e.idEspecialidad}" ${assigned.includes(e.idEspecialidad) ? "checked" : ""}><label class="form-check-label" for="especialidad-${e.idEspecialidad}">${escapeHtml(e.nombre)}${e.estado ? "" : " (inactiva)"}</label></div>`).join("") : '<p>No hay especialidades activas. Registra o activa una especialidad antes de crear al médico.</p>';
    if (doctor) {
        document.querySelector("h1").textContent = "Editar médico";
        document.title = "Editar médico";
        form.querySelector('[type="submit"]').textContent = "Guardar cambios";
        Object.entries(doctor).forEach(([key, value]) => { if (form.elements[key] && typeof value !== "object") form.elements[key].value = value ?? ""; });
        document.querySelector("#codigoMedico").value = doctor.codigoMedico;
        if (doctor.estado === "VACACIONES") {
            form.elements.estado.value = "ACTIVO";
            showMessage("Este registro usa el estado antiguo Vacaciones. Registra sus fechas en Ausencias y guarda el estado activo o suspendido.", "warning");
        }
    }
    const updateDocument = () => {
        const dni = form.elements.tipoDocumento.value === "DNI";
        form.elements.numeroDocumento.pattern = dni ? "[0-9]{8}" : "[A-Za-z0-9]{1,15}";
        form.elements.numeroDocumento.maxLength = dni ? 8 : 15;
        form.elements.numeroDocumento.inputMode = dni ? "numeric" : "text";
    };
    form.elements.tipoDocumento.addEventListener("change", updateDocument);
    updateDocument();
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.rne = data.rne || null;
            if (!/^9[0-9]{8}$/.test(data.telefono)) invalidField("telefono", "El celular debe tener 9 dígitos y empezar por 9.");
            data.correo = data.correo || null;
            data.especialidadIds = [...form.querySelectorAll('[name="especialidadIds"]:checked')].map(input => Number(input.value));
            if (!data.especialidadIds.length) { form.querySelector('[name="especialidadIds"]')?.focus(); throw new Error("Selecciona al menos una especialidad."); }
            const saved = await api(id ? `/api/medicos/${id}` : "/api/medicos", { method: id ? "PUT" : "POST", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash(id ? "Médico actualizado correctamente" : `Médico ${saved.codigoMedico} registrado. Configura su horario y consultorio.`);
            location.href = id || !canProgram() ? "/medicos" : `/horarios/registrar?medicoId=${saved.idMedico}`;
        });
    });
}

async function listSpecialties() {
    const specialties = await api("/api/especialidades");
    const body = document.querySelector("#tabla-especialidades");
    body.innerHTML = specialties.length ? specialties.map(item => `
        <tr>
            <td><strong>${escapeHtml(item.codigo)}</strong></td>
            <td><div class="table-title">${escapeHtml(item.nombre)}</div></td>
            <td>${escapeHtml(item.descripcion || "-")}</td>
            <td>${escapeHtml(item.duracionConsulta)} min</td>
            <td>${statusBadge(item.estado)}</td>
            <td class="text-end">${isAdmin() ? `<a class="btn btn-sm btn-outline-secondary" href="/especialidades/registrar?id=${item.idEspecialidad}">Editar</a> <button class="btn btn-sm btn-outline-danger" data-delete-specialty="${item.idEspecialidad}" aria-label="Eliminar ${escapeHtml(item.nombre)}"><i class="bi bi-trash" aria-hidden="true"></i></button>` : '<span class="text-muted">Consulta</span>'}</td>
        </tr>`).join("") : emptyRow(6, "No hay especialidades registradas");

    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-specialty]");
        if (!button || !confirm("¿Eliminar esta especialidad?")) return;
        try {
            await api(`/api/especialidades/${button.dataset.deleteSpecialty}`, { method: "DELETE" });
            setFlash("Especialidad eliminada correctamente");
            location.reload();
        } catch (error) {
            showMessage(error.message);
        }
    });
}

async function registerSpecialty() {
    const form = document.querySelector("#form-especialidad");
    const id = new URLSearchParams(location.search).get("id");
    if (id) {
        const specialty = await api(`/api/especialidades/${id}`);
        document.querySelector("h1").textContent = "Editar especialidad";
        document.title = "Editar especialidad";
        Object.entries(specialty).forEach(([key, value]) => { if (form.elements[key] && typeof value !== "object") form.elements[key].value = value ?? ""; });
        document.querySelector("#codigo").value = specialty.codigo;
    }
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.duracionConsulta = Number(data.duracionConsulta);
            data.estado = data.estado === "true";
            await api(id ? `/api/especialidades/${id}` : "/api/especialidades", { method: id ? "PUT" : "POST", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash(id ? "Especialidad actualizada correctamente" : "Especialidad registrada correctamente");
            location.href = "/especialidades";
        });
    });
}

async function specialtyStates() {
    const specialties = await api("/api/especialidades");
    const container = document.querySelector("#lista-estados");
    container.innerHTML = specialties.length ? specialties.map(item => `
        <div class="col-xl-4 col-md-6">
            <div class="card content-card h-100"><div class="card-body d-flex align-items-center justify-content-between gap-3">
                <div><small class="text-muted">${escapeHtml(item.codigo)}</small><h2 class="h5 mb-1">${escapeHtml(item.nombre)}</h2>${statusBadge(item.estado)}</div>
                <button class="btn btn-sm ${item.estado ? "btn-outline-danger" : "btn-outline-success"}" data-state-id="${item.idEspecialidad}" data-next-state="${!item.estado}">${item.estado ? "Inactivar" : "Activar"}</button>
            </div></div>
        </div>`).join("") : '<div class="col-12"><div class="empty-cell"><i class="bi bi-inbox"></i><span>No hay especialidades registradas</span></div></div>';
}

function bindSpecialtyStates() {
    const container = document.querySelector("#lista-estados");
    container.addEventListener("click", async event => {
        const button = event.target.closest("[data-state-id]");
        if (!button) return;
        try {
            await api(`/api/especialidades/${button.dataset.stateId}/estado?estado=${button.dataset.nextState}`, { method: "PUT" });
            showMessage("Estado actualizado correctamente", "success");
            await specialtyStates();
        } catch (error) {
            showMessage(error.message);
        }
    });
    return specialtyStates();
}

async function loadDoctorSelect(selectedValue, onlyActive = true) {
    const doctors = await api("/api/medicos");
    fillSelect(document.querySelector("#medicoId"), doctors.filter(item => !onlyActive || item.estado === "ACTIVO" || String(item.idMedico) === String(selectedValue)), item => item.idMedico, item => `${doctorName(item)}${item.estado !== "ACTIVO" ? ` (${item.estado.toLowerCase()})` : ""}`, "Selecciona un médico", selectedValue);
}

async function loadOfficeSelect(selectedValue, onlyAvailable = false) {
    let offices = await api("/api/consultorios");
    if (onlyAvailable) offices = offices.filter(item => item.estado !== "INACTIVO" || String(item.id) === String(selectedValue));
    fillSelect(document.querySelector("#consultorioId"), offices, item => item.id, item => `${item.codigo} · ${item.nombre}${item.estado === "INACTIVO" ? " (inactivo)" : ""}`, "Pendiente de asignar", selectedValue);
}

async function bindScheduleSpecialties(form, selectedSpecialty, selectedOffice) {
    const [doctors, offices] = await Promise.all([api("/api/medicos"), api("/api/consultorios")]);
    const specialtyField = form.elements.especialidadId;
    const renderOffices = selected => {
        const specialtyId = Number(specialtyField.value);
        const compatible = offices.filter(o => o.estado !== "INACTIVO" && (!o.especialidad || o.especialidad.idEspecialidad === specialtyId));
        fillSelect(form.elements.consultorioId, compatible, o => o.id, o => `${o.codigo} · ${o.nombre}`, "Pendiente de asignar", selected);
        if (selected && !compatible.some(o => String(o.id) === String(selected))) showMessage("El consultorio anterior no está operativo o no corresponde a esta especialidad. Selecciona uno compatible.", "warning");
    };
    const renderSpecialties = (selected, office) => {
        const doctor = doctors.find(d => String(d.idMedico) === form.elements.medicoId.value);
        const options = (doctor?.especialidades || []).filter(e => e.estado);
        fillSelect(specialtyField, options, e => e.idEspecialidad, e => e.nombre, options.length ? "Selecciona una especialidad" : "El médico no tiene especialidades activas", selected);
        if (!selected && options.length === 1) specialtyField.value = options[0].idEspecialidad;
        renderOffices(office);
        return options;
    };
    const defaultDuration = () => {
        const doctor = doctors.find(d => String(d.idMedico) === form.elements.medicoId.value);
        const specialty = doctor?.especialidades.find(e => String(e.idEspecialidad) === specialtyField.value);
        if (specialty) form.elements.duracionCita.value = specialty.duracionConsulta;
    };
    renderSpecialties(selectedSpecialty, selectedOffice);
    if (!selectedSpecialty) defaultDuration();
    form.elements.medicoId.addEventListener("change", () => { renderSpecialties(); defaultDuration(); });
    specialtyField.addEventListener("change", () => { renderOffices(); defaultDuration(); });
}

function schedulePayload(data) {
    if (data.horaInicio >= data.horaFin) invalidField("horaFin", "La hora de fin debe ser posterior a la hora de inicio.");
    const minutes = value => Number(value.slice(0, 2)) * 60 + Number(value.slice(3, 5));
    if (Number(data.duracionCita) <= 0 || Number(data.duracionCita) > minutes(data.horaFin) - minutes(data.horaInicio)) invalidField("duracionCita", "La duración debe ser positiva y caber dentro de la franja.");
    if (!data.vigenteDesde) invalidField("vigenteDesde", "Indica desde qué fecha se aplica este horario.");
    if (data.vigenteHasta && data.vigenteHasta < data.vigenteDesde) invalidField("vigenteHasta", "La fecha final no puede ser anterior al inicio de vigencia.");
    return {
        medico: { idMedico: Number(data.medicoId) },
        especialidad: data.especialidadId ? { idEspecialidad: Number(data.especialidadId) } : null,
        consultorio: data.consultorioId ? { id: Number(data.consultorioId) } : null,
        diaSemana: data.diaSemana,
        horaInicio: data.horaInicio,
        horaFin: data.horaFin,
        duracionCita: Number(data.duracionCita),
        estado: data.estado,
        vigenteDesde: data.vigenteDesde,
        vigenteHasta: data.vigenteHasta || null
    };
}

async function registerSchedule() {
    await Promise.all([loadDoctorSelect(), loadOfficeSelect(undefined, true)]);
    const form = document.querySelector("#form-horario");
    form.elements.vigenteDesde.value = today();
    form.elements.vigenteDesde.min = today();
    const selectedDoctor = new URLSearchParams(location.search).get("medicoId");
    if (selectedDoctor) form.elements.medicoId.value = selectedDoctor;
    await bindScheduleSpecialties(form);
    bindScheduleSummary(form);
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            await api("/api/horarios", { method: "POST", headers: jsonHeaders, body: JSON.stringify(schedulePayload(data)) });
            setFlash("Horario registrado correctamente");
            location.href = "/horarios";
        });
    });
}

async function listSchedules() {
    const [schedules, doctors] = await Promise.all([api("/api/horarios"), api("/api/medicos")]);
    const body = document.querySelector("#tabla-horarios");
    const doctorFilter = document.querySelector("#filtro-medico");
    if (doctorFilter) {
        fillSelect(doctorFilter, doctors, item => item.idMedico, doctorName, "Todos los médicos", new URLSearchParams(location.search).get("medicoId"));
    }
    const render = () => {
        const selectedDoctor = doctorFilter?.value;
        const createLink = document.querySelector('.page-heading a[href^="/horarios/registrar"]');
        if (createLink) createLink.href = selectedDoctor ? `/horarios/registrar?medicoId=${selectedDoctor}` : "/horarios/registrar";
        const selectedDay = document.querySelector("#filtro-dia")?.value;
        const selectedState = document.querySelector("#filtro-estado")?.value;
        const search = document.querySelector("#buscar-horarios")?.value.trim().toLocaleLowerCase("es") || "";
        const filtered = schedules.filter(item => (!selectedDoctor || String(item.medico.idMedico) === selectedDoctor)
            && (!selectedDay || item.diaSemana === selectedDay) && (!selectedState || item.estado === selectedState)
            && `${doctorName(item.medico)} ${item.consultorio?.codigo || ""} ${item.consultorio?.nombre || ""}`.toLocaleLowerCase("es").includes(search));
        body.innerHTML = filtered.length ? filtered.map(item => `
        <tr>
            <td><div class="table-title">${escapeHtml(doctorName(item.medico))}</div><small>${escapeHtml(item.especialidad?.nombre || "Revisar especialidad")} · ${escapeHtml(item.medico?.codigoMedico || "-")}</small></td>
            <td>${escapeHtml(item.diaSemana)}</td>
            <td><strong>${escapeHtml(String(item.horaInicio).slice(0, 5))} - ${escapeHtml(String(item.horaFin).slice(0, 5))}</strong></td>
            <td>${escapeHtml(item.duracionCita)} min</td>
            <td>${item.consultorio ? escapeHtml(`${item.consultorio.codigo} · ${item.consultorio.nombre}`) : '<span class="badge text-bg-warning">Pendiente</span>'}</td>
            <td>${item.vigenteDesde ? `${escapeHtml(formatDate(item.vigenteDesde))}<small>${item.vigenteHasta ? `Hasta ${escapeHtml(formatDate(item.vigenteHasta))}` : "Sin fecha final"}</small>` : '<span class="badge text-bg-warning">Revisar vigencia</span>'}</td>
            <td>${statusBadge(item.estado)}</td>
            <td class="text-end">${canProgram() ? `<div class="btn-group"><a class="btn btn-sm btn-outline-primary" href="/horarios/editar/${item.idHorario}">Editar</a><button class="btn btn-sm btn-outline-danger" data-delete-schedule="${item.idHorario}" aria-label="Retirar horario de ${escapeHtml(doctorName(item.medico))}"><i class="bi bi-trash" aria-hidden="true"></i></button></div>` : '<span class="text-muted">Consulta</span>'}</td>
        </tr>`).join("") : emptyRow(8, schedules.length ? "No hay horarios con estos filtros." : "Aún no hay horarios. Configura la primera franja habitual de un médico.");
    };
    render();
    document.querySelectorAll("#filtro-medico, #filtro-dia, #filtro-estado, #buscar-horarios").forEach(field => field.addEventListener("input", render));
    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-schedule]");
        if (!button || !confirm("¿Retirar este horario? Si ya comenzó, se conservará su historial y se cerrará su vigencia.")) return;
        try {
            await api(`/api/horarios/${button.dataset.deleteSchedule}`, { method: "DELETE" });
            setFlash("Horario retirado correctamente");
            location.reload();
        } catch (error) {
            showMessage(error.message);
        }
    });
}

async function editSchedule() {
    const id = document.body.dataset.id;
    const schedule = await api(`/api/horarios/${id}`);
    await Promise.all([loadDoctorSelect(schedule.medico?.idMedico), loadOfficeSelect(schedule.consultorio?.id, true)]);
    const form = document.querySelector("#form-horario");
    form.elements.diaSemana.value = schedule.diaSemana;
    form.elements.horaInicio.value = String(schedule.horaInicio).slice(0, 5);
    form.elements.horaFin.value = String(schedule.horaFin).slice(0, 5);
    form.elements.duracionCita.value = schedule.duracionCita;
    await bindScheduleSpecialties(form, schedule.especialidad?.idEspecialidad, schedule.consultorio?.id);
    form.elements.estado.value = schedule.estado;
    const started = schedule.vigenteDesde && schedule.vigenteDesde <= today();
    const finished = schedule.vigenteHasta && schedule.vigenteHasta < today();
    form.elements.vigenteDesde.value = started ? addDays(today(), 1) : schedule.vigenteDesde || today();
    form.elements.vigenteDesde.min = started ? addDays(today(), 1) : today();
    form.elements.vigenteHasta.value = schedule.vigenteHasta || "";
    if (finished) {
        showMessage("Este horario ya finalizó y se conserva como historial. Registra una nueva franja para programar otra vigencia.", "warning");
        [...form.elements].forEach(field => { field.disabled = true; });
    } else if (started) {
        showMessage("El cambio se aplicará desde la nueva fecha de vigencia. Se conservará la versión anterior hasta el día previo.", "warning");
    }
    bindScheduleSummary(form);
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            await api(`/api/horarios/${id}`, { method: "PUT", headers: jsonHeaders, body: JSON.stringify(schedulePayload(data)) });
            setFlash("Horario actualizado correctamente");
            location.href = "/horarios";
        });
    });
}

async function listOffices() {
    const offices = await api("/api/consultorios");
    const body = document.querySelector("#tabla-consultorios");
    body.innerHTML = offices.length ? offices.map(item => `
        <tr>
            <td><strong>${escapeHtml(item.codigo)}</strong></td>
            <td><div class="table-title">${escapeHtml(item.nombre)}</div></td>
            <td>Piso ${escapeHtml(item.piso)} · ${escapeHtml(item.area)}</td>
            <td>${escapeHtml(item.especialidad?.nombre || "Sin especialidad")}</td>
            <td>${statusBadge(item.estado)}</td>
            <td class="text-end">${canProgram() ? `<a class="btn btn-sm btn-outline-secondary" href="/consultorios/registrar?id=${item.id}">Editar</a> <button class="btn btn-sm btn-outline-danger" data-delete-office="${item.id}" aria-label="Eliminar consultorio ${escapeHtml(item.codigo)}"><i class="bi bi-trash" aria-hidden="true"></i></button>` : '<span class="text-muted">Consulta</span>'}</td>
        </tr>`).join("") : emptyRow(6, "No hay consultorios registrados");

    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-office]");
        if (!button || !confirm("¿Eliminar este consultorio?")) return;
        try {
            await api(`/api/consultorios/${button.dataset.deleteOffice}`, { method: "DELETE" });
            setFlash("Consultorio eliminado correctamente");
            location.reload();
        } catch (error) {
            showMessage(error.message);
        }
    });
}

async function registerOffice() {
    const id = new URLSearchParams(location.search).get("id");
    const [specialties, office] = await Promise.all([api("/api/especialidades"), id ? api(`/api/consultorios/${id}`) : null]);
    fillSelect(document.querySelector("#especialidadId"), specialties.filter(item => item.estado || item.idEspecialidad === office?.especialidad?.idEspecialidad), item => item.idEspecialidad, item => item.nombre, "Uso general", office?.especialidad?.idEspecialidad);
    const form = document.querySelector("#form-consultorio");
    if (office) {
        document.querySelector("h1").textContent = "Editar consultorio";
        document.title = "Editar consultorio";
        Object.entries(office).forEach(([key, value]) => { if (form.elements[key] && typeof value !== "object") form.elements[key].value = value ?? ""; });
        document.querySelector("#codigo").value = office.codigo;
        if (office.estado === "OCUPADO") form.elements.estado.value = "DISPONIBLE";
    }
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            const payload = {
                nombre: data.nombre,
                piso: Number(data.piso),
                area: data.area,
                estado: data.estado,
                especialidad: data.especialidadId ? { idEspecialidad: Number(data.especialidadId) } : null
            };
            await api(id ? `/api/consultorios/${id}` : "/api/consultorios", { method: id ? "PUT" : "POST", headers: jsonHeaders, body: JSON.stringify(payload) });
            setFlash(id ? "Consultorio actualizado correctamente" : "Consultorio registrado correctamente");
            location.href = "/consultorios";
        });
    });
}

async function assignOffice() {
    const [schedules, offices] = await Promise.all([api("/api/horarios"), api("/api/consultorios")]);
    const scheduleField = document.querySelector("#horarioId");
    fillSelect(scheduleField, schedules.filter(item => item.estado === "ACTIVO" && item.especialidad?.estado && item.medico?.estado === "ACTIVO" && item.vigenteDesde && (!item.vigenteHasta || item.vigenteHasta >= today())), item => item.idHorario, item => `${doctorName(item.medico)} · ${item.especialidad.nombre} · ${item.diaSemana} ${String(item.horaInicio).slice(0, 5)}–${String(item.horaFin).slice(0, 5)} · ${item.consultorio?.codigo || "Sin consultorio"}`, "Selecciona un horario");
    const renderOffices = () => {
        const schedule = schedules.find(h => String(h.idHorario) === scheduleField.value);
        fillSelect(document.querySelector("#consultorioId"), offices.filter(item => schedule && item.estado !== "INACTIVO" && (!item.especialidad || item.especialidad.idEspecialidad === schedule.especialidad.idEspecialidad)), item => item.id, item => `${item.codigo} · ${item.nombre}`, "Selecciona un consultorio");
    };
    scheduleField.addEventListener("change", renderOffices);
    renderOffices();
    const form = document.querySelector("#form-asignacion");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            await api(`/api/consultorios/${data.consultorioId}/horarios/${data.horarioId}`, { method: "PUT" });
            setFlash("Consultorio asignado correctamente");
            location.href = "/horarios";
        });
    });
}

// === PREGUNTA 4 - LUIS: Usuarios/Roles ===

async function listUsers() {
    const users = await api("/api/usuarios");
    const body = document.querySelector("#tabla-usuarios");
    body.innerHTML = users.length ? users.map(u => `
        <tr>
            <td><strong>${escapeHtml(u.username)}</strong></td>
            <td>${escapeHtml(u.nombres)}</td>
            <td>${escapeHtml(u.correo || "-")}</td>
            <td>${escapeHtml(u.rol?.nombre || "-")}</td>
            <td>${statusBadge(u.estado)}</td>
            <td class="text-end"><div class="btn-group">
                <a class="btn btn-sm btn-outline-primary" href="/usuarios/editar/${u.idUsuario}" title="Editar"><i class="bi bi-pencil"></i></a>
                <button class="btn btn-sm btn-outline-warning" data-toggle-user="${u.idUsuario}" data-active="${u.estado}" title="Activar/Desactivar"><i class="bi bi-toggle-on"></i></button>
                <button class="btn btn-sm btn-outline-danger" data-delete-user="${u.idUsuario}" title="Eliminar"><i class="bi bi-trash"></i></button>
            </div></td>
        </tr>`).join("") : emptyRow(6, "No hay usuarios registrados");

    body.addEventListener("click", async event => {
        const del = event.target.closest("[data-delete-user]");
        const tog = event.target.closest("[data-toggle-user]");
        if (del && confirm("¿Eliminar este usuario?")) {
            try { await api(`/api/usuarios/${del.dataset.deleteUser}`, { method: "DELETE" }); setFlash("Usuario eliminado"); location.reload(); } catch (e) { showMessage(e.message); }
        }
        if (tog) {
            const nuevo = tog.dataset.active === "true" ? "false" : "true";
            try { await api(`/api/usuarios/${tog.dataset.toggleUser}/estado?activo=${nuevo}`, { method: "PUT" }); setFlash("Estado actualizado"); location.reload(); } catch (e) { showMessage(e.message); }
        }
    });
}

function registerUser() {
    loadRoleSelect().catch(e => showMessage(e.message));
    const form = document.querySelector("#form-usuario");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            const payload = {
                username: data.username,
                password: data.password,
                nombres: data.nombres,
                correo: data.correo,
                estado: data.estado === "true",
                rol: data.rolId ? { idRol: Number(data.rolId) } : null
            };
            await api("/api/usuarios", { method: "POST", headers: jsonHeaders, body: JSON.stringify(payload) });
            setFlash("Usuario registrado correctamente");
            location.href = "/usuarios";
        });
    });
}

async function editUser() {
    const id = document.body.dataset.id;
    const user = await api(`/api/usuarios/${id}`);
    await loadRoleSelect(user.rol?.idRol);
    const form = document.querySelector("#form-usuario");
    form.elements.username.value = user.username;
    form.elements.password.value = "";
    form.elements.nombres.value = user.nombres;
    form.elements.correo.value = user.correo || "";
    form.elements.rolId.value = user.rol?.idRol || "";
    form.elements.estado.value = String(user.estado);
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            const payload = {
                username: data.username,
                password: data.password,
                nombres: data.nombres,
                correo: data.correo,
                estado: data.estado === "true",
                rol: data.rolId ? { idRol: Number(data.rolId) } : null
            };
            await api(`/api/usuarios/${id}`, { method: "PUT", headers: jsonHeaders, body: JSON.stringify(payload) });
            setFlash("Usuario actualizado correctamente");
            location.href = "/usuarios";
        });
    });
}

async function listRoles() {
    const roles = await api("/api/roles");
    const body = document.querySelector("#tabla-roles");
    body.innerHTML = roles.length ? roles.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.nombre)}</strong></td>
            <td>${escapeHtml(r.descripcion || "-")}</td>
            <td>${statusBadge(r.estado)}</td>
            <td class="text-end"><div class="btn-group">
                <a class="btn btn-sm btn-outline-primary" href="/roles/editar/${r.idRol}" aria-label="Editar ${escapeHtml(r.nombre)}"><i class="bi bi-pencil" aria-hidden="true"></i></a>
                <button class="btn btn-sm btn-outline-secondary" data-toggle-role="${r.idRol}" data-active="${r.estado}">${r.estado ? "Desactivar" : "Activar"}</button>
                <button class="btn btn-sm btn-outline-danger" data-delete-role="${r.idRol}" aria-label="Eliminar ${escapeHtml(r.nombre)}"><i class="bi bi-trash" aria-hidden="true"></i></button>
            </div></td>
        </tr>`).join("") : emptyRow(4, "No hay roles registrados");

    body.addEventListener("click", async event => {
        const del = event.target.closest("[data-delete-role]");
        const toggle = event.target.closest("[data-toggle-role]");
        if (toggle) {
            const active = toggle.dataset.active !== "true";
            if (!active && !confirm("¿Desactivar este rol? Los usuarios que lo tengan asignado no podrán acceder al sistema.")) return;
            try { await api(`/api/roles/${toggle.dataset.toggleRole}/estado?activo=${active}`, { method: "PUT" }); setFlash("Estado del rol actualizado"); location.reload(); } catch (error) { showMessage(error.message); }
        }
        if (del && confirm("¿Eliminar este rol?")) {
            try { await api(`/api/roles/${del.dataset.deleteRole}`, { method: "DELETE" }); setFlash("Rol eliminado"); location.reload(); } catch (e) { showMessage(e.message); }
        }
    });
}

function registerRole() {
    const form = document.querySelector("#form-rol");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.estado = data.estado === "true";
            await api("/api/roles", { method: "POST", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash("Rol registrado correctamente");
            location.href = "/roles";
        });
    });
}

async function editRole() {
    const id = document.body.dataset.id;
    const role = await api(`/api/roles/${id}`);
    const form = document.querySelector("#form-rol");
    form.elements.nombre.value = role.nombre;
    form.elements.descripcion.value = role.descripcion || "";
    form.elements.estado.value = String(role.estado);
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.estado = data.estado === "true";
            await api(`/api/roles/${id}`, { method: "PUT", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash("Rol actualizado correctamente");
            location.href = "/roles";
        });
    });
}

async function loadRoleSelect(selectedValue) {
    const roles = await api("/api/roles");
    fillSelect(document.querySelector("#rolId"), roles.filter(role => role.estado || String(role.idRol) === String(selectedValue)), r => r.idRol, r => `${r.nombre}${r.estado ? "" : " (inactivo)"}`, "Selecciona un rol", selectedValue);
}

function initUserForm() {
    loadRoleSelect().catch(e => showMessage(e.message));
}

async function initUsersPage() {
    await listUsers();
}

async function initRolesPage() {
    await listRoles();
}

// === PREGUNTA 2 - AUDITORIA: visor de registros ===

function auditOperation(operation) {
    const colors = { INSERT: "success", UPDATE: "warning text-dark", DELETE: "danger" };
    return `<span class="badge rounded-pill text-bg-${colors[operation] || "secondary"}">${escapeHtml(operation || "-")}</span>`;
}

async function listAudit() {
    const registros = await api("/api/auditoria");
    const body = document.querySelector("#tabla-auditoria");
    body.innerHTML = registros.length ? registros.map(a => `
        <tr>
            <td>${a.idAuditoria}</td>
            <td>${escapeHtml((a.fechaHora || "").replace("T", " "))}</td>
            <td><strong>${escapeHtml(a.usuario || "-")}</strong></td>
            <td>${auditOperation(a.operacion)}</td>
            <td>${escapeHtml(a.entidad || "-")}</td>
            <td>${a.registroId ?? "-"}</td>
        </tr>`).join("") : emptyRow(6, "No hay operaciones registradas");
}

function activateNavigation() {
    const section = location.pathname.split("/")[1];
    document.querySelectorAll(".app-sidebar a, .sidebar .list-group-item").forEach(link => {
        link.classList.toggle("active", section ? link.getAttribute("href") === `/${section}` : link.getAttribute("href") === "/");
        if (link.classList.contains("active")) link.setAttribute("aria-current", "page");
    });
    const toggle = document.querySelector("[data-toggle-sidebar]");
    const sidebar = document.querySelector(".app-sidebar");
    const main = document.querySelector(".app-main");
    const mobile = matchMedia("(max-width: 767px)");
    const setOpen = open => {
        document.body.classList.toggle("sidebar-open", open);
        toggle?.setAttribute("aria-expanded", String(open));
        if (sidebar) sidebar.inert = mobile.matches && !open;
        if (main) main.inert = mobile.matches && open;
    };
    const close = returnFocus => { setOpen(false); if (returnFocus) toggle?.focus(); };
    setOpen(false);
    toggle?.addEventListener("click", () => {
        const open = !document.body.classList.contains("sidebar-open");
        setOpen(open);
        if (open) sidebar?.querySelector("a")?.focus();
    });
    const backdrop = document.querySelector(".sidebar-backdrop");
    if (backdrop) { backdrop.tabIndex = -1; backdrop.addEventListener("click", () => close(true)); }
    mobile.addEventListener("change", () => close(false));
    document.addEventListener("keydown", event => {
        if (!mobile.matches || !document.body.classList.contains("sidebar-open")) return;
        if (event.key === "Escape") close(true);
        if (event.key === "Tab") {
            const links = [toggle, ...sidebar.querySelectorAll("a[href]")].filter(Boolean);
            if (event.shiftKey && document.activeElement === links[0]) { event.preventDefault(); links.at(-1).focus(); }
            else if (!event.shiftKey && document.activeElement === links.at(-1)) { event.preventDefault(); links[0].focus(); }
        }
    });
}

function bindScheduleSummary(form) {
    const summary = document.querySelector("#resumen-horario");
    if (!summary) return;
    const render = () => {
        const data = formData(form);
        summary.textContent = data.horaInicio && data.horaFin
            ? `${data.diaSemana}, de ${data.horaInicio} a ${data.horaFin}. Se repite cada semana desde ${formatDate(data.vigenteDesde)}${data.vigenteHasta ? ` hasta ${formatDate(data.vigenteHasta)}` : ""}. Las ausencias se aplican por fecha.`
            : "Completa la franja y su vigencia para revisar el horario habitual.";
    };
    form.addEventListener("input", render);
    render();
}

function agendaBlock(block, compact = false) {
    const kind = block.tipo || "ATENCION";
    const labels = { ATENCION: "Atención", AUSENCIA: "Ausencia", PENDIENTE: "Por completar" };
    const icons = { ATENCION: "bi-calendar-check", AUSENCIA: "bi-calendar-x", PENDIENTE: "bi-hourglass" };
    return `<article class="schedule-block type-${kind.toLowerCase()}" aria-label="${escapeHtml(`${labels[kind]}: ${doctorName(block.medico)}, ${block.fecha}, ${String(block.horaInicio).slice(0, 5)} a ${String(block.horaFin).slice(0, 5)}`)}">
        ${kind !== "ATENCION" ? `<span class="calendar-block-type"><i class="bi ${icons[kind]}" aria-hidden="true"></i>${labels[kind]}</span>` : ""}
        <span class="calendar-time">${escapeHtml(String(block.horaInicio).slice(0, 5))}–${escapeHtml(String(block.horaFin).slice(0, 5))}</span>
        <span class="calendar-doctor">${escapeHtml(doctorName(block.medico))}</span>
        <span class="calendar-office">${escapeHtml(block.especialidad?.nombre || "Especialidad pendiente")}</span>
        <span class="calendar-office">${escapeHtml(kind === "AUSENCIA" ? block.motivo || "No atiende" : block.consultorio ? `${block.consultorio.codigo}${compact ? "" : ` · ${block.consultorio.nombre}`}` : "Pendiente de asignar")}</span>
    </article>`;
}

async function initAgenda() {
    const dateField = document.querySelector("#agenda-desde");
    const date = today();
    const weekday = new Date(`${date}T12:00:00`).getDay();
    dateField.value = addDays(date, -(weekday === 0 ? 6 : weekday - 1));
    const [doctors, offices] = await Promise.all([api("/api/medicos"), api("/api/consultorios")]);
    fillSelect(document.querySelector("#agenda-medico"), doctors, item => item.idMedico, doctorName, "Todos los médicos", new URLSearchParams(location.search).get("medicoId"));
    fillSelect(document.querySelector("#agenda-consultorio"), offices, item => item.id, item => `${item.codigo} · ${item.nombre}`, "Todos los consultorios");
    let version = 0;
    const render = async () => {
        const request = ++version;
        const start = dateField.value;
        if (!start) return;
        const end = addDays(start, 6);
        const params = new URLSearchParams({ desde: start, hasta: end });
        const doctor = document.querySelector("#agenda-medico").value;
        const office = document.querySelector("#agenda-consultorio").value;
        if (doctor) params.set("medicoId", doctor);
        if (office) params.set("consultorioId", office);
        document.querySelector("#rango-agenda").textContent = `${formatDate(start, { year: undefined })} – ${formatDate(end)}`;
        const week = document.querySelector("#agenda-semanal");
        const list = document.querySelector("#agenda-lista");
        week.setAttribute("aria-busy", "true");
        list.setAttribute("aria-busy", "true");
        try {
            const blocks = await api(`/api/horarios/agenda?${params}`);
            if (request !== version) return;
            const days = Array.from({ length: 7 }, (_, index) => addDays(start, index));
            week.innerHTML = `<div class="calendar-grid">${days.map(day => {
                const items = blocks.filter(block => block.fecha === day);
                return `<section class="calendar-day ${day === today() ? "is-today" : ""}"><h2 class="calendar-day-heading"><span>${escapeHtml(formatDate(day, { weekday: "short", day: undefined, month: undefined, year: undefined }))}</span><strong>${day.slice(8)}</strong></h2><div class="calendar-day-body">${items.length ? items.map(block => agendaBlock(block, true)).join("") : '<p class="calendar-no-blocks">Sin atención</p>'}</div></section>`;
            }).join("")}</div>`;
            list.innerHTML = days.map(day => {
                const items = blocks.filter(block => block.fecha === day);
                return `<section class="agenda-list-day"><h2>${escapeHtml(formatDate(day, { weekday: "long" }))}${day === today() ? " · Hoy" : ""}</h2>${items.length ? items.map(block => agendaBlock(block)).join("") : '<p class="text-muted small">Sin atención programada.</p>'}</section>`;
            }).join("");
        } catch (error) {
            if (request !== version) return;
            week.innerHTML = list.innerHTML = '<div class="empty-cell"><span>No se pudo cargar la agenda.</span><button type="button" class="btn btn-outline-primary mt-3" data-retry-agenda>Reintentar</button></div>';
            showMessage(error.message);
            document.querySelectorAll("[data-retry-agenda]").forEach(button => button.addEventListener("click", render));
        } finally {
            if (request === version) { week.removeAttribute("aria-busy"); list.removeAttribute("aria-busy"); }
        }
    };
    const setView = view => {
        document.querySelector("#agenda-semanal").hidden = view !== "week";
        document.querySelector("#agenda-lista").hidden = view !== "list";
        [["#vista-semana", "week"], ["#vista-lista", "list"]].forEach(([selector, value]) => {
            const button = document.querySelector(selector);
            button.classList.toggle("active", value === view);
            button.setAttribute("aria-pressed", String(value === view));
        });
    };
    document.querySelector("#vista-semana").addEventListener("click", () => setView("week"));
    document.querySelector("#vista-lista").addEventListener("click", () => setView("list"));
    setView(matchMedia("(max-width: 767px)").matches ? "list" : "week");
    document.querySelector("#semana-anterior").addEventListener("click", () => { dateField.value = addDays(dateField.value, -7); render(); });
    document.querySelector("#semana-siguiente").addEventListener("click", () => { dateField.value = addDays(dateField.value, 7); render(); });
    document.querySelector("#semana-hoy").addEventListener("click", () => { const current = today(); const day = new Date(`${current}T12:00:00`).getDay(); dateField.value = addDays(current, -(day === 0 ? 6 : day - 1)); render(); });
    document.querySelectorAll("#agenda-desde, #agenda-medico, #agenda-consultorio").forEach(field => field.addEventListener("change", render));
    await render();
}

async function initAbsences() {
    const body = document.querySelector("#tabla-ausencias");
    const render = async () => {
        const items = await api("/api/horarios/ausencias");
        body.innerHTML = items.length ? items.map(item => `<tr><td><div class="table-title">${escapeHtml(doctorName(item.medico))}</div></td>
            <td>${escapeHtml(formatDate(item.inicio))} · ${escapeHtml(item.inicio.slice(11, 16))}<small>Hasta ${escapeHtml(formatDate(item.fin))} · ${escapeHtml(item.fin.slice(11, 16))}</small></td>
            <td>${escapeHtml(item.motivo)}</td><td>${item.estado === "ACTIVO" && item.fin <= nowLocal() ? '<span class="badge text-bg-secondary">Finalizada</span>' : statusBadge(item.estado)}</td>
            <td class="text-end">${canProgram() && item.estado === "ACTIVO" && item.fin > nowLocal() ? `<button class="btn btn-sm btn-outline-danger" data-cancel-absence="${item.idAusencia}">${item.inicio <= nowLocal() ? "Finalizar" : "Anular"}</button>` : '<span class="text-muted">—</span>'}</td></tr>`).join("") : emptyRow(5, "No hay ausencias registradas. El horario habitual se aplica con normalidad.");
    };
    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-cancel-absence]");
        if (!button || !confirm("¿Retirar esta ausencia? Se conservará el tramo pasado y volverá a aplicarse el horario habitual desde ahora.")) return;
        try { await api(`/api/horarios/ausencias/${button.dataset.cancelAbsence}`, { method: "DELETE" }); await render(); showMessage("Ausencia actualizada. La agenda refleja el cambio y se conserva el historial.", "success"); } catch (error) { showMessage(error.message); }
    });
    const form = document.querySelector("#form-ausencia");
    if (form) {
        await loadDoctorSelect(undefined, false);
        form.elements.inicio.value = `${today()}T08:00`;
        form.elements.fin.value = `${today()}T18:00`;
        form.addEventListener("submit", event => {
            event.preventDefault();
            submitForm(form, async data => {
                if (data.fin <= data.inicio) invalidField("fin", "El final de la ausencia debe ser posterior al inicio.");
                if (!data.motivo.trim()) invalidField("motivo", "Indica el motivo de la ausencia.");
                if (data.motivo.trim().length > 200) invalidField("motivo", "El motivo admite hasta 200 caracteres.");
                await api("/api/horarios/ausencias", { method: "POST", headers: jsonHeaders, body: JSON.stringify({ medico: { idMedico: Number(data.medicoId) }, inicio: data.inicio, fin: data.fin, motivo: data.motivo.trim(), estado: "ACTIVO" }) });
                form.elements.motivo.value = "";
                await render();
                showMessage("Ausencia registrada. La agenda ya refleja el cambio.", "success");
            });
        });
    }
    await render();
}

async function initHome() {
    const [doctors, schedules, blocks] = await Promise.all([api("/api/medicos"), api("/api/horarios"), api(`/api/horarios/agenda?desde=${today()}&hasta=${today()}`)]);
    const pending = schedules.filter(item => item.estado === "ACTIVO" && (!item.especialidad || !item.consultorio || !item.vigenteDesde)).length;
    const attention = blocks.filter(item => item.tipo === "ATENCION");
    document.querySelector("#resumen-inicio").innerHTML = `<div class="summary-list"><div class="summary-item"><strong>${doctors.filter(item => item.estado === "ACTIVO").length}</strong><span>Médicos activos</span></div><div class="summary-item"><strong>${attention.length}</strong><span>Franjas de hoy</span></div><div class="summary-item"><strong>${pending}</strong><span>Horarios por completar</span></div></div>`;
    document.querySelector("#agenda-hoy").innerHTML = blocks.length ? blocks.map(block => `<div class="today-row"><span class="time-range">${escapeHtml(String(block.horaInicio).slice(0, 5))}–${escapeHtml(String(block.horaFin).slice(0, 5))}</span><div class="today-info"><strong>${escapeHtml(doctorName(block.medico))}</strong><small>${escapeHtml(block.tipo === "AUSENCIA" ? block.motivo || "Ausencia" : block.consultorio ? `${block.consultorio.codigo} · ${block.consultorio.nombre}` : "Pendiente de consultorio")}</small></div><span class="badge ${block.tipo === "ATENCION" ? "text-bg-success" : block.tipo === "AUSENCIA" ? "text-bg-secondary" : "text-bg-warning"}">${block.tipo === "ATENCION" ? "Atención" : block.tipo === "AUSENCIA" ? "Ausencia" : "Pendiente"}</span></div>`).join("") : '<div class="empty-cell"><i class="bi bi-calendar2-check" aria-hidden="true"></i><span>No hay atención programada para hoy.</span><a href="/horarios/agenda">Consultar la semana</a></div>';
}

function bindCatalogSearch() {
    const body = document.querySelector("#tabla-medicos, #tabla-especialidades, #tabla-consultorios, #tabla-usuarios, #tabla-roles");
    if (!body || body.querySelector(".empty-cell")) return;
    let field = document.querySelector("[data-table-search]");
    if (!field) {
        const toolbar = document.createElement("div");
        toolbar.className = "filter-bar";
        toolbar.innerHTML = '<div class="search-field"><label for="buscar-catalogo" class="form-label">Buscar en el listado</label><input type="search" id="buscar-catalogo" data-table-search class="form-control" placeholder="Nombre, código u otro dato…"></div>';
        body.closest(".content-card").before(toolbar);
        field = toolbar.querySelector("input");
    }
    field.addEventListener("input", () => {
        const value = field.value.trim().toLocaleLowerCase("es");
        body.querySelectorAll("tr").forEach(row => { row.hidden = !row.textContent.toLocaleLowerCase("es").includes(value); });
    });
}

const pageInitializers = {
    "inicio": initHome,
    "medicos-listar": listDoctors,
    "medicos-registrar": registerDoctor,
    "especialidades-listar": listSpecialties,
    "especialidades-registrar": registerSpecialty,
    "especialidades-estado": bindSpecialtyStates,
    "horarios-listar": listSchedules,
    "horarios-registrar": registerSchedule,
    "horarios-editar": editSchedule,
    "horarios-agenda": initAgenda,
    "horarios-ausencias": initAbsences,
    "consultorios-listar": listOffices,
    "consultorios-registrar": registerOffice,
    "consultorios-asignar": assignOffice,
    // Pregunta 4 - Luis: Usuarios/Roles
    "usuarios-listar": listUsers,
    "usuarios-registrar": registerUser,
    "usuarios-editar": editUser,
    "roles-listar": listRoles,
    "roles-registrar": registerRole,
    "roles-editar": editRole,
    "auditoria-listar": listAudit
};

document.addEventListener("DOMContentLoaded", async () => {
    activateNavigation();
    showStoredFlash();
    const initializer = pageInitializers[document.body.dataset.page];
    const buttons = initializer ? [...document.querySelectorAll('form[id] button[type="submit"]')] : [];
    buttons.forEach(button => { button.disabled = true; });
    try {
        await initializer?.();
        buttons.forEach(button => { if (!button.closest("form").querySelector("input:disabled")) button.disabled = false; });
        bindCatalogSearch();
    } catch (error) {
        showMessage(error.message);
        document.querySelectorAll("tbody").forEach(body => {
            if (body.querySelector(".loading-cell")) body.innerHTML = emptyRow(body.closest("table").querySelectorAll("thead th").length, "No se pudo cargar el listado. Recarga la página para reintentar.");
        });
        document.querySelectorAll(".loading-cell").forEach(cell => {
            cell.innerHTML = '<span>No se pudo cargar la información.</span><button class="btn btn-outline-primary mt-3" type="button" data-retry-page>Reintentar</button>';
        });
        document.querySelectorAll("[data-retry-page]").forEach(button => button.addEventListener("click", () => location.reload()));
        document.querySelectorAll("#agenda-desde, #agenda-medico, #agenda-consultorio, .agenda-nav button, .view-switch button").forEach(control => { control.disabled = true; });
        document.querySelectorAll('form button[type="submit"]').forEach(button => { button.disabled = true; });
    }
});
