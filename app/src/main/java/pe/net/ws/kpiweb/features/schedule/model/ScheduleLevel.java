package pe.net.ws.kpiweb.features.schedule.model;

public class ScheduleLevel {
    public String range;
    public String level;
    /** Color como "#RRGGBB" (los ids de recursos no son estables al serializar). */
    public String colorHex;
    public String description;

    public ScheduleLevel() { }

    public ScheduleLevel(String range, String level, String colorHex, String description) {
        this.range = range;
        this.level = level;
        this.colorHex = colorHex;
        this.description = description;
    }
}
