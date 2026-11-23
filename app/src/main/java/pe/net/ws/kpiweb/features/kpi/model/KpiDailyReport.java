package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo completo del Registro Diario de Actividades (SIGD - KPI).
 * Engloba las 8 secciones del sistema.
 */
public class KpiDailyReport implements Serializable {
    private String id;
    
    // 01 DATOS GENERALES
    private String fecha;
    private String practicante;
    private String supervisor;
    private String turno;
    private String rol;

    // 02 META DIARIA DE PRODUCCIÓN
    private List<KpiProductionGoal> metas;

    // 03 REGISTRO DE ACTIVIDADES
    private List<KpiActivityItem> actividades;

    // 04 RESUMEN DE PRODUCCIÓN DEL DÍA
    private int tareasAsignadas;
    private int tareasCompletadas;
    private int tareasPendientes;
    private int tareasJustificadasCampo;
    private int funcionalidadesDesarrolladas;
    private int incidenciasCorregidas;
    private int bugsDetectados;
    private int bugsGenerados;
    private int tareasAprobadas;
    private int tareasRequierenCorreccion;
    private int cumplimientoMeta; // porcentaje e.g. 100%

    // Contadores en vivo por categoría
    private int countDesarrollo;
    private int countCorreccion;
    private int countMejora;
    private int countMaquetacion;
    private int countApi;
    private int countMantenimiento;

    // 05 EVALUACIÓN RÁPIDA DEL DÍA (Criterio -> Cumple)
    private Map<String, Boolean> criteriosEvaluacion;

    // 06 INCIDENCIAS DEL DÍA
    private boolean huboDificultad;
    private String detalleDificultad;

    // MODALIDAD: SALIDA A CAMPO / COMISIÓN EXTERNA (Protección de KPI ante Admin)
    private boolean esSalidaCampo;
    private String motivoSalidaCampo;
    private String horaInicioCampo = "--:--";
    private String horaFinCampo = "--:--";

    // 07 OBSERVACIÓN DEL SUPERVISOR Y FIRMAS
    private String observacionSupervisor;
    private String firmaPracticante;
    private String firmaSupervisor;

    // 08 NIVEL DE PRODUCTIVIDAD DEL DÍA
    private String nivelProductividad; // EXCELENTE, MUY BUENO, BUENO, REGULAR, DEFICIENTE
    private int puntaje; // e.g. 18 a 20

    public KpiDailyReport() {
        this.metas = new ArrayList<>();
        this.actividades = new ArrayList<>();
        this.criteriosEvaluacion = new LinkedHashMap<>();
    }

