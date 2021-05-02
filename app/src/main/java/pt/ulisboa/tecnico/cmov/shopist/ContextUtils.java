package pt.ulisboa.tecnico.cmov.shopist;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;

import java.util.Locale;

public class ContextUtils extends ContextWrapper {

    public ContextUtils(Context base) {
        super(base);
    }

    public static ContextWrapper updateLocale(Context context, String language) {
        Configuration config = context.getResources().getConfiguration();
        Locale sysLocale = getSystemLocale(config);
        if (!language.equals("") && !sysLocale.getLanguage().equals(language)) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            setSystemLocale(config, locale);
        }
        context = context.createConfigurationContext(config);
        return new ContextUtils(context);
    }

    public static Locale getSystemLocale(Configuration config){
        return config.getLocales().get(0);
    }

    public static void setSystemLocale(Configuration config, Locale locale){
        config.setLocale(locale);
    }
}
