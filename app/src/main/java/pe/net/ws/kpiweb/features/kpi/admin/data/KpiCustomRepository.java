package pe.net.ws.kpiweb.features.kpi.admin.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.engine.KpiMeasurementType;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;
import pe.net.ws.kpiweb.core.session.SessionManager;

/**
 * Repositorio central de KPIs personalizados para Gerencia y Colaboradores.
 * Permite que Gerencia cree, edite, versionar y active indicadores
 * sin tocar el código fuente, preservando la inmutabilidad histórica.
 */
public class KpiCustomRepository {

    private static final String PREF_NAME = "kpi_custom_admin_v1";
    private static final String KEY_KPIS = "key_kpi_definitions_json";
    private static final String KEY_KPI_VERSIONS = "key_kpi_versions_json";
    private static final String KEY_RECORDS = "key_kpi_records_json";

    private static volatile KpiCustomRepository instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final SharedPreferences prefs;
    private final Context appContext;
    private final int workerId;

    private final Map<String, KpiDefinition> kpiStore = new LinkedHashMap<>();
    private final List<KpiDefinition> versionHistory = new ArrayList<>();
    private final List<KpiRecordEntry> recordedEntries = new ArrayList<>();

    private KpiCustomRepository(Context context) {
        this.appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(context);
        this.workerId = session.getWorkerId();
        this.prefs = context.getApplicationContext().getSharedPreferences(
                PREF_NAME + "_user_" + session.getUserId(), Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized KpiCustomRepository getInstance(Context context) {
        int userId = new SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiCustomRepository(context);
            instanceUserId = userId;
        }
        return instance;
    }

    public synchronized List<KpiDefinition> getAllKpis() {
        return new ArrayList<>(kpiStore.values());
    }

    public synchronized KpiDefinition getKpiById(String id) {
        if (id == null) return null;
        return kpiStore.get(id);
    }

    /** Imports a newer private server draft; a locally edited pending draft is filtered by the caller. */
    public synchronized void importServerDraft(KpiDefinition draft) {
        if (draft == null || draft.getId() == null || draft.getId().isEmpty()) return;
        KpiDefinition local = kpiStore.get(draft.getId());
        if (local != null && local.getUpdatedAt() >= draft.getUpdatedAt()) return;
        kpiStore.put(draft.getId(), draft);
        saveToPrefs();
    }

    public synchronized List<KpiDefinition> getActiveKpisForRole(String role) {
        List<KpiDefinition> list = new ArrayList<>();
        if (role == null) return list;
        for (KpiDefinition kpi : kpiStore.values()) {
            if (kpi.isActive() && (role.equalsIgnoreCase(kpi.getRole()) || "Todos".equalsIgnoreCase(kpi.getRole()))) {
                list.add(kpi);
            }
        }
        return list;
    }

    public synchronized List<String> getAllConfiguredRoles(Context context) {
        java.util.LinkedHashSet<String> roles = new java.util.LinkedHashSet<>();
        roles.add("Asesor de Ventas");
        roles.add("Diseñador Gráfico");
        roles.add("Diseñador Gráfico Junior");
        roles.add("Diseñador Gráfico Senior");
        roles.add("Programador");
        for (KpiDefinition k : kpiStore.values()) {
            if (k.getRole() != null && !k.getRole().trim().isEmpty() && !"Todos".equalsIgnoreCase(k.getRole())) {
                String r = k.getRole().trim();
                if (!r.toLowerCase(java.util.Locale.ROOT).contains("fot")) {
                    roles.add(r);
                }
            }
        }
        if (context != null) {
            for (pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.CustomRoleEntry c :
                    pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.getCustomRoles(context)) {
                if (c.roleName != null && !c.roleName.trim().isEmpty()
                        && !c.roleName.toLowerCase(java.util.Locale.ROOT).contains("fot")) {
                    roles.add(c.roleName.trim());
                }
            }
        }
        return new ArrayList<>(roles);
    }

    public synchronized List<String> getAllConfiguredAreas(Context context) {
        java.util.LinkedHashSet<String> areas = new java.util.LinkedHashSet<>();
        areas.add("Audiovisual");
        areas.add("Comercial");
        areas.add("Diseño Gráfico");
        areas.add("Marketing Digital");
        areas.add("Tecnología");
        areas.add("Gestión General");
        for (KpiDefinition k : kpiStore.values()) {
            if (k.getArea() != null && !k.getArea().trim().isEmpty()) {
                areas.add(k.getArea().trim());
            }
        }
        if (context != null) {
            for (pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.CustomRoleEntry c :
                    pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.getCustomRoles(context)) {
                if (c.areaName != null && !c.areaName.trim().isEmpty()) {
                    areas.add(c.areaName.trim());
                }
            }
        }
        return new ArrayList<>(areas);
    }

    public synchronized void saveOrUpdateKpi(KpiDefinition kpi) {
        if (kpi == null) return;
        if (kpi.getId() == null || kpi.getId().isEmpty()) {
            kpi.setId("KPI-" + System.currentTimeMillis());
            kpi.setVersion(1);
            kpi.setCreatedAt(System.currentTimeMillis());
            kpi.setUpdatedAt(System.currentTimeMillis());
            kpiStore.put(kpi.getId(), kpi);
        } else {
            KpiDefinition existing = kpiStore.get(kpi.getId());
            if (existing != null) {
                // Versionamiento automático: si cambia la meta o el peso, se archiva la versión anterior
                if (existing.getTargetValue() != kpi.getTargetValue() || existing.getWeight() != kpi.getWeight()) {
                    versionHistory.add(existing);
                    kpi.setVersion(existing.getVersion() + 1);
                } else {
                    kpi.setVersion(existing.getVersion());
                }
            }
            kpi.setUpdatedAt(System.currentTimeMillis());
            kpiStore.put(kpi.getId(), kpi);
        }
        saveToPrefs();
    }

    public synchronized void toggleKpiStatus(String id) {
        KpiDefinition kpi = kpiStore.get(id);
        if (kpi != null) {
            kpi.setActive(!kpi.isActive());
            kpi.setUpdatedAt(System.currentTimeMillis());
            saveToPrefs();
        }
    }

    public synchronized List<KpiDefinition> getVersionHistoryForKpi(String id) {
        List<KpiDefinition> list = new ArrayList<>();
        if (id == null) return list;
        for (KpiDefinition v : versionHistory) {
            if (id.equals(v.getId())) {
                list.add(v);
            }
        }
        Collections.reverse(list);
        return list;
    }

    // Registro de entradas capturadas por colaboradores
    public synchronized void saveRecordEntry(KpiRecordEntry entry) {
        if (entry == null) return;
        if (workerId <= 0) return;
        entry.setEmployeeId(String.valueOf(workerId));
        recordedEntries.add(entry);
        saveRecordsToPrefs();
    }

    /**
     * Registra o actualiza la evaluación diaria de cualquiera de los 5 cargos operativos
     * en la columna vertebral unificada (alimentando puntos, sincronización y exportación PDF/CSV).
     */
    public synchronized void saveRoleDailyEvaluation(String role, String date, String kpiId, String kpiName,
                                                     double actualValue, double targetValue,
                                                     double compliancePercentage, boolean isFieldWork,
                                                     String observation) {
        // These codes and targets come from the original visual prototypes. Real
        // accounts submit complete form snapshots; an official numeric evaluation
        // requires a server assignment and must not be fabricated here.
        if (new pe.net.ws.kpiweb.core.session.SessionManager(appContext).isLoggedIn()) return;
        if (workerId <= 0 || role == null || date == null) return;
        String cleanDate = date.replace(" ", "");
        // Keep previous captures, including PENDING ones, until the server confirms them.
        double safeCompliance = Math.max(0.0, Math.min(100.0, compliancePercentage));
        KpiRecordEntry entry = new KpiRecordEntry(
                "REC-" + java.util.UUID.randomUUID(),
                kpiId != null ? kpiId : "KPI-ROL",
                1,
                kpiName != null ? kpiName : ("Evaluación Diaria - " + role),
                role,
                String.valueOf(workerId),
                cleanDate,
                actualValue,
                targetValue,
                100.0,
                safeCompliance,
                safeCompliance,
                false
        );
        entry.setFieldWork(isFieldWork);
        entry.setFieldWorkReason(isFieldWork ? (observation != null && !observation.isEmpty() ? observation : "Jornada autorizada en campo") : "");
        entry.setSupervisorObservation(observation != null ? observation : "");
        entry.setSyncStatus("PENDING");
        recordedEntries.add(0, entry);
        saveRecordsToPrefs();
        // The official numeric capture remains pending until the server confirms its assignment.
        pe.net.ws.kpiweb.features.kpi.sync.KpiSyncManager.getInstance(appContext).syncPendingRecords(null);
    }

    /**
     * Marca una lista de registros como sincronizados (SYNCED) y persiste el cambio en disco.
     */
    public synchronized void markEntriesAsSynced(List<KpiRecordEntry> syncedList) {
        if (syncedList == null || syncedList.isEmpty()) return;
        for (KpiRecordEntry target : syncedList) {
            target.setSyncStatus("SYNCED");
            for (KpiRecordEntry stored : recordedEntries) {
                if (stored.getRecordId() != null && stored.getRecordId().equals(target.getRecordId())) {
                    stored.setSyncStatus("SYNCED");
                }
            }
        }
        saveRecordsToPrefs();
    }

    /** Marks only records whose local payload has not changed since the server request. */
    public synchronized int markConfirmedEntries(Map<String, String> sentPayloadById) {
        if (sentPayloadById == null || sentPayloadById.isEmpty()) return 0;
        int count = 0;
        for (KpiRecordEntry stored : recordedEntries) {
            String sent = sentPayloadById.get(stored.getRecordId());
            if (sent != null && "PENDING".equalsIgnoreCase(stored.getSyncStatus())
                    && sent.equals(stored.toJson().toString())) {
                stored.setSyncStatus("SYNCED");
                count++;
            }
        }
        if (count > 0) saveRecordsToPrefs();
        return count;
    }

    public synchronized List<KpiRecordEntry> getRecordEntriesByDateAndRole(String date, String role) {
        List<KpiRecordEntry> list = new ArrayList<>();
        for (KpiRecordEntry e : recordedEntries) {
            boolean dateMatch = (date == null || date.equalsIgnoreCase(e.getDate()));
            boolean roleMatch = (role == null || role.equalsIgnoreCase(e.getRole()));
            if (dateMatch && roleMatch) {
                list.add(e);
            }
        }
        return list;
    }

    public synchronized boolean hasSubmittedToday(String date, String role) {
        if (date == null || role == null) return false;
        for (KpiRecordEntry e : recordedEntries) {
            if (date.equalsIgnoreCase(e.getDate()) && role.equalsIgnoreCase(e.getRole())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void deleteRecordsForDateAndRole(String date, String role) {
        if (date == null || role == null) return;
        List<KpiRecordEntry> toRemove = new ArrayList<>();
        for (KpiRecordEntry e : recordedEntries) {
            if (date.equalsIgnoreCase(e.getDate()) && role.equalsIgnoreCase(e.getRole())) {
                toRemove.add(e);
            }
        }
        if (!toRemove.isEmpty()) {
            recordedEntries.removeAll(toRemove);
            saveRecordsToPrefs();
        }
    }

    public synchronized List<KpiRecordEntry> getRecordEntriesByPeriod(String period) {
        List<KpiRecordEntry> list = new ArrayList<>();
        if (period == null || period.isEmpty()) return new ArrayList<>(recordedEntries);
        for (KpiRecordEntry e : recordedEntries) {
            if (period.equalsIgnoreCase(e.getPeriod())) {
                list.add(e);
            }
        }
        return list;
    }

    public synchronized List<KpiRecordEntry> getAllRecordEntries() {
        return new ArrayList<>(recordedEntries);
    }

    private synchronized void saveToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (KpiDefinition k : kpiStore.values()) {
                array.put(k.toJson());
            }
            JSONArray vArray = new JSONArray();
            for (KpiDefinition v : versionHistory) {
                vArray.put(v.toJson());
            }
            prefs.edit()
                    .putString(KEY_KPIS, array.toString())
                    .putString(KEY_KPI_VERSIONS, vArray.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveRecordsToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (KpiRecordEntry r : recordedEntries) {
                array.put(r.toJson());
            }
            prefs.edit().putString(KEY_RECORDS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        try {
            String jsonStr = prefs.getString(KEY_KPIS, null);
            if (jsonStr != null) {
                JSONArray array = new JSONArray(jsonStr);
                kpiStore.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        KpiDefinition k = KpiDefinition.fromJson(obj);
                        if (k != null && k.getId() != null) {
                            kpiStore.put(k.getId(), k);
                        }
                    }
                }
            }

            String vStr = prefs.getString(KEY_KPI_VERSIONS, null);
            if (vStr != null) {
                JSONArray array = new JSONArray(vStr);
                versionHistory.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        KpiDefinition k = KpiDefinition.fromJson(obj);
                        if (k != null) versionHistory.add(k);
                    }
                }
            }

            String rStr = prefs.getString(KEY_RECORDS, null);
            if (rStr != null) {
                JSONArray array = new JSONArray(rStr);
                recordedEntries.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        KpiRecordEntry r = KpiRecordEntry.fromJson(obj);
                        if (r != null) recordedEntries.add(r);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Semilla inicial con los indicadores corporativos oficiales para que Gerencia
     * disponga de datos reales navegables y configurables de inmediato.
     */
    private void seedDefaultCorporateKpis() {
        // Comercial / Asesor de Ventas
        addSeed("KPI-COM-01", "Prospecciones y llamadas diarias", "Contactar a potenciales clientes empresariales", "Comercial", "Asesor de Ventas", KpiMeasurementType.CANTIDAD, "llamadas", 15.0, 25.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-COM-02", "Reuniones y visitas en campo", "Presentación de servicios de desarrollo y diseño web", "Comercial", "Asesor de Ventas", KpiMeasurementType.CANTIDAD, "visitas", 3.0, 35.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-COM-03", "Propuestas comerciales enviadas", "Envío de cotizaciones formales a prospectos calificados", "Comercial", "Asesor de Ventas", KpiMeasurementType.CANTIDAD, "propuestas", 2.0, 20.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-COM-04", "Cierres y contratos firmados", "Efectivización de ventas con anticipo cancelado", "Comercial", "Asesor de Ventas", KpiMeasurementType.CANTIDAD, "cierres", 1.0, 20.0, KpiPolarity.MAYOR_ES_MEJOR);

        // Creativa / Diseñador Gráfico Pleno
        addSeed("KPI-DIS-01", "Diseños gráficos finalizados", "Creación de piezas digitales para redes sociales y web", "Diseño Gráfico", "Diseñador Gráfico", KpiMeasurementType.CANTIDAD, "diseños", 6.0, 50.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-DIS-02", "Edición y montaje de videos", "Producción de reels y piezas audiovisuales dinámicas", "Diseño Gráfico", "Diseñador Gráfico", KpiMeasurementType.CANTIDAD, "videos", 2.0, 35.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-DIS-03", "Sesiones fotográficas de producto", "Registro visual y postproducción fotográfica en estudio", "Diseño Gráfico", "Diseñador Gráfico", KpiMeasurementType.CANTIDAD, "sesiones", 1.0, 15.0, KpiPolarity.MAYOR_ES_MEJOR);

        // Creativa / Diseñador Junior
        addSeed("KPI-JUN-01", "Posts para redes sociales", "Diseño de artes para parrilla semanal de clientes", "Diseño Gráfico", "Diseñador Gráfico Junior", KpiMeasurementType.CANTIDAD, "posts", 5.0, 50.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-JUN-02", "Adaptaciones de formato", "Redimensionado y ajuste de piezas para stories y banners", "Diseño Gráfico", "Diseñador Gráfico Junior", KpiMeasurementType.CANTIDAD, "piezas", 3.0, 30.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-JUN-03", "Cumplimiento del briefing", "Alineamiento a la identidad corporativa y directrices de marca", "Diseño Gráfico", "Diseñador Gráfico Junior", KpiMeasurementType.PORCENTAJE, "%", 100.0, 20.0, KpiPolarity.MAYOR_ES_MEJOR);

        // Creativa / Diseñador Senior
        addSeed("KPI-SEN-01", "Producción personal de diseño", "Piezas complejas de identidad corporativa y branding", "Diseño Gráfico", "Diseñador Gráfico Senior", KpiMeasurementType.CANTIDAD, "entregables", 4.0, 40.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-SEN-02", "Supervisión y feedback de equipo", "Revisión técnica de piezas creadas por el equipo", "Diseño Gráfico", "Diseñador Gráfico Senior", KpiMeasurementType.CANTIDAD, "colaboradores", 2.0, 30.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-SEN-03", "Avance de entregas del equipo", "Tasa de entregables aprobados sin reprocesos", "Diseño Gráfico", "Diseñador Gráfico Senior", KpiMeasurementType.PORCENTAJE, "%", 80.0, 30.0, KpiPolarity.MAYOR_ES_MEJOR);

        // Tecnología / Programador
        addSeed("KPI-PROG-01", "Funcionalidades de software desarrolladas", "Construcción de módulos y pantallas funcionales en producción", "Tecnología", "Programador", KpiMeasurementType.CANTIDAD, "tareas", 2.0, 40.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-PROG-02", "Incidencias técnicas resueltas", "Corrección de errores y soporte a incidencias reportadas", "Tecnología", "Programador", KpiMeasurementType.CANTIDAD, "incidencias", 3.0, 35.0, KpiPolarity.MAYOR_ES_MEJOR);
        addSeed("KPI-PROG-03", "Bugs generados en despliegue", "Tasa de defectos detectados tras liberación a pruebas", "Tecnología", "Programador", KpiMeasurementType.CANTIDAD, "bugs", 0.0, 25.0, KpiPolarity.MENOR_ES_MEJOR);
    }

    private void purgeRemovedPhotographyKpis() {
        String[] removedIds = {
                "KPI-FOTO-01", "KPI-FOTO-02", "KPI-FOTO-03", "KPI-FOTO-04",
                "KPI-PFOT-01", "KPI-PFOT-02", "KPI-PFOT-03", "KPI-PFOT-04"
        };
        for (String id : removedIds) {
            kpiStore.remove(id);
        }
    }

    private void addSeed(String id, String name, String desc, String area, String role,
                         KpiMeasurementType type, String unit, double target, double weight,
                         KpiPolarity polarity) {
        KpiDefinition k = new KpiDefinition(id, name, area, role, type, unit, target, weight, polarity);
        k.setDescription(desc);
        k.setObjective("Elevar el estándar de calidad y cumplimiento normativo SIGD 2026.");
        k.setResponsible("Colaborador evaluado");
        k.setFrequency("Diaria");
        k.setBaseline(0.0);
        k.setActive(true);
        kpiStore.put(k.getId(), k);
    }

    private void seedDefaultHistoricalRecords() {
        addRecordSeed("REC-SEED-01", "KPI-PROG-01", "Cumplimiento de Desarrollo de Software", "Programador", "20/10/2026", 4.0, 4.0, 100.0, false, "");
        addRecordSeed("REC-SEED-02", "KPI-COM-01", "Cumplimiento Comercial Diario", "Asesor de Ventas", "21/10/2026", 15.0, 15.0, 96.0, true, "Visitas corporativas en sede de cliente");
        addRecordSeed("REC-SEED-03", "KPI-DIS-01", "Producción Diaria (Modalidad A/B)", "Diseñador Gráfico", "22/10/2026", 6.0, 6.0, 100.0, false, "");
        addRecordSeed("REC-SEED-04", "KPI-JUN-01", "Producción Mixta Redes Sociales", "Diseñador Gráfico Junior", "23/10/2026", 5.0, 6.0, 88.0, false, "");
        addRecordSeed("REC-SEED-05", "KPI-SEN-01", "Producción Personal + Supervisión", "Diseñador Gráfico Senior", "24/10/2026", 5.0, 5.0, 92.0, false, "");
    }

    private void addRecordSeed(String recId, String kpiId, String kpiName, String role, String date,
                               double actual, double target, double compliance, boolean fieldWork, String reason) {
        KpiRecordEntry e = new KpiRecordEntry(
                recId, kpiId, 1, kpiName, role, "COLAB-ACTUAL", date,
                actual, target, 100.0, compliance, compliance, true
        );
        e.setFieldWork(fieldWork);
        e.setFieldWorkReason(reason);
        e.setSyncStatus("PENDING");
        recordedEntries.add(e);
    }
}
