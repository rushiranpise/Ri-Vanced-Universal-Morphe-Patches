package app.morphe.extension.shared.settings;

import static app.morphe.extension.shared.settings.Setting.parent;
import static java.lang.Boolean.FALSE;

public class YouTubeAndMusicSettings extends BaseSettings {
    // Custom filter
    public static final BooleanSetting CUSTOM_FILTER = new BooleanSetting("rivanced_custom_filter", FALSE);
    public static final StringSetting CUSTOM_FILTER_STRINGS = new StringSetting("rivanced_custom_filter_strings", "", true, parent(CUSTOM_FILTER));

    // Miscellaneous
    public static final BooleanSetting DEBUG_PROTOCOLBUFFER = new BooleanSetting("rivanced_debug_protocolbuffer", FALSE, false,
            "rivanced_debug_protocolbuffer_user_dialog_message", parent(BaseSettings.DEBUG));
}
