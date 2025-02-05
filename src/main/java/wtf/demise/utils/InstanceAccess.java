package wtf.demise.utils;

import net.minecraft.client.Minecraft;
import wtf.demise.Demise;

public interface InstanceAccess {

    Minecraft mc = Minecraft.getMinecraft();

    Demise INSTANCE = Demise.INSTANCE;
}

