package cz.honestlead.honest;

/** Prijima od Chromu upozorneni webu os.honestlead.cz a ukazuje je jako upozorneni appky Honest. */
public class DelegationService extends com.google.androidbrowserhelper.trusted.DelegationService {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
