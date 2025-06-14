package wtf.demise.gui.altmanager.login;

import org.apache.commons.lang3.StringUtils;

public enum AltType {

    CRACKED,
    PREMIUM;

    private final String capitalized;

    AltType() {
        this.capitalized = StringUtils.capitalize(name().toLowerCase());
    }

    public String getCapitalized() {
        return this.capitalized;
    }

}
