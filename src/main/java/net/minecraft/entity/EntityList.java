package net.minecraft.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.ai.EntityMinecartMobSpawner;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityList {
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, Class<? extends Entity>> stringToClassMapping = Maps.newHashMap();
    private static final Map<Class<? extends Entity>, String> classToStringMapping = Maps.newHashMap();
    private static final Map<Integer, Class<? extends Entity>> idToClassMapping = Maps.newHashMap();
    private static final Map<Class<? extends Entity>, Integer> classToIDMapping = Maps.newHashMap();
    private static final Map<String, Integer> stringToIDMapping = Maps.newHashMap();
    public static final Map<Integer, EntityList.EntityEggInfo> entityEggs = Maps.newLinkedHashMap();

    private static void addMapping(Class<? extends Entity> entityClass, String entityName, int id) {
        if (stringToClassMapping.containsKey(entityName)) {
            throw new IllegalArgumentException("ID is already registered: " + entityName);
        } else if (idToClassMapping.containsKey(id)) {
            throw new IllegalArgumentException("ID is already registered: " + id);
        } else if (id == 0) {
            throw new IllegalArgumentException("Cannot register to reserved id: " + id);
        } else if (entityClass == null) {
            throw new IllegalArgumentException("Cannot register null clazz for id: " + id);
        } else {
            stringToClassMapping.put(entityName, entityClass);
            classToStringMapping.put(entityClass, entityName);
            idToClassMapping.put(id, entityClass);
            classToIDMapping.put(entityClass, id);
            stringToIDMapping.put(entityName, id);
        }
    }

    private static void addMapping(Class<? extends Entity> entityClass, String entityName, int entityID, int baseColor, int spotColor) {
        addMapping(entityClass, entityName, entityID);
        entityEggs.put(entityID, new EntityList.EntityEggInfo(entityID, baseColor, spotColor));
    }

    public static Entity createEntityByName(String entityName, World worldIn) {
        Entity entity = null;

        try {
            Class<? extends Entity> oclass = stringToClassMapping.get(entityName);

            if (oclass != null) {
                entity = oclass.getConstructor(new Class[]{World.class}).newInstance(worldIn);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return entity;
    }

    public static Entity createEntityFromNBT(NBTTagCompound nbt, World worldIn) {
        Entity entity = null;

        if ("Minecart".equals(nbt.getString("id"))) {
            nbt.setString("id", EntityMinecart.EnumMinecartType.byNetworkID(nbt.getInteger("Type")).getName());
            nbt.removeTag("Type");
        }

        try {
            Class<? extends Entity> oclass = stringToClassMapping.get(nbt.getString("id"));

            if (oclass != null) {
                entity = oclass.getConstructor(new Class[]{World.class}).newInstance(worldIn);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (entity != null) {
            entity.readFromNBT(nbt);
        } else {
            logger.warn("Skipping Entity with id {}", nbt.getString("id"));
        }

        return entity;
    }

    public static Entity createEntityByID(int entityID, World worldIn) {
        Entity entity = null;

        try {
            Class<? extends Entity> oclass = getClassFromID(entityID);

            if (oclass != null) {
                entity = oclass.getConstructor(new Class[]{World.class}).newInstance(worldIn);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (entity == null) {
            logger.warn("Skipping Entity with id {}", entityID);
        }

        return entity;
    }

    public static int getEntityID(Entity entityIn) {
        Integer integer = classToIDMapping.get(entityIn.getClass());
        return integer == null ? 0 : integer;
    }

    public static Class<? extends Entity> getClassFromID(int entityID) {
        return idToClassMapping.get(entityID);
    }

    public static String getEntityString(Entity entityIn) {
        return classToStringMapping.get(entityIn.getClass());
    }

    public static int getIDFromString(String entityName) {
        Integer integer = stringToIDMapping.get(entityName);
        return integer == null ? 90 : integer;
    }

    public static String getStringFromID(int entityID) {
        return classToStringMapping.get(getClassFromID(entityID));
    }

    public static void func_151514_a() {
    }

    public static List<String> getEntityNameList() {
        Set<String> set = stringToClassMapping.keySet();
        List<String> list = Lists.newArrayList();

        for (String s : set) {
            Class<? extends Entity> oclass = stringToClassMapping.get(s);

            if ((oclass.getModifiers() & 1024) != 1024) {
                list.add(s);
            }
        }

        list.add("LightningBolt");
        return list;
    }

    public static boolean isStringEntityName(Entity entityIn, String entityName) {
        String s = getEntityString(entityIn);

        if (s == null && entityIn instanceof EntityPlayer) {
            s = "Player";
        } else if (s == null && entityIn instanceof EntityLightningBolt) {
            s = "LightningBolt";
        }

        return entityName.equals(s);
    }

    public static boolean isStringValidEntityName(String entityName) {
        return "Player".equals(entityName) || getEntityNameList().contains(entityName);
    }

    static {
        addMapping(EntityItem.class, "Item", 1);
        addMapping(EntityXPOrb.class, "XPOrb", 2);
        addMapping(EntityEgg.class, "ThrownEgg", 7);
        addMapping(EntityLeashKnot.class, "LeashKnot", 8);
        addMapping(EntityPainting.class, "Painting", 9);
        addMapping(EntityArrow.class, "Arrow", 10);
        addMapping(EntitySnowball.class, "Snowball", 11);
        addMapping(EntityLargeFireball.class, "Fireball", 12);
        addMapping(EntitySmallFireball.class, "SmallFireball", 13);
        addMapping(EntityEnderPearl.class, "ThrownEnderpearl", 14);
        addMapping(EntityEnderEye.class, "EyeOfEnderSignal", 15);
        addMapping(EntityPotion.class, "ThrownPotion", 16);
        addMapping(EntityExpBottle.class, "ThrownExpBottle", 17);
        addMapping(EntityItemFrame.class, "ItemFrame", 18);
        addMapping(EntityWitherSkull.class, "WitherSkull", 19);
        addMapping(EntityTNTPrimed.class, "PrimedTnt", 20);
        addMapping(EntityFallingBlock.class, "FallingSand", 21);
        addMapping(EntityFireworkRocket.class, "FireworksRocketEntity", 22);
        addMapping(EntityArmorStand.class, "ArmorStand", 30);
        addMapping(EntityBoat.class, "Boat", 41);
        addMapping(EntityMinecartEmpty.class, EntityMinecart.EnumMinecartType.RIDEABLE.getName(), 42);
        addMapping(EntityMinecartChest.class, EntityMinecart.EnumMinecartType.CHEST.getName(), 43);
        addMapping(EntityMinecartFurnace.class, EntityMinecart.EnumMinecartType.FURNACE.getName(), 44);
        addMapping(EntityMinecartTNT.class, EntityMinecart.EnumMinecartType.TNT.getName(), 45);
        addMapping(EntityMinecartHopper.class, EntityMinecart.EnumMinecartType.HOPPER.getName(), 46);
        addMapping(EntityMinecartMobSpawner.class, EntityMinecart.EnumMinecartType.SPAWNER.getName(), 47);
        addMapping(EntityMinecartCommandBlock.class, EntityMinecart.EnumMinecartType.COMMAND_BLOCK.getName(), 40);
        addMapping(EntityLiving.class, "Mob", 48);
        addMapping(EntityMob.class, "Monster", 49);
        addMapping(EntityCreeper.class, "Creeper", 50, 894731, 0);
        addMapping(EntitySkeleton.class, "Skeleton", 51, 12698049, 4802889);
        addMapping(EntitySpider.class, "Spider", 52, 3419431, 11013646);
        addMapping(EntityGiantZombie.class, "Giant", 53);
        addMapping(EntityZombie.class, "Zombie", 54, 44975, 7969893);
        addMapping(EntitySlime.class, "Slime", 55, 5349438, 8306542);
        addMapping(EntityGhast.class, "Ghast", 56, 16382457, 12369084);
        addMapping(EntityPigZombie.class, "PigZombie", 57, 15373203, 5009705);
        addMapping(EntityEnderman.class, "Enderman", 58, 1447446, 0);
        addMapping(EntityCaveSpider.class, "CaveSpider", 59, 803406, 11013646);
        addMapping(EntitySilverfish.class, "Silverfish", 60, 7237230, 3158064);
        addMapping(EntityBlaze.class, "Blaze", 61, 16167425, 16775294);
        addMapping(EntityMagmaCube.class, "LavaSlime", 62, 3407872, 16579584);
        addMapping(EntityDragon.class, "EnderDragon", 63);
        addMapping(EntityWither.class, "WitherBoss", 64);
        addMapping(EntityBat.class, "Bat", 65, 4996656, 986895);
        addMapping(EntityWitch.class, "Witch", 66, 3407872, 5349438);
        addMapping(EntityEndermite.class, "Endermite", 67, 1447446, 7237230);
        addMapping(EntityGuardian.class, "Guardian", 68, 5931634, 15826224);
        addMapping(EntityPig.class, "Pig", 90, 15771042, 14377823);
        addMapping(EntitySheep.class, "Sheep", 91, 15198183, 16758197);
        addMapping(EntityCow.class, "Cow", 92, 4470310, 10592673);
        addMapping(EntityChicken.class, "Chicken", 93, 10592673, 16711680);
        addMapping(EntitySquid.class, "Squid", 94, 2243405, 7375001);
        addMapping(EntityWolf.class, "Wolf", 95, 14144467, 13545366);
        addMapping(EntityMooshroom.class, "MushroomCow", 96, 10489616, 12040119);
        addMapping(EntitySnowman.class, "SnowMan", 97);
        addMapping(EntityOcelot.class, "Ozelot", 98, 15720061, 5653556);
        addMapping(EntityIronGolem.class, "VillagerGolem", 99);
        addMapping(EntityHorse.class, "EntityHorse", 100, 12623485, 15656192);
        addMapping(EntityRabbit.class, "Rabbit", 101, 10051392, 7555121);
        addMapping(EntityVillager.class, "Villager", 120, 5651507, 12422002);
        addMapping(EntityEnderCrystal.class, "EnderCrystal", 200);
    }

    public static class EntityEggInfo {
        public final int spawnedID;
        public final int primaryColor;
        public final int secondaryColor;
        public final StatBase field_151512_d;
        public final StatBase field_151513_e;

        public EntityEggInfo(int id, int baseColor, int spotColor) {
            this.spawnedID = id;
            this.primaryColor = baseColor;
            this.secondaryColor = spotColor;
            this.field_151512_d = StatList.getStatKillEntity(this);
            this.field_151513_e = StatList.getStatEntityKilledBy(this);
        }
    }
}
