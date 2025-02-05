package wtf.demise.features.modules.impl.combat;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;

import java.util.*;

@ModuleInfo(name = "Velocity", category = ModuleCategory.Combat)
public class Velocity extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Cancel", "IntaveReduce", "JumpReset", "Legit"}, "Normal", this);
    private final SliderValue horizontal = new SliderValue("Horizontal", 0, 0, 100, 1, this, () -> Objects.equals(mode.get(), "Normal") || Objects.equals(mode.get(), "Cancel"));
    private final SliderValue vertical = new SliderValue("Vertical", 100, 0, 100, 1, this, () -> Objects.equals(mode.get(), "Normal") || Objects.equals(mode.get(), "Cancel"));
    private final SliderValue chance = new SliderValue("Chance", 100, 0, 100, 1, this);
    private final SliderValue intaveHurtTime = new SliderValue("Intave HurtTime", 9, 1, 10, 1, this, () -> mode.is("IntaveReduce"));
    private final SliderValue intaveFactor = new SliderValue("Intave Factor", 0.6f, 0, 1, 0.05f, this, () -> mode.is("IntaveReduce"));

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (PlayerUtils.nullCheck() && mc.thePlayer.maxHurtTime > 0) {
            if (chance.get() != 100.0D) {
                if (new Random().nextInt(100) <= chance.get()) {
                    return;
                }
            }

            switch (mode.get()) {
                case "Normal":
                    if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
                        if (horizontal.get() != 100.0D) {
                            mc.thePlayer.motionX *= horizontal.get() / 100.0D;
                            mc.thePlayer.motionZ *= horizontal.get() / 100.0D;
                        }

                        if (vertical.get() != 100.0D) {
                            mc.thePlayer.motionY *= vertical.get() / 100.0D;
                        }
                    }
                    break;
                case "JumpReset":
                    if (mc.thePlayer.onGround && mc.thePlayer.hurtTime > 5) {
                        mc.thePlayer.jump();
                    }
                    break;
            }
        }

        this.setTag(mode.get());
    }

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        if (Objects.equals(mode.get(), "Cancel") && e.getPacket() instanceof S12PacketEntityVelocity) {
            Packet<?> packet = e.getPacket();
            S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
            if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();
                if (horizontal.get() != 100.0D) {
                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.get() / 100.0;
                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.get() / 100.0;
                }

                if (vertical.get() != 100.0D) {
                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.get() / 100.0;
                }

                e.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mode.is("Legit") && KillAura.currentTarget != null && mc.thePlayer.hurtTime > 0) {
            ArrayList<Vec3> vec3s = new ArrayList<>();
            HashMap<Vec3, Integer> map = new HashMap<>();
            Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            Vec3 onlyForward = PlayerUtils.getPredictedPos(1.0F, 0.0F).add(playerPos);
            Vec3 strafeLeft = PlayerUtils.getPredictedPos(1.0F, 1.0F).add(playerPos);
            Vec3 strafeRight = PlayerUtils.getPredictedPos(1.0F, -1.0F).add(playerPos);
            map.put(onlyForward, 0);
            map.put(strafeLeft, 1);
            map.put(strafeRight, -1);
            vec3s.add(onlyForward);
            vec3s.add(strafeLeft);
            vec3s.add(strafeRight);
            Vec3 targetVec = new Vec3(KillAura.currentTarget.posX, KillAura.currentTarget.posY, KillAura.currentTarget.posZ);
            vec3s.sort(Comparator.comparingDouble(targetVec::distanceXZTo));
            if (!mc.thePlayer.movementInput.sneak) {
                System.out.println(map.get(vec3s.get(0)));
                mc.thePlayer.movementInput.moveStrafe = map.get(vec3s.get(0));
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (mode.is("IntaveReduce")) {
            if (mc.thePlayer.hurtTime == intaveHurtTime.get()) {
                mc.thePlayer.motionX *= intaveFactor.get();
                mc.thePlayer.motionZ *= intaveFactor.get();
            }
        }
    }
}