// Honest pro iPhone: Honest OS (os.honestlead.cz) ve WebView, otevřený rovnou na Honest Chatu.
// Na iOS ve WebView nefunguje Web Push, proto appka žádá o vlastní push token (Expo/APNs) a předá ho stránce
// událostí "honest-native-push". Serverová část (uložit token, posílat přes Expo Push) zatím NENÍ: viz
// docs/IOS-TESTFLIGHT.md, sekce Upozornění. Bez ní chat funguje, upozornění chodí přes Telegram a e-mail.
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Linking, Platform, StyleSheet, View } from "react-native";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import { StatusBar } from "expo-status-bar";
import { WebView } from "react-native-webview";
import Constants from "expo-constants";
import * as Device from "expo-device";
import * as Notifications from "expo-notifications";

const extra = Constants.expoConfig?.extra ?? {};
const START_URL = extra.startUrl ?? "https://os.honestlead.cz/#lab-chat";
const HOST = extra.host ?? "os.honestlead.cz";

Notifications.setNotificationHandler({
  handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: true }),
});

async function pushToken() {
  if (!Device.isDevice) return null;
  const current = await Notifications.getPermissionsAsync();
  const status = current.granted ? "granted" : (await Notifications.requestPermissionsAsync()).status;
  if (status !== "granted") return null;
  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) return null;
  return (await Notifications.getExpoPushTokenAsync({ projectId })).data;
}

// Jen adresy Honest OS zůstávají v appce, ostatní odkazy (třeba přílohy jinde, web klienta) otevře Safari.
function isOwn(url) {
  try {
    return new URL(url).host === HOST;
  } catch {
    return false;
  }
}

export default function App() {
  const web = useRef(null);
  const [url, setUrl] = useState(START_URL);
  const [token, setToken] = useState(null);

  useEffect(() => {
    pushToken().then(setToken).catch(() => setToken(null));
    // Klepnutí na upozornění: otevřít adresu z dat upozornění (jen Honest OS).
    const sub = Notifications.addNotificationResponseReceivedListener((response) => {
      const target = response.notification.request.content.data?.url;
      if (typeof target !== "string" || !isOwn(target)) return;
      if (web.current) web.current.injectJavaScript(`location.href = ${JSON.stringify(target)}; true;`);
      else setUrl(target);
    });
    return () => sub.remove();
  }, []);

  const announce = () => {
    if (!token || !web.current) return;
    const detail = JSON.stringify({ platform: Platform.OS, token });
    web.current.injectJavaScript(`window.dispatchEvent(new CustomEvent("honest-native-push", { detail: ${detail} })); true;`);
  };

  useEffect(announce, [token]);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.root} edges={["top", "bottom"]}>
        <StatusBar style="dark" />
        <WebView
          ref={web}
          source={{ uri: url }}
          style={styles.root}
          sharedCookiesEnabled
          allowsBackForwardNavigationGestures
          allowsInlineMediaPlayback
          pullToRefreshEnabled
          startInLoadingState
          renderLoading={() => (
            <View style={styles.loading}>
              <ActivityIndicator />
            </View>
          )}
          onLoadEnd={announce}
          onShouldStartLoadWithRequest={(req) => {
            if (isOwn(req.url) || req.url.startsWith("about:") || req.url.startsWith("blob:") || req.url.startsWith("data:")) return true;
            Linking.openURL(req.url).catch(() => {});
            return false;
          }}
        />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#F7F7F9" },
  loading: { ...StyleSheet.absoluteFillObject, alignItems: "center", justifyContent: "center", backgroundColor: "#F7F7F9" },
});
