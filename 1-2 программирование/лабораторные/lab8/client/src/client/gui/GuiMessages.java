package client.gui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * ResourceBundle wrapper for GUI texts.
 */
public final class GuiMessages {
    private static final String BUNDLE_NAME = "messages";

    private final Locale locale;
    private final ResourceBundle bundle;

    private GuiMessages(Locale locale) {
        this.locale = Objects.requireNonNullElse(locale, Locale.forLanguageTag("ru"));
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, this.locale);
    }

    public static GuiMessages forLocale(Locale locale) {
        return new GuiMessages(locale);
    }

    public Locale locale() {
        return locale;
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String format(String key, Object... arguments) {
        MessageFormat format = new MessageFormat(get(key), locale);
        return format.format(arguments);
    }
}
