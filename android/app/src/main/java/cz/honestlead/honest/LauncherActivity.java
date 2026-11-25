package cz.honestlead.honest;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Spusti Honest OS v Trusted Web Activity a na pozadi zkontroluje, jestli neni nova verze APK.
 * Kdyz uz je o nove verzi vedet (z predchozi kontroly), nejdriv se zepta: Aktualizovat (stahne APK,
 * Android pak nabidne instalaci pres starou verzi) nebo Pozdeji (pripomene se za 12 hodin).
 * Web (menu, chat, upozorneni) se aktualizuje sam, novou APK je potreba jen pri zmene obalu.
 */
public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {
    private String pendingUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pendingUpdate = UpdateCheck.pendingName(this);
        super.onCreate(savedInstanceState);
        UpdateCheck.maybeRun(this);
        if (pendingUpdate != null && !isFinishing()) askUpdate(pendingUpdate);
    }

    @Override
    protected boolean shouldLaunchImmediately() {
        return pendingUpdate == null;
    }

    private void askUpdate(String name) {
        String title = name.isEmpty() ? "Nová verze Honest" : "Nová verze Honest " + name;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(title)
                .setMessage("Stáhni ji a nainstaluj přes současnou. Nic se neztratí, zůstaneš přihlášený.")
                .setPositiveButton("Aktualizovat", (d, w) -> {
                    Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.updateApkUrl)));
                    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(open);
                        finish();
                    } catch (Exception e) {
                        launchTwa();
                    }
                })
                .setNegativeButton("Později", (d, w) -> {
                    UpdateCheck.snooze(this);
                    launchTwa();
                })
                .setOnCancelListener(d -> {
                    UpdateCheck.snooze(this);
                    launchTwa();
                })
                .show();
    }
}
