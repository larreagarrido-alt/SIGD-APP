package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesActivityItem;

public class KpiSalesActivityAdapter extends RecyclerView.Adapter<KpiSalesActivityAdapter.SalesViewHolder> {

    public interface OnSalesDataChangedListener {
        void onDataChanged();
        void onItemDeleted(int position);
    }

    private final Context context;
    private final List<KpiSalesActivityItem> items;
    private final OnSalesDataChangedListener listener;

    private static final String[] TIPOS_GESTION = {
            "Prospección", "Seguimiento", "Reunión / Demo",
            "Cotización", "Cierre de venta", "Visita en terreno"
    };

    public KpiSalesActivityAdapter(Context context, List<KpiSalesActivityItem> items, OnSalesDataChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.kpi_item_sales_activity_card, parent, false);
        return new SalesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalesViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class SalesViewHolder extends RecyclerView.ViewHolder {
        View vAccentLineSales;
        TextView tvSalesIcon;
        TextView tvSalesNumber;
        TextView tvSalesStatusBadge;
        ImageView btnDeleteSalesActivity;
        EditText etSalesClient;
        Spinner spSalesType;
        EditText etSalesDetail;
        LinearLayout llSalesStartTime;
        TextView tvSalesStartTime;
        LinearLayout llSalesEndTime;
        TextView tvSalesEndTime;
        RadioGroup rgSalesStatus;
        RadioButton rbSalesConcretado;
        RadioButton rbSalesPendiente;
        RadioButton rbSalesCampo;
        LinearLayout llSalesCampoNotice;
        EditText etSalesMonto;

        SalesViewHolder(@NonNull View itemView) {
            super(itemView);
            vAccentLineSales = itemView.findViewById(R.id.vAccentLineSales);
            tvSalesIcon = itemView.findViewById(R.id.tvSalesIcon);
            tvSalesNumber = itemView.findViewById(R.id.tvSalesNumber);
            tvSalesStatusBadge = itemView.findViewById(R.id.tvSalesStatusBadge);
            btnDeleteSalesActivity = itemView.findViewById(R.id.btnDeleteSalesActivity);
            etSalesClient = itemView.findViewById(R.id.etSalesClient);
            spSalesType = itemView.findViewById(R.id.spSalesType);
            etSalesDetail = itemView.findViewById(R.id.etSalesDetail);
            llSalesStartTime = itemView.findViewById(R.id.llSalesStartTime);
            tvSalesStartTime = itemView.findViewById(R.id.tvSalesStartTime);
            llSalesEndTime = itemView.findViewById(R.id.llSalesEndTime);
            tvSalesEndTime = itemView.findViewById(R.id.tvSalesEndTime);
            rgSalesStatus = itemView.findViewById(R.id.rgSalesStatus);
            rbSalesConcretado = itemView.findViewById(R.id.rbSalesConcretado);
            rbSalesPendiente = itemView.findViewById(R.id.rbSalesPendiente);
            rbSalesCampo = itemView.findViewById(R.id.rbSalesCampo);
            llSalesCampoNotice = itemView.findViewById(R.id.llSalesCampoNotice);
            etSalesMonto = itemView.findViewById(R.id.etSalesMonto);
        }

        void bind(KpiSalesActivityItem item, int position) {
            String formattedNumber = String.format(Locale.getDefault(), "%02d", position + 1);
            tvSalesNumber.setText(formattedNumber);

            // Estilo por categoría
            aplicarEstilo(item.getTipoGestion());

            // Cliente
            etSalesClient.setText(item.getCliente() != null ? item.getCliente() : "");
            etSalesClient.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setCliente(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Spinner Tipo
            spSalesType.setOnItemSelectedListener(null);
            ArrayAdapter<String> adapterTipos = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, TIPOS_GESTION);
            spSalesType.setAdapter(adapterTipos);
            for (int i = 0; i < TIPOS_GESTION.length; i++) {
                if (TIPOS_GESTION[i].equalsIgnoreCase(item.getTipoGestion())) {
                    spSalesType.setSelection(i);
                    break;
                }
            }
            spSalesType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                    String sel = TIPOS_GESTION[pos];
                    if (!sel.equalsIgnoreCase(item.getTipoGestion())) {
                        item.setTipoGestion(sel);
                        aplicarEstilo(sel);
                        if ("Visita en terreno".equalsIgnoreCase(sel)) {
                            rbSalesCampo.setChecked(true);
                        }
                        if (listener != null) listener.onDataChanged();
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            // Detalle
            etSalesDetail.setText(item.getDetalle() != null ? item.getDetalle() : "");
            etSalesDetail.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.setDetalle(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Horarios
            String startStr = item.getHoraInicio() != null ? item.getHoraInicio() : "03:30 pm";
            String endStr = item.getHoraFin() != null ? item.getHoraFin() : "04:15 pm";
            tvSalesStartTime.setText(startStr);
            tvSalesEndTime.setText(endStr);

            View.OnClickListener startClick = v -> showTimePicker(item, true, tvSalesStartTime);
            if (llSalesStartTime != null) llSalesStartTime.setOnClickListener(startClick);
            tvSalesStartTime.setOnClickListener(startClick);

            View.OnClickListener endClick = v -> showTimePicker(item, false, tvSalesEndTime);
            if (llSalesEndTime != null) llSalesEndTime.setOnClickListener(endClick);
            tvSalesEndTime.setOnClickListener(endClick);

            // Estado
            rgSalesStatus.setOnCheckedChangeListener(null);
            if (item.isSalidaCampo()) {
                rbSalesCampo.setChecked(true);
                tvSalesStatusBadge.setText("● En terreno");
                tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_justificado));
                if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.VISIBLE);
            } else if (item.isConcretado()) {
                rbSalesConcretado.setChecked(true);
                tvSalesStatusBadge.setText("● Concretado");
                tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_entregado));
                if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.GONE);
            } else {
                rbSalesPendiente.setChecked(true);
                tvSalesStatusBadge.setText("● En seguimiento");
                tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_pendiente));
                if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.GONE);
            }

            rgSalesStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbSalesCampo) {
                    item.setEstado("Salida a campo");
                    tvSalesStatusBadge.setText("● En terreno");
                    tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_justificado));
                    if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.VISIBLE);
                    aplicarEstilo("Visita en terreno");
                } else if (checkedId == R.id.rbSalesConcretado) {
                    item.setEstado("Concretado");
                    tvSalesStatusBadge.setText("● Concretado");
                    tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_entregado));
                    if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.GONE);
                    aplicarEstilo(item.getTipoGestion());
                } else {
                    item.setEstado("En seguimiento");
                    tvSalesStatusBadge.setText("● En seguimiento");
                    tvSalesStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.kpi_status_pendiente));
                    if (llSalesCampoNotice != null) llSalesCampoNotice.setVisibility(View.GONE);
                    aplicarEstilo(item.getTipoGestion());
                }
                if (listener != null) listener.onDataChanged();
            });

            // Monto
            etSalesMonto.setText(item.getMonto() > 0 ? String.format(Locale.getDefault(), "%.2f", item.getMonto()) : "");
            etSalesMonto.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        item.setMonto(Double.parseDouble(s.toString()));
                        if (listener != null) listener.onDataChanged();
                    } catch (NumberFormatException ignored) {}
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Eliminar
            btnDeleteSalesActivity.setOnClickListener(v -> {
                if (listener != null) listener.onItemDeleted(getAdapterPosition());
            });
        }

        private void showTimePicker(KpiSalesActivityItem item, boolean isStartTime, TextView targetView) {
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
                if (listener != null) listener.onDataChanged();
            }, 16, 0, false);
            dialog.show();
        }

        private void aplicarEstilo(String tipo) {
            int color;
            String icon;

            if ("Cierre de venta".equalsIgnoreCase(tipo)) {
                color = ContextCompat.getColor(context, R.color.kpi_secondary);
                icon = "VEN";
            } else if ("Cotización".equalsIgnoreCase(tipo)) {
                color = ContextCompat.getColor(context, R.color.kpi_cat_maquetacion);
                icon = "COT";
            } else if ("Reunión / Demo".equalsIgnoreCase(tipo)) {
                color = ContextCompat.getColor(context, R.color.kpi_primary);
                icon = "DEM";
            } else if ("Visita en terreno".equalsIgnoreCase(tipo) || "Salida a campo".equalsIgnoreCase(tipo)) {
                color = ContextCompat.getColor(context, R.color.kpi_status_justificado);
                icon = "TER";
            } else if ("Seguimiento".equalsIgnoreCase(tipo)) {
                color = ContextCompat.getColor(context, R.color.kpi_warning);
                icon = "SEG";
            } else {
                color = ContextCompat.getColor(context, R.color.kpi_cat_api);
                icon = "ACT";
            }

            vAccentLineSales.setBackgroundColor(color);
            tvSalesIcon.setText(icon);
        }
    }
}
