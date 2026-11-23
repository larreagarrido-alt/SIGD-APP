package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiProjectSuggestionsStore;
import pe.net.ws.kpiweb.features.kpi.model.KpiActivityItem;

/**
 * Adaptador dinámico para las tarjetas de actividades de Programador Junior:
 * - Proyecto editable manualmente por el usuario.
 * - Horas iniciadas en "--:--" antes de colocarlas con TimePickerDialog.
 * - Al marcar "Salida a campo", se ocultan Proyecto/Tipo/Bugs/Commit Git dejando solo
 *   Hora inicio, Hora fin y Justificación, acreditando el KPI al 100%.
 */
public class KpiActivityAdapter extends RecyclerView.Adapter<KpiActivityAdapter.ActivityViewHolder> {

    public interface OnActivityDataChangedListener {
        void onDataChanged();
        void onItemDeleted(int position);
    }

    private final Context context;
    private final List<KpiActivityItem> items;
    private final OnActivityDataChangedListener listener;
    private boolean afternoonShift = false;
    private boolean readOnly = false;

    private static final String[] TIPOS = {
            "Desarrollo", "Corrección", "Maquetación",
            "Integración / API", "Mantenimiento Web"
    };

    public KpiActivityAdapter(Context context, List<KpiActivityItem> items, OnActivityDataChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void setAfternoonShift(boolean afternoonShift) {
        this.afternoonShift = afternoonShift;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.kpi_item_activity_card, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        KpiActivityItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {
        View vAccentLine;
        TextView tvCategoryIcon;
        TextView tvItemNumber;
        TextView tvStatusBadge;
        TextView tvCardNumberBox;
        ImageView btnDeleteActivity;
        LinearLayout llProjectAndTypeRow;
        AutoCompleteTextView etProject;
        Spinner spType;
        TextView tvActivityLabel;
        EditText etActivityDescription;
        LinearLayout llStartTime;
        TextView tvStartTime;
        LinearLayout llEndTime;
        TextView tvEndTime;
        RadioGroup rgStatus;
        RadioButton rbEntregado;
        RadioButton rbPendiente;
        LinearLayout llBugsAndCommitRow;
        EditText etBugs;
        EditText tvCommitHash;

        TextWatcher projectWatcher;
        TextWatcher descWatcher;
        TextWatcher bugsWatcher;
        TextWatcher commitWatcher;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            vAccentLine = itemView.findViewById(R.id.vAccentLine);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvItemNumber = itemView.findViewById(R.id.tvItemNumber);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvCardNumberBox = itemView.findViewById(R.id.tvCardNumberBox);
            btnDeleteActivity = itemView.findViewById(R.id.btnDeleteActivity);
            llProjectAndTypeRow = itemView.findViewById(R.id.llProjectAndTypeRow);
            etProject = itemView.findViewById(R.id.etProject);
            spType = itemView.findViewById(R.id.spType);
            tvActivityLabel = itemView.findViewById(R.id.tvActivityLabel);
            etActivityDescription = itemView.findViewById(R.id.etActivityDescription);
            llStartTime = itemView.findViewById(R.id.llStartTime);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            llEndTime = itemView.findViewById(R.id.llEndTime);
            tvEndTime = itemView.findViewById(R.id.tvEndTime);
            rgStatus = itemView.findViewById(R.id.rgStatus);
            rbEntregado = itemView.findViewById(R.id.rbEntregado);
            rbPendiente = itemView.findViewById(R.id.rbPendiente);
            llBugsAndCommitRow = itemView.findViewById(R.id.llBugsAndCommitRow);
            etBugs = itemView.findViewById(R.id.etBugs);
            tvCommitHash = itemView.findViewById(R.id.tvCommitHash);
        }

        void bind(KpiActivityItem item, int position) {
            String formattedNumber = String.format(Locale.getDefault(), "%02d", position + 1);
            tvItemNumber.setText(formattedNumber);
            tvCardNumberBox.setText(formattedNumber);

            // Limpiar TextWatchers previos antes de asignar valores reciclados
            if (projectWatcher != null) etProject.removeTextChangedListener(projectWatcher);
            if (descWatcher != null) etActivityDescription.removeTextChangedListener(descWatcher);
            if (bugsWatcher != null) etBugs.removeTextChangedListener(bugsWatcher);
            if (commitWatcher != null) tvCommitHash.removeTextChangedListener(commitWatcher);

            // Proyecto escrito manualmente con sugerencias automáticas de días anteriores
            List<String> projectSuggestions = KpiProjectSuggestionsStore.getSuggestions(context);
            ArrayAdapter<String> suggestAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_dropdown_item_1line, projectSuggestions);
            etProject.setAdapter(suggestAdapter);
            etProject.setText(item.getProyecto() != null ? item.getProyecto() : "");
            projectWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setProyecto(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {
                    if (listener != null) listener.onDataChanged();
                }
            };
            etProject.addTextChangedListener(projectWatcher);
            etProject.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && item.getProyecto() != null && item.getProyecto().trim().length() >= 2) {
                    KpiProjectSuggestionsStore.saveProject(context, item.getProyecto());
                }
            });

            // Spinner Tipo
            ArrayAdapter<String> adapterTipos = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, TIPOS);
            spType.setOnItemSelectedListener(null);
            spType.setAdapter(adapterTipos);
            for (int i = 0; i < TIPOS.length; i++) {
                if (TIPOS[i].equalsIgnoreCase(item.getTipo())) {
                    spType.setSelection(i);
                    break;
                }
            }
            spType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    if (item.isSalidaCampo()) return;
                    String selectedTipo = TIPOS[pos];
                    if (!selectedTipo.equalsIgnoreCase(item.getTipo())) {
                        item.setTipo(selectedTipo);
                        aplicarEstiloCategoria(selectedTipo);
                        if (listener != null) listener.onDataChanged();
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            // Descripción o Justificación
            etActivityDescription.setText(item.getDescripcion() != null ? item.getDescripcion() : "");
            descWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setDescripcion(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {
                    if (listener != null) listener.onDataChanged();
                }
            };
            etActivityDescription.addTextChangedListener(descWatcher);

            // Horarios: "--:--" antes de colocarlos
            String startStr = (item.getHoraInicio() == null || item.getHoraInicio().trim().isEmpty())
                    ? "--:--" : item.getHoraInicio();
            String endStr = (item.getHoraFin() == null || item.getHoraFin().trim().isEmpty())
                    ? "--:--" : item.getHoraFin();
            tvStartTime.setText(startStr);
            tvEndTime.setText(endStr);

            View.OnClickListener startTimeClick = v -> showTimePicker(item, true, tvStartTime);
            if (llStartTime != null) llStartTime.setOnClickListener(startTimeClick);
            tvStartTime.setOnClickListener(startTimeClick);

            View.OnClickListener endTimeClick = v -> showTimePicker(item, false, tvEndTime);
            if (llEndTime != null) llEndTime.setOnClickListener(endTimeClick);
            tvEndTime.setOnClickListener(endTimeClick);

            // Estado de la actividad (solo Entregado o Pendiente; Salida a campo es general arriba)
            rgStatus.setOnCheckedChangeListener(null);
            aplicarModoVisualSegunEstado(item);

            rgStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbEntregado) {
                    item.setEstado("Entregado");
                } else {
                    item.setEstado("Pendiente");
                }
                aplicarModoVisualSegunEstado(item);
                if (listener != null) listener.onDataChanged();
            });

            // Bugs
            etBugs.setText(String.valueOf(item.getBugs()));
            bugsWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        item.setBugs(s.length() == 0 ? 0 : Integer.parseInt(s.toString()));
                        if (listener != null) listener.onDataChanged();
                    } catch (NumberFormatException ignored) {}
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            etBugs.addTextChangedListener(bugsWatcher);

            // Commit Git
            tvCommitHash.setText(item.getCommitHash() != null ? item.getCommitHash() : "");
            commitWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setCommitHash(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {
                    if (listener != null) listener.onDataChanged();
                }
            };
            tvCommitHash.addTextChangedListener(commitWatcher);

            // Eliminar actividad
            btnDeleteActivity.setVisibility(readOnly ? View.GONE : View.VISIBLE);
            btnDeleteActivity.setOnClickListener(v -> {
                if (readOnly) return;
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemDeleted(pos);
                }
            });

            etProject.setEnabled(!readOnly);
            spType.setEnabled(!readOnly);
            etActivityDescription.setEnabled(!readOnly);
            if (llStartTime != null) llStartTime.setEnabled(!readOnly);
            tvStartTime.setEnabled(!readOnly);
            if (llEndTime != null) llEndTime.setEnabled(!readOnly);
            tvEndTime.setEnabled(!readOnly);
            rbEntregado.setEnabled(!readOnly);
            rbPendiente.setEnabled(!readOnly);
            etBugs.setEnabled(!readOnly);
            tvCommitHash.setEnabled(!readOnly);
        }

        private void aplicarModoVisualSegunEstado(KpiActivityItem item) {
            if (llProjectAndTypeRow != null) llProjectAndTypeRow.setVisibility(View.VISIBLE);
            if (llBugsAndCommitRow != null) llBugsAndCommitRow.setVisibility(View.VISIBLE);
            if (tvActivityLabel != null) tvActivityLabel.setText("Actividad / Tarea");
            etActivityDescription.setHint("Describa la actividad o tarea realizada...");

            if (item.isEntregado()) {
                rbEntregado.setChecked(true);
                tvStatusBadge.setText("● Entregado");
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_entregado));
            } else {
                rbPendiente.setChecked(true);
                tvStatusBadge.setText("● Pendiente");
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_pendiente));
            }
            tvStatusBadge.setBackgroundResource(R.drawable.kpi_bg_pill_status);
            aplicarEstiloCategoria(item.getTipo());
        }

        private void showTimePicker(KpiActivityItem item, boolean isStartTime, TextView targetView) {
            String currentText = isStartTime ? item.getHoraInicio() : item.getHoraFin();
            // Predeterminado interno según turno: Mañana (8:00 a 1:30) | Tarde (3:30 a 6:30)
            int defaultHour = afternoonShift ? (isStartTime ? 15 : 18) : (isStartTime ? 8 : 13);
            int defaultMinute = afternoonShift ? 30 : (isStartTime ? 0 : 30);

            if (currentText != null && currentText.contains(":") && !currentText.contains("--")) {
                try {
                    String[] parts = currentText.trim().split("[: ]");
                    if (parts.length >= 2) {
                        int h = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        boolean isPm = currentText.toLowerCase().contains("pm");
                        if (isPm && h < 12) h += 12;
                        if (!isPm && h == 12) h = 0;
                        defaultHour = h;
                        defaultMinute = m;
                    }
                } catch (Exception ignored) {}
            }

            TimePickerDialog dialog = new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                String amPm = hourOfDay >= 12 ? "pm" : "am";
                int hour12 = hourOfDay % 12;
                if (hour12 == 0) hour12 = 12;
                String formatted = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);

                targetView.setText(formatted);
                if (isStartTime) {
                    item.setHoraInicio(formatted);
                } else {
                    item.setHoraFin(formatted);
                }
                if (listener != null) {
                    listener.onDataChanged();
                }
            }, defaultHour, defaultMinute, false);

            dialog.show();
        }

        private void aplicarEstiloCategoria(String tipo) {
            int accentColor;
            String iconText;

            if ("Salida a campo".equalsIgnoreCase(tipo) || "Comisión".equalsIgnoreCase(tipo)) {
                accentColor = ContextCompat.getColor(context, R.color.kpi_status_justificado);
                iconText = "TER";
            } else if ("Desarrollo".equalsIgnoreCase(tipo)) {
                accentColor = ContextCompat.getColor(context, R.color.kpi_cat_desarrollo);
                iconText = "< >";
            } else if ("Corrección".equalsIgnoreCase(tipo)) {
                accentColor = ContextCompat.getColor(context, R.color.kpi_cat_correccion);
                iconText = "FIX";
            } else if ("Maquetación".equalsIgnoreCase(tipo)) {
                accentColor = ContextCompat.getColor(context, R.color.kpi_cat_maquetacion);
                iconText = "UI";
            } else if ("Integración / API".equalsIgnoreCase(tipo)) {
                accentColor = ContextCompat.getColor(context, R.color.kpi_cat_api);
                iconText = "API";
            } else {
                accentColor = ContextCompat.getColor(context, R.color.kpi_cat_mantenimiento);
                iconText = "DEV";
            }

            vAccentLine.setBackgroundColor(accentColor);
            tvCategoryIcon.setText(iconText);
            tvCategoryIcon.setTextColor(accentColor);
        }
    }
}
