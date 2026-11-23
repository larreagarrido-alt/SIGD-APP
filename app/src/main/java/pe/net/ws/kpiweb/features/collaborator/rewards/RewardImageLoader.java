package pe.net.ws.kpiweb.features.collaborator.rewards;

import android.app.Activity;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.ImageView;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/** Reads only authenticated prize images from this application's API origin. */
public final class RewardImageLoader {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private RewardImageLoader() {}

    public static void load(Activity activity, ImageView view, View placeholder, String relative, String bearer) {
        load(activity, view, placeholder, relative, bearer, 800);
    }

    public static void load(Activity activity, ImageView view, View placeholder, String relative, String bearer,
                            int maxDimension) {
        if (relative == null || !relative.matches("/websiteplus-api/api/v1/premios/[0-9]+/imagen")
                || bearer == null || !bearer.startsWith("Bearer ")) return;
        String url = "https://ws.net.pe" + relative;
        view.setTag(url);
        POOL.execute(() -> {
            HttpsURLConnection connection = null;
            try {
                connection = (HttpsURLConnection)new URL(url).openConnection();
                connection.setRequestProperty("Authorization", bearer);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(false);
                if (connection.getResponseCode()!=200 || connection.getContentLengthLong()>5242880) return;
                String mime=connection.getContentType();
                if (mime==null || !(mime.startsWith("image/jpeg") || mime.startsWith("image/png") || mime.startsWith("image/webp"))) return;
                ByteArrayOutputStream output=new ByteArrayOutputStream();
                try(InputStream input=connection.getInputStream()) {
                    byte[] buffer=new byte[8192]; int size;
                    while((size=input.read(buffer))!=-1) {
                        if (output.size()+size>5242880) return;
                        output.write(buffer,0,size);
                    }
                }
                final Bitmap ready = decode(output.toByteArray(), mime, maxDimension);
                if (ready == null) return;
                activity.runOnUiThread(() -> {
                    if(!activity.isFinishing() && !activity.isDestroyed() && url.equals(view.getTag())) {
                        view.setImageBitmap(ready);
                        if (placeholder != null) placeholder.setVisibility(View.GONE);
                    }
                });
            } catch(Exception ignored) { /* Keep the image placeholder. */ }
            finally { if(connection!=null) connection.disconnect(); }
        });
    }

    public static void loadLocal(Activity activity, ImageView view, Uri uri, int maxDimension) {
        if (uri == null) return;
        String key = uri.toString();
        view.setTag(key);
        POOL.execute(() -> {
            try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                if (input == null) return;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (output.size() + read > 5242880) return;
                    output.write(buffer, 0, read);
                }
                String mime = activity.getContentResolver().getType(uri);
                Bitmap ready = decode(output.toByteArray(), mime, maxDimension);
                if (ready == null) return;
                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed() && key.equals(view.getTag()))
                        view.setImageBitmap(ready);
                });
            } catch (Exception ignored) { /* The selected image remains untouched. */ }
        });
    }

    private static Bitmap decode(byte[] bytes, String mime, int maxDimension) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth < 1 || bounds.outHeight < 1
                || (long) bounds.outWidth * bounds.outHeight > 50000000) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight)
                / Math.max(800, maxDimension));
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null || mime == null || !mime.startsWith("image/jpeg")) return bitmap;
        int orientation = new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix correction = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) correction.postRotate(90);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) correction.postRotate(180);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) correction.postRotate(270);
        else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) correction.postScale(-1, 1);
        else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) correction.postScale(1, -1);
        else if (orientation == ExifInterface.ORIENTATION_TRANSPOSE) {
            correction.postScale(-1, 1); correction.postRotate(270);
        } else if (orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            correction.postScale(-1, 1); correction.postRotate(90);
        }
        return correction.isIdentity() ? bitmap : Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), correction, true);
    }
}
