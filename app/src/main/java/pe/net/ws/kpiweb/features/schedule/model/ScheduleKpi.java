package pe.net.ws.kpiweb.features.schedule.model;

public class ScheduleKpi {
    public String name;
    public String target;
    public boolean weekly;
    /** Texto del chip; si es null se usa Semanal/Mensual según weekly. */
    public String chipText;

    public ScheduleKpi() { }

    public ScheduleKpi(String name, String target, boolean weekly) {
        this(name, target, weekly, null);
    }

    public ScheduleKpi(String name, String target, boolean weekly, String chipText) {
        this.name = name;
        this.target = target;
        this.weekly = weekly;
        this.chipText = chipText;
    }
}
