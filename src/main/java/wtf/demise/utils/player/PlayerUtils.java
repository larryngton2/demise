package wtf.demise.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.optifine.reflect.Reflector;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PlayerUtils implements InstanceAccess {
    public boolean isBlockUnder(double height, boolean boundingBox) {
        if (boundingBox) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, -height, 0);

            return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty();
        } else {
            for (int offset = 0; offset < height; offset++) {
                if (PlayerUtils.blockRelativeToPlayer(0, -offset, 0).isFullBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    public int findTool(BlockPos blockPos) {
        float bestSpeed = 1;
        int bestSlot = -1;

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack == null) {
                continue;
            }

            float speed = itemStack.getStrVsBlock(blockState.getBlock());

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public boolean overVoid() {
        return overVoid(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    public boolean overVoid(double posX, double posY, double posZ) {
        for (int i = (int) posY; i > -1; i--) {
            if (!(mc.theWorld.getBlockState(new BlockPos(posX, i, posZ)).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public boolean isInTeam(Entity entity) {
        if (mc.thePlayer.getDisplayName() != null && entity.getDisplayName() != null) {
            String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
            String clientName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
            return targetName.startsWith("§" + clientName.charAt(1));
        }
        return false;
    }

    public double getDistanceToEntityBox(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1);
        Vec3 pos = RotationUtils.getBestHitVec(entity);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
    }

    public double getCustomDistanceToEntityBox(Vec3 playerPos, Entity entity) {
        Vec3 eyes = new Vec3(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
        Vec3 pos = RotationUtils.getBestHitVec(entity);
        double xDist = Math.abs(pos.xCoord - eyes.xCoord);
        double yDist = Math.abs(pos.yCoord - eyes.yCoord);
        double zDist = Math.abs(pos.zCoord - eyes.zCoord);
        return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
    }

    public boolean inLiquid() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    public Block getBlock(double x, double y, double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        return getBlock(mc.thePlayer.posX + offsetX, mc.thePlayer.posY + offsetY, mc.thePlayer.posZ + offsetZ);
    }

    public EntityPlayer getTarget(double distance, boolean teamCheck) {
        EntityPlayer target = null;
        if (mc.theWorld == null) {
            return null;
        }

        for (EntityPlayer entity : mc.theWorld.playerEntities) {
            if (teamCheck && isInTeam(entity))
                continue;

            if (Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isEnabled() && Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).bots.contains(entity))
                continue;

            float tempDistance = mc.thePlayer.getDistanceToEntity(entity) - 0.5657f;
            if (entity != mc.thePlayer && tempDistance <= distance) {
                target = entity;
                distance = tempDistance;
            }
        }

        if (Demise.INSTANCE.getModuleManager().getModule(KillAura.class).isEnabled() && KillAura.currentTarget != null) {
            return (EntityPlayer) KillAura.currentTarget;
        }

        return target;
    }

    public Block getBlock(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public boolean isReplaceable(BlockPos blockPos) {
        return getBlock(blockPos).isReplaceable(mc.theWorld, blockPos);
    }

    private String[] healthSubstrings = {"hp", "health", "lives", "❤"};

    public Float getActualHealth(EntityLivingBase entity) {
        Scoreboard scoreboard = entity.getEntityWorld().getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);

        if (objective == null) {
            return entity.getHealth();
        }

        Score score = scoreboard.getValueFromObjective(entity.getName(), objective);

        if (score == null || objective.getDisplayName() == null) {
            return entity.getHealth();
        }

        String displayName = objective.getDisplayName();

        boolean containsHealthSubstring = false;
        for (String substring : healthSubstrings) {
            if (displayName.contains(substring)) {
                containsHealthSubstring = true;
                break;
            }
        }

        if (!containsHealthSubstring) {
            return entity.getHealth();
        }

        int scoreboardHealth = score.getScorePoints();

        if (scoreboardHealth > 0) {
            return (float) scoreboardHealth;
        }

        return entity.getHealth();
    }

    public class PredictProcess {
        public Vec3 position;
        public float fallDistance;
        public boolean onGround;
        public boolean isCollidedHorizontally;
        public int tick;
        public boolean isInWater;

        public PredictProcess(Vec3 position, float fallDistance, boolean onGround, boolean isCollidedHorizontally) {
            this.position = position;
            this.fallDistance = fallDistance;
            this.onGround = onGround;
            this.isCollidedHorizontally = isCollidedHorizontally;
            this.isInWater = false;
        }

        public PredictProcess(Vec3 position, float fallDistance, boolean onGround, boolean isCollidedHorizontally, boolean inWater) {
            this.position = position;
            this.fallDistance = fallDistance;
            this.onGround = onGround;
            this.isCollidedHorizontally = isCollidedHorizontally;
            this.isInWater = inWater;
        }
    }

    public boolean insideBlock() {
        if (mc.thePlayer.ticksExisted < 5) {
            return false;
        }

        EntityPlayerSP player = mc.thePlayer;
        WorldClient world = mc.theWorld;
        AxisAlignedBB bb = player.getEntityBoundingBox();
        for (int x = MathHelper.floor_double(bb.minX); x < MathHelper.floor_double(bb.maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(bb.minY); y < MathHelper.floor_double(bb.maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(bb.minZ); z < MathHelper.floor_double(bb.maxZ) + 1; ++z) {
                    Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    AxisAlignedBB boundingBox;
                    if (block != null && !(block instanceof BlockAir) && (boundingBox = block.getCollisionBoundingBox(world, new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)))) != null && player.getEntityBoundingBox().intersectsWith(boundingBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getCurrServer() {
        String srv;

        if (!mc.isSingleplayer()) {
            if (mc.getCurrentServerData().serverIP.toLowerCase().contains("liquidproxy.net")) {
                srv = "liquidproxy.net";
            } else {
                srv = mc.getCurrentServerData().serverIP;
            }
        } else {
            srv = "Singleplayer";
        }

        return srv;
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public double getDistToTargetFromMouseOver(Entity target) {
        return getDistToTargetFromMouseOver(mc.thePlayer.getPositionEyes(1), mc.thePlayer.getLook(1), target, target.getHitbox());
    }

    public double getDistToTargetFromMouseOver(Vec3 playerPos, Vec3 look, Entity target, AxisAlignedBB targetBB) {
        double blockReachDistance = 64;
        Vec3 vec32 = playerPos.addVector(look.xCoord * blockReachDistance, look.yCoord * blockReachDistance, look.zCoord * blockReachDistance);

        Vec3 vec33 = null;

        MovingObjectPosition movingobjectposition = targetBB.calculateIntercept(playerPos, vec32);

        if (targetBB.isVecInside(playerPos)) {
            vec33 = movingobjectposition == null ? playerPos : movingobjectposition.hitVec;
        } else if (movingobjectposition != null) {
            double d3 = playerPos.distanceTo(movingobjectposition.hitVec);

            if (d3 < blockReachDistance) {
                boolean flag1 = false;

                if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                    flag1 = Reflector.callBoolean(target, Reflector.ForgeEntity_canRiderInteract);
                }

                if (!(!flag1 && target == mc.thePlayer.ridingEntity)) {
                    vec33 = movingobjectposition.hitVec;
                }
            }

        }

        return vec33 == null ? Double.MAX_VALUE : playerPos.distanceTo(vec33);
    }

    public Vec3 getPosFromAABB(AxisAlignedBB a) {
        return new Vec3((a.minX + a.maxX) / 2, a.minY, (a.minZ + a.maxZ) / 2);
    }

    public boolean isOnLiquid() {
        boolean onLiquid = false;
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        WorldClient world = mc.theWorld;
        int y = (int) playerBB.offset(0.0, -0.01, 0.0).minY;
        for (int x = MathHelper.floor_double(playerBB.minX); x < MathHelper.floor_double(playerBB.maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(playerBB.minZ); z < MathHelper.floor_double(playerBB.maxZ) + 1; ++z) {
                Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block != null && !(block instanceof BlockAir)) {
                    if (!(block instanceof BlockLiquid)) {
                        return false;
                    }
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }

    public PredictProcess predictPlayerPosition(int ticks) {
        List<PredictProcess> selfPrediction = new ArrayList<>();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput, 1);

        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < ticks; i++) {
            simulatedSelf.tick();

            PlayerUtils.PredictProcess predictProcess = new PlayerUtils.PredictProcess(
                    simulatedSelf.getPos(),
                    simulatedSelf.fallDistance,
                    simulatedSelf.onGround,
                    simulatedSelf.isCollidedHorizontally
            );

            predictProcess.tick = i;

            selfPrediction.add(predictProcess);
        }

        return selfPrediction.get(selfPrediction.size() - 1);
    }
}