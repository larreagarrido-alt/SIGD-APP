package pe.net.ws.kpiweb.features.admin.dashboard.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminEmployeeKpiItem;
import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminKpiSummary;
import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminMonthlyPerformance;
import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminTeamSummary;
import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminWellnessAlert;

/**
 * Fuente de datos ficticios determinista desacoplada.
 * Los datos se calculan a partir de la semilla temporal del período seleccionado,
 * garantizando coherencia aritmética (asignados = entregados + pendientes + vencidos)
 * y facilitando el reemplazo futuro por una API en PHP.
 */
public class AdminDashboardMockDataSource {

    private static final String[] EMPLOYEE_NAMES = {
            "Luis Paredes", "Pedro Salas", "Ana Morales", "Carlos Mendoza",
            "Sofía Vargas", "Jorge Huamán", "Elena Rivas", "Mateo Flores"
    };

    private static final String[] EMPLOYEE_ROLES = {
            "Senior Backend Developer", "Frontend Engineer", "Product Designer", "DevOps Engineer",
            "QA Automation", "Mobile Android Developer", "Data Analyst", "Full Stack Engineer"
    };

    private long calculateSeed(Calendar start, Calendar end) {
        long s = (long) start.get(Calendar.YEAR) * 10000 + (start.get(Calendar.MONTH) + 1) * 100 + start.get(Calendar.DAY_OF_MONTH);
        long e = (long) end.get(Calendar.YEAR) * 10000 + (end.get(Calendar.MONTH) + 1) * 100 + end.get(Calendar.DAY_OF_MONTH);
        return s * 31 + e;
    }

    public AdminKpiSummary getKpiSummary(Calendar start, Calendar end) {
        long seed = calculateSeed(start, end);
        Random rand = new Random(seed);

        // Diferenciar si es un día, semana o mes por la diferencia de milisegundos
        long diffDays = Math.max(1, (end.getTimeInMillis() - start.getTimeInMillis()) / (1000 * 60 * 60 * 24) + 1);

        int basePerDay = 8 + rand.nextInt(5);
        int total = (int) Math.min(180, basePerDay * diffDays);

        int delivered = (int) (total * (0.65 + (rand.nextInt(15) / 100.0)));
        int remaining = total - delivered;
        int pending = (int) (remaining * (0.70 + (rand.nextInt(15) / 100.0)));
        int overdue = remaining - pending;

        if (overdue < 0) {
            overdue = 0;
            pending = total - delivered;
        }

        // Garantizar regla estricta: assigned = delivered + pending + overdue
        int assigned = delivered + pending + overdue;

        return new AdminKpiSummary(assigned, delivered, pending, overdue);
    }

    public AdminTeamSummary getTeamSummary(Calendar start, Calendar end) {
        long seed = calculateSeed(start, end);
        Random rand = new Random(seed + 101);

        int collaborators = 48;
        int generalAvg = 80 + rand.nextInt(15); // e.g. 87%
        int testsCompleted = 30 + rand.nextInt(18); // e.g. 36 a 48
        int kpiOnTrack = 75 + rand.nextInt(20); // e.g. 82%

        return new AdminTeamSummary(collaborators, generalAvg, testsCompleted, collaborators, kpiOnTrack);
    }

    public List<AdminMonthlyPerformance> getMonthlyPerformance() {
        List<AdminMonthlyPerformance> list = new ArrayList<>();
        list.add(new AdminMonthlyPerformance("Ene", 74));
        list.add(new AdminMonthlyPerformance("Feb", 82));
        list.add(new AdminMonthlyPerformance("Mar", 79));
        list.add(new AdminMonthlyPerformance("Abr", 88));
        list.add(new AdminMonthlyPerformance("May", 91));
        list.add(new AdminMonthlyPerformance("Jun", 87));
        return list;
    }

    public AdminWellnessAlert getWellnessAlert(Calendar start, Calendar end) {
        long seed = calculateSeed(start, end);
        Random rand = new Random(seed + 202);

        int alerts = 1 + rand.nextInt(3);
        String desc = "Luis Paredes y Pedro Salas obtuvieron «Necesita atención» en sus tests. Se recomienda seguimiento.";
        return new AdminWellnessAlert(alerts, alerts + " alertas de bienestar", desc, "Ver colaboradores");
    }

    public List<AdminEmployeeKpiItem> getEmployees(Calendar start, Calendar end) {
        long seed = calculateSeed(start, end);
        Random rand = new Random(seed + 303);

        List<AdminEmployeeKpiItem> list = new ArrayList<>();

        // Para domingos en vista de día único, simular estado sin registros si cae en día 1 del mes para comprobar estado vacío
        if (start.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && start.get(Calendar.DAY_OF_MONTH) == 1) {
            return list;
        }

        int count = Math.min(EMPLOYEE_NAMES.length, 5 + rand.nextInt(4));
        for (int i = 0; i < count; i++) {
            String name = EMPLOYEE_NAMES[i];
            String role = EMPLOYEE_ROLES[i];

            int assigned = 3 + rand.nextInt(6);
            int delivered = Math.min(assigned, 2 + rand.nextInt(assigned + 1));
            int pending = assigned - delivered;

            String statusLabel;
            AdminEmployeeKpiItem.StatusType statusType;

            if (pending == 0) {
                statusLabel = "Al día";
                statusType = AdminEmployeeKpiItem.StatusType.ON_TRACK;
            } else if (delivered >= pending) {
                statusLabel = "Pendiente";
                statusType = AdminEmployeeKpiItem.StatusType.PENDING;
            } else {
                statusLabel = "En riesgo";
                statusType = AdminEmployeeKpiItem.StatusType.AT_RISK;
            }

            list.add(new AdminEmployeeKpiItem(name, role, assigned, delivered, pending, statusLabel, statusType));
        }

        return list;
    }
}
