# Minifikace je vypnuta (build.gradle). Kdyby se zapnula, tridy androidbrowserhelper se nesmi prejmenovat:
# Android je hleda podle jmena a appka by hned po spusteni spadla.
-keep class com.google.androidbrowserhelper.** { *; }
-keep class cz.honestlead.honest.** { *; }
