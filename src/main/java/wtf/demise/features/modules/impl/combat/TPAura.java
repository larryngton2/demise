package wtf.demise.features.modules.impl.combat;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;

import java.util.*;
import java.util.function.Supplier;

@ModuleInfo(name = "TPAura", description = "For the love of God DON'T FUCKING ENABLE THIS")
public class TPAura extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Single", "Multi"}, "Single", this);
    private final SliderValue attackDelay = new SliderValue("Attack delay", 500, 1, 2000, 1, this);
    private final SliderValue maxRange = new SliderValue("Max range", 500, 500, 10, 1, this);

    private EntityLivingBase target;
    private final TimerUtils attackTimer = new TimerUtils();
    private final ObjectPool<Node> nodePool = new ObjectPool<>(Node::new);
    private final ObjectPool<Vec3> vec3Pool = new ObjectPool<>(() -> new Vec3(0, 0, 0));
    private final PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(node -> node.fCost));
    private final Long2DoubleOpenHashMap gScores = new Long2DoubleOpenHashMap();
    private final LongOpenHashSet closedSet = new LongOpenHashSet();

    private long vecToLongKey(Vec3 pos) {
        return ((long) pos.xCoord & 0x1FFFFF) << 42 | ((long) pos.yCoord & 0xFFF) << 21 | ((long) pos.zCoord & 0x1FFFFF);
    }

    private static class ObjectPool<T> {
        private final Supplier<T> creator;
        private final Queue<T> pool = new ArrayDeque<>();

        public ObjectPool(Supplier<T> creator) {
            this.creator = creator;
        }

        public T acquire() {
            return pool.isEmpty() ? creator.get() : pool.poll();
        }

        public void release(T obj) {
            pool.offer(obj);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        if (attackTimer.hasTimeElapsed(attackDelay.get())) {
            if (mode.is("Single")) {
                target = PlayerUtils.getTarget(maxRange.get());
            }

            switch (mode.get()) {
                case "Single":
                    List<Vec3> taiwanIsACountry = calculatePath(mc.thePlayer.getPositionVector(), target.getPositionVector());

                    for (Vec3 vec : taiwanIsACountry) {
                        sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(vec.xCoord, vec.yCoord, vec.zCoord, false));
                    }

                    if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        sendPacket(new C0APacketAnimation());
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                    } else {
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                        sendPacket(new C0APacketAnimation());
                    }

                    Collections.reverse(taiwanIsACountry);

                    for (Vec3 vec : taiwanIsACountry) {
                        sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(vec.xCoord, vec.yCoord, vec.zCoord, false));
                    }
                    break;
            }

            attackTimer.reset();
        }
    }

    private List<Vec3> calculatePath(Vec3 start, Vec3 goal) {
        openSet.clear();
        gScores.clear();
        closedSet.clear();

        long goalKey = vecToLongKey(goal);

        Node startNode = nodePool.acquire();
        startNode.set(start, null, 0, start.distanceTo(goal));
        openSet.add(startNode);
        gScores.put(vecToLongKey(start), 0.0);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            long currentKey = vecToLongKey(current.position);

            if (currentKey == goalKey) {
                List<Vec3> path = new ArrayList<>();
                while (current != null) {
                    path.add(current.position);
                    current = current.parent;
                }
                Collections.reverse(path);

                nodePool.release(current);
                return path;
            }

            closedSet.add(currentKey);

            for (Vec3 neighbor : getNeighbors(current.position)) {
                long neighborKey = vecToLongKey(neighbor);
                if (closedSet.contains(neighborKey)) continue;

                double tentativeG = gScores.get(currentKey) + current.position.distanceTo(neighbor);

                if (!gScores.containsKey(neighborKey) || tentativeG < gScores.get(neighborKey)) {
                    gScores.put(neighborKey, tentativeG);
                    double hCost = neighbor.distanceTo(goal);
                    Node neighborNode = nodePool.acquire();
                    neighborNode.set(neighbor, current, tentativeG, hCost);
                    openSet.add(neighborNode);
                }
            }

            nodePool.release(current);
        }

        return Collections.emptyList();
    }

    private final Vec3[] neighborOffsets = {
            new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
            new Vec3(0, 1, 0), new Vec3(0, -1, 0),
            new Vec3(0, 0, 1), new Vec3(0, 0, -1)
    };

    private List<Vec3> getNeighbors(Vec3 pos) {
        List<Vec3> neighbors = new ArrayList<>(6);
        for (Vec3 offset : neighborOffsets) {
            Vec3 neighbor = vec3Pool.acquire();
            neighbor.xCoord = pos.xCoord + offset.xCoord;
            neighbor.yCoord = pos.yCoord + offset.yCoord;
            neighbor.zCoord = pos.zCoord + offset.zCoord;

            if (isWalkable(neighbor)) {
                neighbors.add(neighbor);
            } else {
                vec3Pool.release(neighbor);
            }
        }
        return neighbors;
    }

    private boolean isWalkable(Vec3 pos) {
        BlockPos blockPos = new BlockPos(pos);
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block.isPassable(mc.theWorld, blockPos);
    }

    class Node {
        Vec3 position;
        Node parent;
        double gCost;
        double hCost;
        double fCost;

        void set(Vec3 position, Node parent, double gCost, double hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        void reset() {
            position = null;
            parent = null;
            gCost = 0;
            hCost = 0;
            fCost = 0;
        }
    }
}