    /**
     * Recalcula automáticamente los totales del Resumen (Sección 04)
     * basándose en las actividades registradas.
     * Si el practicante está en Salida a Campo o tiene actividades justificadas en terreno,
     * su KPI queda protegido al 100% ante el Administrador sin sufrir penalización.
     */
    public void recalcularResumen() {
        this.tareasAsignadas = actividades.size();
        this.tareasCompletadas = 0;
        this.tareasPendientes = 0;
        this.tareasJustificadasCampo = 0;
        this.funcionalidadesDesarrolladas = 0;
        this.incidenciasCorregidas = 0;
        this.bugsDetectados = 0;
        this.countDesarrollo = 0;
        this.countCorreccion = 0;
        this.countMejora = 0;
        this.countMaquetacion = 0;
        this.countApi = 0;
        this.countMantenimiento = 0;

        for (KpiActivityItem item : actividades) {
            if (item.isSalidaCampo()) {
                this.tareasJustificadasCampo++;
            } else if (item.isEntregado()) {
                this.tareasCompletadas++;

                String tipo = item.getTipo() != null ? item.getTipo().trim() : "";
                if ("Desarrollo".equalsIgnoreCase(tipo)) {
                    this.funcionalidadesDesarrolladas++;
                    this.countDesarrollo++;
                } else if ("Corrección".equalsIgnoreCase(tipo)) {
                    this.incidenciasCorregidas++;
                    this.countCorreccion++;
                } else if ("Mejora".equalsIgnoreCase(tipo) || tipo.toLowerCase().contains("mejora")) {
                    this.countMejora++;
                } else if ("Maquetación".equalsIgnoreCase(tipo) || tipo.toLowerCase().contains("maquetaci")) {
                    this.countMaquetacion++;
                } else if ("Integración / API".equalsIgnoreCase(tipo) || tipo.toLowerCase().contains("api")) {
                    this.countApi++;
                } else if ("Mantenimiento Web".equalsIgnoreCase(tipo) || tipo.toLowerCase().contains("mantenim")) {
                    this.countMantenimiento++;
                }
            } else {
                this.tareasPendientes++;
            }

            this.bugsDetectados += item.getBugs();
        }

        this.tareasAprobadas = this.tareasCompletadas + this.tareasJustificadasCampo;
        this.tareasRequierenCorreccion = 0;

        // Si se registró salida a campo general, se justifica la jornada completa sin penalizar el KPI
        if (this.esSalidaCampo) {
            this.cumplimientoMeta = 100;
            this.nivelProductividad = "EXCELENTE";
            this.puntaje = 20;
            return;
        }

        if (this.tareasAsignadas > 0) {
            int tareasEfectivas = this.tareasCompletadas + this.tareasJustificadasCampo;
            this.cumplimientoMeta = (int) Math.round(((double) tareasEfectivas / this.tareasAsignadas) * 100);
        } else {
            this.cumplimientoMeta = 0;
            this.nivelProductividad = "PENDIENTE";
            this.puntaje = 0;
            return;
        }

        if (cumplimientoMeta >= 90) {
            this.nivelProductividad = "EXCELENTE";
            this.puntaje = 20;
        } else if (cumplimientoMeta >= 80) {
            this.nivelProductividad = "MUY BUENO";
            this.puntaje = 17;
        } else if (cumplimientoMeta >= 60) {
            this.nivelProductividad = "BUENO";
            this.puntaje = 14;
        } else if (cumplimientoMeta >= 40) {
            this.nivelProductividad = "REGULAR";
            this.puntaje = 12;
        } else if (cumplimientoMeta > 0) {
            this.nivelProductividad = "DEFICIENTE";
            this.puntaje = 10;
        } else {
            this.nivelProductividad = "PENDIENTE";
            this.puntaje = 0;
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getPracticante() { return practicante; }
    public void setPracticante(String practicante) { this.practicante = practicante; }

    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public List<KpiProductionGoal> getMetas() { return metas; }
    public void setMetas(List<KpiProductionGoal> metas) { this.metas = metas; }

    public List<KpiActivityItem> getActividades() { return actividades; }
    public void setActividades(List<KpiActivityItem> actividades) {
        this.actividades = actividades;
        recalcularResumen();
    }

    public int getTareasAsignadas() { return tareasAsignadas; }
    public void setTareasAsignadas(int tareasAsignadas) { this.tareasAsignadas = tareasAsignadas; }

    public int getTareasCompletadas() { return tareasCompletadas; }
    public void setTareasCompletadas(int tareasCompletadas) { this.tareasCompletadas = tareasCompletadas; }

    public int getTareasPendientes() { return tareasPendientes; }
    public void setTareasPendientes(int tareasPendientes) { this.tareasPendientes = tareasPendientes; }

    public int getTareasJustificadasCampo() { return tareasJustificadasCampo; }
    public void setTareasJustificadasCampo(int v) { this.tareasJustificadasCampo = v; }

    public int getFuncionalidadesDesarrolladas() { return funcionalidadesDesarrolladas; }
    public void setFuncionalidadesDesarrolladas(int v) { this.funcionalidadesDesarrolladas = v; }

    public int getIncidenciasCorregidas() { return incidenciasCorregidas; }
    public void setIncidenciasCorregidas(int v) { this.incidenciasCorregidas = v; }

    public int getBugsDetectados() { return bugsDetectados; }
    public void setBugsDetectados(int bugsDetectados) { this.bugsDetectados = bugsDetectados; }

    public int getBugsGenerados() { return bugsGenerados; }
    public void setBugsGenerados(int bugsGenerados) { this.bugsGenerados = bugsGenerados; }

    public int getTareasAprobadas() { return tareasAprobadas; }
    public void setTareasAprobadas(int tareasAprobadas) { this.tareasAprobadas = tareasAprobadas; }

    public int getTareasRequierenCorreccion() { return tareasRequierenCorreccion; }
    public void setTareasRequierenCorreccion(int v) { this.tareasRequierenCorreccion = v; }

    public int getCumplimientoMeta() { return cumplimientoMeta; }
    public void setCumplimientoMeta(int cumplimientoMeta) { this.cumplimientoMeta = cumplimientoMeta; }

    public Map<String, Boolean> getCriteriosEvaluacion() { return criteriosEvaluacion; }
    public void setCriteriosEvaluacion(Map<String, Boolean> criteriosEvaluacion) { this.criteriosEvaluacion = criteriosEvaluacion; }

    public boolean isHuboDificultad() { return huboDificultad; }
    public void setHuboDificultad(boolean huboDificultad) { this.huboDificultad = huboDificultad; }

    public String getDetalleDificultad() { return detalleDificultad; }
    public void setDetalleDificultad(String detalleDificultad) { this.detalleDificultad = detalleDificultad; }

    public String getObservacionSupervisor() { return observacionSupervisor; }
    public void setObservacionSupervisor(String observacionSupervisor) { this.observacionSupervisor = observacionSupervisor; }

    public String getFirmaPracticante() { return firmaPracticante; }
    public void setFirmaPracticante(String firmaPracticante) { this.firmaPracticante = firmaPracticante; }

    public String getFirmaSupervisor() { return firmaSupervisor; }
    public void setFirmaSupervisor(String firmaSupervisor) { this.firmaSupervisor = firmaSupervisor; }

    public String getNivelProductividad() { return nivelProductividad; }
    public void setNivelProductividad(String nivelProductividad) { this.nivelProductividad = nivelProductividad; }

    public int getPuntaje() { return puntaje; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }

    public boolean isEsSalidaCampo() { return esSalidaCampo; }
    public void setEsSalidaCampo(boolean esSalidaCampo) { this.esSalidaCampo = esSalidaCampo; }

    public String getMotivoSalidaCampo() { return motivoSalidaCampo; }
    public void setMotivoSalidaCampo(String motivoSalidaCampo) { this.motivoSalidaCampo = motivoSalidaCampo; }

    public String getHoraInicioCampo() { return horaInicioCampo; }
    public void setHoraInicioCampo(String horaInicioCampo) { this.horaInicioCampo = horaInicioCampo; }

    public String getHoraFinCampo() { return horaFinCampo; }
    public void setHoraFinCampo(String horaFinCampo) { this.horaFinCampo = horaFinCampo; }

    public int getCountDesarrollo() { return countDesarrollo; }
    public int getCountCorreccion() { return countCorreccion; }
    public int getCountMejora() { return countMejora; }
    public int getCountMaquetacion() { return countMaquetacion; }
    public int getCountApi() { return countApi; }
    public int getCountMantenimiento() { return countMantenimiento; }
}
