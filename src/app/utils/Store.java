package app.utils;

import java.util.prefs.Preferences;

public class Store {
    public static Preferences getPreferences() {
        return Preferences.userRoot().node(Store.class.getName());
    }
}
