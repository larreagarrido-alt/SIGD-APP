package pe.net.ws.kpiweb.features.schedule.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/** Cargo con su horario semanal, metas/KPIs y (opcional) parámetros de evaluación. Serializable con Gson. */
public class ScheduleProfile {
    /** Escala aprobada para todos los cargos; coincide con los puntos del servidor. */
    public void useOfficialEvaluationLevels() {
        levels = new ArrayList<>(Arrays.asList(
                new ScheduleLevel("90% - 100%", "Excelente", "#16A34A", "Cumple o supera la meta del cargo."),
                new ScheduleLevel("80% - 89%", "Muy Bueno", "#0284C7", "Se acerca a la meta del cargo."),
                new ScheduleLevel("60% - 79%", "Bueno", "#D97706", "Avance parcial respecto a la meta."),
                new ScheduleLevel("40% - 59%", "Regular", "#EA580C", "Actividad por debajo de la meta."),
                new ScheduleLevel("Menos de 40%", "Deficiente", "#DC2626", "Necesita revisión del avance.")));
    }
    public String id;
    public String roleName;
    public String subtitle;
    public List<Slot> slots = new ArrayList<>();
    public String goalsTabLabel = "Metas";
    public List<ScheduleKpi> goals = new ArrayList<>();
    /** Escala de evaluación mostrada junto al cronograma. */
    public List<ScheduleLevel> levels;
    /** null o vacío = sin tarjeta de frase motivacional. */
    public String quote;
    public String[] pillars = new String[0];
    public String motto = "";
    /** true = cargo oficial de fábrica (no se elimina, solo se restaura). */
    public boolean factory;
    /** true = horario propuesto automáticamente a partir de los KPI del cargo (pendiente de validar). */
    public boolean generated;

    public ScheduleProfile() { }

    public ScheduleProfile(String id, String roleName, String subtitle, List<Slot> slots,
            String goalsTabLabel, List<ScheduleKpi> goals, List<ScheduleLevel> levels,
            String quote, String[] pillars, String motto) {
        this.id = id;
        this.roleName = roleName;
        this.subtitle = subtitle;
        this.slots = slots;
        this.goalsTabLabel = goalsTabLabel;
        this.goals = goals;
        this.levels = levels;
        this.quote = quote;
        this.pillars = pillars;
        this.motto = motto;
    }

    public boolean hasQuote() {
        return quote != null && !quote.trim().isEmpty();
    }
}
