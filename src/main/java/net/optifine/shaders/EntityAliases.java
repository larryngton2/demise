package net.optifine.shaders;

import net.minecraft.src.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.config.ConnectedParser;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.shaders.config.MacroProcessor;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.StrUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EntityAliases {
    private static int[] entityAliases = null;
    private static boolean updateOnResourcesReloaded;

    public static int getEntityAliasId(int entityId) {
        if (entityAliases == null) {
            return -1;
        } else if (entityId >= 0 && entityId < entityAliases.length) {
            return entityAliases[entityId];
        } else {
            return -1;
        }
    }

    public static void resourcesReloaded() {
        if (updateOnResourcesReloaded) {
            updateOnResourcesReloaded = false;
            update(Shaders.getShaderPack());
        }
    }

    public static void update(IShaderPack shaderPack) {
        reset();

        if (shaderPack != null) {
            if (Reflector.Loader_getActiveModList.exists() && Config.getResourceManager() == null) {
                Config.dbg("[Shaders] Delayed loading of entity mappings after resources are loaded");
                updateOnResourcesReloaded = true;
            } else {
                List<Integer> list = new ArrayList<>();
                String s = "/shaders/entity.properties";
                InputStream inputstream = shaderPack.getResourceAsStream(s);

                if (inputstream != null) {
                    loadEntityAliases(inputstream, s, list);
                }

                loadModEntityAliases(list);

                if (!list.isEmpty()) {
                    entityAliases = toArray(list);
                }
            }
        }
    }

    private static void loadModEntityAliases(List<Integer> listEntityAliases) {
        String[] astring = ReflectorForge.getForgeModIds();

        for (String s : astring) {
            try {
                ResourceLocation resourcelocation = new ResourceLocation(s, "shaders/entity.properties");
                InputStream inputstream = Config.getResourceStream(resourcelocation);
                loadEntityAliases(inputstream, resourcelocation.toString(), listEntityAliases);
            } catch (IOException ignored) {
            }
        }
    }

    private static void loadEntityAliases(InputStream in, String path, List<Integer> listEntityAliases) {
        if (in != null) {
            try {
                in = MacroProcessor.process(in, path);
                Properties properties = new PropertiesOrdered();
                properties.load(in);
                in.close();
                Config.dbg("[Shaders] Parsing entity mappings: " + path);
                ConnectedParser connectedparser = new ConnectedParser("Shaders");

                for (Object o : properties.keySet()) {
                    String s = (String) o;
                    String s1 = properties.getProperty(s);
                    String s2 = "entity.";

                    if (!s.startsWith(s2)) {
                        Config.warn("[Shaders] Invalid entity ID: " + s);
                    } else {
                        String s3 = StrUtils.removePrefix(s, s2);
                        int i = Config.parseInt(s3, -1);

                        if (i < 0) {
                            Config.warn("[Shaders] Invalid entity alias ID: " + i);
                        } else {
                            int[] aint = connectedparser.parseEntities(s1);

                            if (aint != null && aint.length >= 1) {
                                for (int k : aint) {
                                    addToList(listEntityAliases, k, i);
                                }
                            } else {
                                Config.warn("[Shaders] Invalid entity ID mapping: " + s + "=" + s1);
                            }
                        }
                    }
                }
            } catch (IOException var15) {
                Config.warn("[Shaders] Error reading: " + path);
            }
        }
    }

    private static void addToList(List<Integer> list, int index, int val) {
        while (list.size() <= index) {
            list.add(-1);
        }

        list.set(index, val);
    }

    private static int[] toArray(List<Integer> list) {
        int[] aint = new int[list.size()];

        for (int i = 0; i < aint.length; ++i) {
            aint[i] = list.get(i);
        }

        return aint;
    }

    public static void reset() {
        entityAliases = null;
    }
}
