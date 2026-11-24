package cz.honestlead.honest;

import android.os.Bundle;

/** Spusti Honest OS v Trusted Web Activity a na pozadi zkontroluje, jestli neni nova verze APK. */
public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UpdateCheck.maybeRun(this);
    }
}
