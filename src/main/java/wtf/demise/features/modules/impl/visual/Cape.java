package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.features.values.impl.ModeValue;

import java.awt.*;

@ModuleInfo(name = "Cape", category = ModuleCategory.Visual)
public class Cape extends Module {
    public final ModeValue capeMode = new ModeValue("Cape mode", new String[]{"Default", "Normal", "Minecraft", "Rise", "What the fuck."}, "Normal", this);
    public final ModeValue riseMode = new ModeValue("Rise cape mode", new String[]{"Normal", "Red", "Green", "Blue", "Dogshit"}, "Normal", this, () -> capeMode.is("Rise"));
    public final ModeValue mcMode = new ModeValue("Minecraft cape mode",
            new String[]{
                    "15thAnniversary",
                    "Birthday",
                    "CherryBlossom",
                    "Cobalt",
                    "dB",
                    "Followers",
                    "MCC15Year",
                    "MillionthCustomer",
                    "MineCon2011",
                    "MineCon2012",
                    "MineCon2013",
                    "MineCon2015",
                    "MineCon2016",
                    "MinecraftExperience",
                    "Mojang",
                    "MojangClassic",
                    "MojangOffice",
                    "MojangStudios",
                    "MojiraMod",
                    "Prismarine",
                    "RealmsMapMaker",
                    "Scrolls",
                    "Snowman",
                    "Spade",
                    "test",
                    "Translator",
                    "TranslatorCN",
                    "TranslatorJP",
                    "Turtle",
                    "Valentine",
                    "Vanilla"
            }, "Mojang", this, () -> capeMode.is("Minecraft"));

    public final BoolValue enchanted = new BoolValue("Enchanted", false, this, () -> capeMode.is("Normal"));
    public final BoolValue alternativePhysics = new BoolValue("Alternative cape physics", false, this);
    public final BoolValue tint = new BoolValue("Cape tint", false, this);
    public final ColorValue color = new ColorValue("Tint color", Color.white, this, tint::get);
}
