package app.utils;

import java.util.prefs.Preferences;

public class Token {
    public static String getAuthorizationToken(){
        Preferences prefs = Store.getPreferences();
        return "Bearer " + prefs.get("token", "");
    }
}
