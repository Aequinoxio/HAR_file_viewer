import java.util.prefs.Preferences;

public enum AppPrefs {
    LoadFileLocation,
    SaveFolderLocation;
    private static Preferences prefs = Preferences.userRoot().node(AppPrefs.class.getName());

    String get(String defaultValue){
        return prefs.get(this.name(),defaultValue);
    }

    void put(String value){
        prefs.put(this.name(),value);
    }
}
