package wtf.demise.features.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModuleCategory {
    Combat("Combat"),
    Legit("Legit"),
    Movement("Movement"),
    Player("Player"),
    Misc("Misc"),
    Exploit("Exploit"),
    Visual("Visual"),
    Fun("Fun");

    private final String name;
}