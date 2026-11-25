package cz.honestlead.honest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

/**
 * Prijima od Chromu upozorneni webu os.honestlead.cz a ukazuje je jako upozorneni appky Honest.
 *
 * Urgentni zpravy z Honest Chatu (web je oznaci tagem "urgent-<id>" a titulkem "🚨 URGENTNÍ", viz chat-sw.js)
 * presune do vlastniho kanalu "Urgentní zprávy": vysoka dulezitost, dlouha agresivni vibrace a zvuk vyzvaneni.
 * Delku vibrace a zvuk na Androidu 8+ ridi kanal, ne web, proto to musi udelat appka.
 * Kanal se po vytvoreni uz neda zmenit (jen uzivatelem v Nastaveni), proto ma ID s verzi: zmena vzoru = nove ID.
 */
public class DelegationService extends com.google.androidbrowserhelper.trusted.DelegationService {
    static final String URGENT_CHANNEL = "honest_urgent_v1";
    // ~6 s: tri dlouhe zabzuceni, pak jedno extra dlouhe
    private static final long[] URGENT_VIBRATION = {0, 900, 250, 900, 250, 900, 400, 2200};

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public boolean onNotifyNotificationWithChannel(String platformTag, int platformId, Notification notification, String channelName) {
        if (Build.VERSION.SDK_INT >= 26 && isUrgent(platformTag, notification)) {
            try {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && nm.areNotificationsEnabled()) {
                    ensureUrgentChannel(nm);
                    Notification.Builder b = Notification.Builder.recoverBuilder(this, notification)
                            .setChannelId(URGENT_CHANNEL)
                            .setCategory(Notification.CATEGORY_ALARM)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setGroupAlertBehavior(Notification.GROUP_ALERT_ALL);
                    nm.notify(platformTag, platformId, b.build());
                    return true;
                }
            } catch (Exception ignored) {
                // Kdyby cokoli selhalo, ukaze se aspon bezne upozorneni.
            }
        }
        return super.onNotifyNotificationWithChannel(platformTag, platformId, notification, channelName);
    }

    private static boolean isUrgent(String platformTag, Notification n) {
        if (platformTag != null && platformTag.contains("urgent-")) return true;
        CharSequence title = n.extras != null ? n.extras.getCharSequence(Notification.EXTRA_TITLE) : null;
        return title != null && title.toString().contains("URGENTN");
    }

    private void ensureUrgentChannel(NotificationManager nm) {
        if (nm.getNotificationChannel(URGENT_CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(URGENT_CHANNEL, "Urgentní zprávy", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Urgentní zprávy z Honest Chatu: dlouhá vibrace a vyzvánění, i ve ztlumené konverzaci.");
        ch.enableVibration(true);
        ch.setVibrationPattern(URGENT_VIBRATION);
        ch.enableLights(true);
        ch.setLightColor(0xFFE0245E);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setBypassDnd(true); // projde pres Nerusit jen kdyz to uzivatel u appky povoli
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ch.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        nm.createNotificationChannel(ch);
    }
}
