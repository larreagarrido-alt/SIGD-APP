package pe.net.ws.kpiweb.features.collaborator.common;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;

/** Keeps demo figures out of a worker's authenticated account. */
public final class CollaboratorDataState {
    private CollaboratorDataState() {}

    public static boolean isRealWorker(CollaboratorProfile profile) {
        return profile != null && profile.getId() != null && profile.getId().matches("[0-9]+");
    }

    public static void showUnavailable(Activity activity, int scrollId, String title, String detail) {
        ScrollView scroll = activity.findViewById(scrollId);
        if (scroll == null) return;
        scroll.removeAllViews();
        int pad = (int) (24 * activity.getResources().getDisplayMetrics().density);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundResource(R.drawable.collab_bg_card);
        ScrollView.LayoutParams cardParams = new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(pad, pad, pad, pad);
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextSize(18);
        heading.setTextColor(activity.getColor(R.color.collab_text_primary));
        TextView description = new TextView(activity);
        description.setText(detail);
        description.setTextSize(14);
        description.setTextColor(activity.getColor(R.color.collab_text_secondary));
        description.setPadding(0, pad / 2, 0, 0);
        card.addView(heading);
        card.addView(description);
        scroll.addView(card, cardParams);
    }
}
