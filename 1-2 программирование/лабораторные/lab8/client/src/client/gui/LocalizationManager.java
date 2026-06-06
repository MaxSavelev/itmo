package client.gui;

import java.util.Locale;
import java.util.Objects;

/**
 * Stores current GUI locale and exposes messages for that locale.
 */
public final class LocalizationManager {
    private Locale locale;
    private GuiMessages messages;

    public LocalizationManager(Locale locale) {
        setLocale(locale);
    }

    public Locale getLocale() {
        return locale;
    }

    public GuiMessages getMessages() {
        return messages;
    }

    public void setLocale(Locale locale) {
        this.locale = Objects.requireNonNullElse(locale, Locale.forLanguageTag("ru"));
        messages = GuiMessages.forLocale(this.locale);
    }
}
