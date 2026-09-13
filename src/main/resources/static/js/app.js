const jsonHeaders = { "Content-Type": "application/json" };

async function api(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = `No se pudo completar la operación (${response.status})`;
        try {
            const error = await response.json();
            message = error.detail || error.message || error.error || message;
        } catch (_) {
            // Keep the HTTP status message when the server does not return JSON.
        }
        throw new Error(message);
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
    button.disabled = true;
    try {
        await action(formData(form));
    } catch (error) {
        showMessage(error.message);
        button.disabled = false;
    }
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
            <td>${escapeHtml(doctor.cmp)}</td>
            <td><div>${escapeHtml(doctor.telefono || "-")}</div><small>${escapeHtml(doctor.correo || "Sin correo")}</small></td>
            <td>${statusBadge(doctor.estado)}</td>
            <td class="text-end"><button class="btn btn-sm btn-outline-danger" data-delete-doctor="${doctor.idMedico}" title="Eliminar"><i class="bi bi-trash"></i></button></td>
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

function registerDoctor() {
    const form = document.querySelector("#form-medico");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.rne = data.rne || null;
            data.telefono = data.telefono || null;
            data.correo = data.correo || null;
            await api("/api/medicos", { method: "POST", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash("Médico registrado correctamente");
            location.href = "/medicos";
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
            <td class="text-end"><button class="btn btn-sm btn-outline-danger" data-delete-specialty="${item.idEspecialidad}" title="Eliminar"><i class="bi bi-trash"></i></button></td>
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

function registerSpecialty() {
    const form = document.querySelector("#form-especialidad");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            data.duracionConsulta = Number(data.duracionConsulta);
            data.estado = data.estado === "true";
            await api("/api/especialidades", { method: "POST", headers: jsonHeaders, body: JSON.stringify(data) });
            setFlash("Especialidad registrada correctamente");
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

async function loadDoctorSelect(selectedValue) {
    const doctors = await api("/api/medicos");
    fillSelect(document.querySelector("#medicoId"), doctors, item => item.idMedico, doctorName, "Selecciona un médico", selectedValue);
}

async function loadOfficeSelect(selectedValue, onlyAvailable = false) {
    let offices = await api("/api/consultorios");
    if (onlyAvailable) offices = offices.filter(item => item.estado === "DISPONIBLE");
    fillSelect(document.querySelector("#consultorioId"), offices, item => item.id, item => `${item.codigo} - ${item.nombre}`, "Sin asignar", selectedValue);
}

function schedulePayload(data) {
    if (data.horaInicio >= data.horaFin) throw new Error("La hora de fin debe ser posterior a la hora de inicio");
    return {
        medico: { idMedico: Number(data.medicoId) },
        consultorio: data.consultorioId ? { id: Number(data.consultorioId) } : null,
        diaSemana: data.diaSemana,
        horaInicio: data.horaInicio,
        horaFin: data.horaFin,
        duracionCita: Number(data.duracionCita),
        estado: data.estado
    };
}

async function registerSchedule() {
    await Promise.all([loadDoctorSelect(), loadOfficeSelect(undefined, true)]);
    const form = document.querySelector("#form-horario");
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
    const schedules = await api("/api/horarios");
    const body = document.querySelector("#tabla-horarios");
    body.innerHTML = schedules.length ? schedules.map(item => `
        <tr>
            <td><div class="table-title">${escapeHtml(doctorName(item.medico))}</div><small>${escapeHtml(item.medico?.codigoMedico || "-")}</small></td>
            <td>${escapeHtml(item.diaSemana)}</td>
            <td><strong>${escapeHtml(String(item.horaInicio).slice(0, 5))} - ${escapeHtml(String(item.horaFin).slice(0, 5))}</strong></td>
            <td>${escapeHtml(item.duracionCita)} min</td>
            <td>${item.consultorio ? escapeHtml(`${item.consultorio.codigo} - ${item.consultorio.nombre}`) : '<span class="text-muted">Sin asignar</span>'}</td>
            <td>${statusBadge(item.estado)}</td>
            <td class="text-end"><div class="btn-group"><a class="btn btn-sm btn-outline-primary" href="/horarios/editar/${item.idHorario}" title="Editar"><i class="bi bi-pencil"></i></a><button class="btn btn-sm btn-outline-danger" data-delete-schedule="${item.idHorario}" title="Eliminar"><i class="bi bi-trash"></i></button></div></td>
        </tr>`).join("") : emptyRow(7, "No hay horarios registrados");

    body.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-schedule]");
        if (!button || !confirm("¿Eliminar este horario?")) return;
        try {
            await api(`/api/horarios/${button.dataset.deleteSchedule}`, { method: "DELETE" });
            setFlash("Horario eliminado correctamente");
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
    form.elements.estado.value = schedule.estado;
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
            <td class="text-end"><button class="btn btn-sm btn-outline-danger" data-delete-office="${item.id}" title="Eliminar"><i class="bi bi-trash"></i></button></td>
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
    const specialties = await api("/api/especialidades");
    fillSelect(document.querySelector("#especialidadId"), specialties.filter(item => item.estado), item => item.idEspecialidad, item => item.nombre, "Sin especialidad");
    const form = document.querySelector("#form-consultorio");
    form.addEventListener("submit", event => {
        event.preventDefault();
        submitForm(form, async data => {
            const payload = {
                codigo: data.codigo,
                nombre: data.nombre,
                piso: Number(data.piso),
                area: data.area,
                estado: data.estado,
                especialidad: data.especialidadId ? { idEspecialidad: Number(data.especialidadId) } : null
            };
            await api("/api/consultorios", { method: "POST", headers: jsonHeaders, body: JSON.stringify(payload) });
            setFlash("Consultorio registrado correctamente");
            location.href = "/consultorios";
        });
    });
}

async function assignOffice() {
    const [schedules, offices] = await Promise.all([api("/api/horarios"), api("/api/consultorios")]);
    fillSelect(document.querySelector("#horarioId"), schedules, item => item.idHorario, item => `${doctorName(item.medico)} · ${item.diaSemana} ${String(item.horaInicio).slice(0, 5)}`, "Selecciona un horario");
    fillSelect(document.querySelector("#consultorioId"), offices.filter(item => item.estado === "DISPONIBLE"), item => item.id, item => `${item.codigo} - ${item.nombre}`, "Selecciona un consultorio");
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

function activateNavigation() {
    const section = location.pathname.split("/")[1];
    document.querySelectorAll(".navbar .nav-link").forEach(link => {
        link.classList.toggle("active", section ? link.getAttribute("href") === `/${section}` : link.getAttribute("href") === "/");
    });
}

const pageInitializers = {
    "medicos-listar": listDoctors,
    "medicos-registrar": registerDoctor,
    "especialidades-listar": listSpecialties,
    "especialidades-registrar": registerSpecialty,
    "especialidades-estado": bindSpecialtyStates,
    "horarios-listar": listSchedules,
    "horarios-registrar": registerSchedule,
    "horarios-editar": editSchedule,
    "consultorios-listar": listOffices,
    "consultorios-registrar": registerOffice,
    "consultorios-asignar": assignOffice
};

document.addEventListener("DOMContentLoaded", async () => {
    activateNavigation();
    showStoredFlash();
    try {
        await pageInitializers[document.body.dataset.page]?.();
    } catch (error) {
        showMessage(error.message);
    }
});
