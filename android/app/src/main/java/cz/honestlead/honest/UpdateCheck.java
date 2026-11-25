package cz.honestlead.honest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Kontrola nove verze nativniho obalu. Vetsina zmen prijde sama z webu, novou APK je potreba jen vyjimecne.
 * Nejvys jednou za 6 hodin stahne version.json z GitHub Releases a kdyz je tam vyssi versionCode,
 * ukaze jedno upozorneni "Nova verze Honest" s odkazem na stazeni. Zadne udaje se nikam neposilaji.
 */
final class UpdateCheck {
    private static final String PREFS = "honest_update";
    // Kontrola nejvys jednou za 30 minut (pri spusteni appky), "Pozdeji" odlozi dotaz o 12 hodin.
    private static final long INTERVAL_MS = 30L * 60 * 1000;
    private static final long SNOOZE_MS = 12L * 60 * 60 * 1000;
    private static final String CHANNEL = "honest_update";

    private UpdateCheck() {}

    /** Nazev novejsi verze, o ktere uz vime z predchozi kontroly a ktera neni odlozena; jinak null. */
    static String pendingName(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long latest = prefs.getLong("latestCode", 0L);
            if (latest <= currentVersionCode(context)) return null;
            if (prefs.getLong("snoozedCode", 0L) == latest && System.currentTimeMillis() < prefs.getLong("snoozedUntil", 0L)) return null;
            return prefs.getString("latestName", "");
        } catch (Exception e) {
            return null;
        }
    }

    static void snooze(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putLong("snoozedCode", prefs.getLong("latestCode", 0L))
                .putLong("snoozedUntil", System.currentTimeMillis() + SNOOZE_MS).apply();
    }

    static void maybeRun(Context context) {
        final Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong("lastCheck", 0L) < INTERVAL_MS) return;
        prefs.edit().putLong("lastCheck", now).apply();
        new Thread(() -> run(app), "honest-update").start();
    }

    private static void run(Context context) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(context.getString(R.string.updateJsonUrl)).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != 200) return;
            JSONObject json = new JSONObject(read(conn.getInputStream()));
            long latest = json.optLong("versionCode", 0L);
            String name = json.optString("versionName", "");
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            prefs.edit().putLong("latestCode", latest).putString("latestName", name).apply();
            if (latest <= currentVersionCode(context)) return;
            if (prefs.getLong("notified", 0L) == latest) return;
            prefs.edit().putLong("notified", latest).apply();
            notifyUpdate(context, name);
        } catch (Exception ignored) {
            // Bez site nebo s chybou se nic nedeje, zkusi se to pri pristim spusteni.
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                out.write(buf, 0, n);
                if (out.size() > 64 * 1024) throw new IllegalStateException("version.json je moc velky");
            }
            return out.toString("UTF-8");
        }
    }

    @SuppressWarnings("deprecation")
    private static long currentVersionCode(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private static void notifyUpdate(Context context, String versionName) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Aktualizace aplikace", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.updateApkUrl)));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = versionName.isEmpty() ? "Nová verze Honest" : "Nová verze Honest " + versionName;
        Notification n = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(title)
                .setContentText("Klepni, stáhni a nainstaluj přes starou verzi. Nic se neztratí.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            nm.notify(1001, n);
        } catch (SecurityException ignored) {
            // Upozorneni nejsou povolena: nevadi, nova verze je i na webu appky.
        }
    }
}
