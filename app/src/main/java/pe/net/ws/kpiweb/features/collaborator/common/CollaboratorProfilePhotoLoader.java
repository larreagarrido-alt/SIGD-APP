package pe.net.ws.kpiweb.features.collaborator.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.session.SessionManager;

/** Loads worker photos through the authenticated API, with the legacy public URL as fallback. */
public final class CollaboratorProfilePhotoLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private CollaboratorProfilePhotoLoader() { }

    public static void load(Activity activity, ImageView photoView, TextView initialsView, String rawUrl) {
        load(activity, photoView, initialsView, rawUrl, new SessionManager(activity).getWorkerId());
    }

    public static void load(Activity activity, ImageView photoView, TextView initialsView,
                            String rawUrl, int workerId) {
        if (photoView == null || initialsView == null) return;
        photoView.setVisibility(View.GONE);
        initialsView.setVisibility(View.VISIBLE);
        String safeUrl = resolvePhotoUrl(rawUrl);
        SessionManager session = new SessionManager(activity);
        String token = session.getAccessToken();
        String privateUrl = workerId > 0 && session.isLoggedIn() && token != null && !token.isEmpty()
                ? ApiClient.BASE_URL + "personal/" + workerId + "/foto" : null;
        String key = privateUrl != null ? privateUrl : safeUrl;
        photoView.setTag(key);
        if (key == null) return;

        EXECUTOR.execute(() -> {
            try {
                Bitmap bitmap = null;
                if (privateUrl != null) {
                    try { bitmap = fetch(privateUrl, token); }
                    catch (Exception ignored) { /* Try the original attendance URL. */ }
                }
                if (bitmap == null && safeUrl != null) bitmap = fetch(safeUrl, null);
                if (bitmap == null) return;
                Bitmap loaded = bitmap;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()
                            || !key.equals(photoView.getTag())
                            || privateUrl != null && !token.equals(new SessionManager(activity).getAccessToken())) return;
                    photoView.setImageBitmap(loaded);
                    photoView.setVisibility(View.VISIBLE);
                    initialsView.setVisibility(View.GONE);
                });
            } catch (Exception ignored) {
                // Keep the initials when the server has no accessible profile photo.
            }
        });
    }

    private static Bitmap fetch(String url, String token) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(3500);
            if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (connection.getResponseCode() != 200 || connection.getContentLength() > MAX_BYTES) return null;
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) return null;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = input.read(chunk)) != -1) {
                    if (output.size() + count > MAX_BYTES) return null;
                    output.write(chunk, 0, count);
                }
            }
            byte[] bytes = output.toByteArray();
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options sampled = new BitmapFactory.Options();
            sampled.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 512);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, sampled);
        } finally {
            connection.disconnect();
        }
    }

    static String resolvePhotoUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String value = rawUrl.trim();
        if (value.isEmpty() || value.startsWith("//") || value.indexOf('\\') >= 0) return null;
        if (!value.startsWith("https://") && !value.startsWith("http://")
                && !value.contains(":")) value = "https://ws.net.pe/" + (value.startsWith("/") ? value.substring(1) : value);
        Uri uri = Uri.parse(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"ws.net.pe".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null || uri.getPort() > 0 && uri.getPort() != 443
                || uri.getFragment() != null) return null;
        for (String segment : uri.getPathSegments()) {
            if ("..".equals(segment) || ".".equals(segment)) return null;
        }
        return uri.toString();
    }
}
