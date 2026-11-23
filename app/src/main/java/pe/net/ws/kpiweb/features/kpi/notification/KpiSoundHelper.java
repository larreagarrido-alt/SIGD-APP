package pe.net.ws.kpiweb.features.kpi.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.SparseIntArray;

import pe.net.ws.kpiweb.R;

/**
 * Reproductor de sonidos personalizados del modulo KPI:
 * - kpi_sound_reminder_8pm: tono de recordatorio diario de las 8:00 PM.
 * - kpi_sound_weekend_success: confirmación local de la ficha, sin modificar el volumen del teléfono.
 */
public final class KpiSoundHelper {

    private static SoundPool soundPool;
    private static final SparseIntArray soundMap = new SparseIntArray();
    private static MediaPlayer activePlayer;

    private KpiSoundHelper() {}

    public static Uri getReminder8PmSoundUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName() + "/" + R.raw.kpi_sound_reminder_8pm);
    }

    public static void preload(Context context) {
        if (context == null) return;
        initSoundPool(context.getApplicationContext());
    }

    public static void playCollaboratorWeekendSuccess(Context context) {
        playSound(context, R.raw.kpi_sound_weekend_success, 1);
    }

    /** Confirmación de captura local; el servidor confirma la recepción por separado. */
    public static void playDailySuccess(Context context) {
        playSound(context, R.raw.kpi_sound_weekend_success, 1);
    }

    public static void playReminder8PmTone(Context context) {
        playSound(context, R.raw.kpi_sound_reminder_8pm, 1);
    }

    public static synchronized void playDoubleReminderTone(Context context) {
        playSound(context, R.raw.kpi_sound_reminder_8pm, 2);
    }

    private static synchronized void playSound(Context context, int rawResId, int repeatCount) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();

        // 1. Vibracion haptica de confirmacion
        try {
            Vibrator vib = (Vibrator) appCtx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(180);
                }
            }
        } catch (Throwable ignored) {}

        // 2. Reproducción al volumen elegido por la persona.
        int streamId = 0;
        try {
            initSoundPool(appCtx);
            int soundId = soundMap.get(rawResId, -1);
            if (soundId != -1) {
                int loop = Math.max(0, repeatCount - 1);
                streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, loop, 1.0f);
            } else {
                int newSoundId = soundPool.load(appCtx, rawResId, 1);
                soundMap.put(rawResId, newSoundId);
            }
        } catch (Throwable ignored) {}

        // 3. Si SoundPool todavía no cargó en memoria este recurso en el instante exacto,
        // reproducir inmediatamente con MediaPlayer para que siempre suene en el primer envio.
        if (streamId == 0) {
            playWithMediaPlayerFallback(appCtx, rawResId, repeatCount);
        }
    }

    private static synchronized void initSoundPool(Context appCtx) {
        if (soundPool == null) {
            AudioAttributes audioAttrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttrs)
                    .build();
            int s1 = soundPool.load(appCtx, R.raw.kpi_sound_weekend_success, 1);
            int s2 = soundPool.load(appCtx, R.raw.kpi_sound_reminder_8pm, 1);
            soundMap.put(R.raw.kpi_sound_weekend_success, s1);
            soundMap.put(R.raw.kpi_sound_reminder_8pm, s2);
        }
    }

    private static void playWithMediaPlayerFallback(Context appCtx, int rawResId, int maxPlays) {
        stopActivePlayer();
        try {
            MediaPlayer mp = MediaPlayer.create(appCtx, rawResId);
            if (mp == null) return;

            AudioAttributes audioAttrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mp.setAudioAttributes(audioAttrs);
            mp.setVolume(1.0f, 1.0f);

            final int[] playCount = {1};
            activePlayer = mp;
            mp.setOnCompletionListener(player -> {
                synchronized (KpiSoundHelper.class) {
                    if (playCount[0] < maxPlays) {
                        playCount[0]++;
                        try {
                            player.seekTo(0);
                            player.start();
                            return;
                        } catch (Exception ignored) {}
                    }
                    if (activePlayer == player) {
                        activePlayer = null;
                    }
                    try {
                        player.release();
                    } catch (Exception ignored) {}
                }
            });
            mp.setOnErrorListener((player, what, extra) -> {
                synchronized (KpiSoundHelper.class) {
                    if (activePlayer == player) {
                        activePlayer = null;
                    }
                    try {
                        player.release();
                    } catch (Exception ignored) {}
                }
                return true;
            });
            mp.start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static synchronized void stopActivePlayer() {
        if (activePlayer != null) {
            try {
                if (activePlayer.isPlaying()) {
                    activePlayer.stop();
                }
            } catch (Exception ignored) {}
            try {
                activePlayer.release();
            } catch (Exception ignored) {}
            activePlayer = null;
        }
    }
}
