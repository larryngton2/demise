package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "Cape", category = ModuleCategory.Visual)
public class Cape extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Minecraft", "Rise"}, "Normal", this);
    public final ModeValue riseMode = new ModeValue("Rise mode", new String[]{"Normal", "Red", "Green", "Blue", "Dogshit"}, "Normal", this, () -> mode.is("Rise"));
    public final ModeValue mcMode = new ModeValue("Minecraft mode",
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
            }, "Mojang", this, () -> mode.is("Minecraft"));

    public final BoolValue enchanted = new BoolValue("Enchanted", false, this, () -> mode.is("Normal"));
    public final BoolValue alternativePhysics = new BoolValue("Alternative physics", false, this);
}
