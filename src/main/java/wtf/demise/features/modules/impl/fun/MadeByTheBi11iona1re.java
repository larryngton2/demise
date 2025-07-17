package wtf.demise.features.modules.impl.fun;

//        switch (autoBlock.getValue().getName()) {
//            case "Legit":
//                break;
//        }

// Import statements - bringing in Minecraft chat utilities and module/event handling classes

// For creating text components to display in chat
import net.minecraft.util.ChatComponentText;


       /*         blockTicks++;
                PingSpoofComponent.blink();

                if (blockTicks == 1) {
                    allowAttack = true;

                } else if (blockTicks >= 2) {

                    allowAttack = false;
                    this.block(false, true);
                    blockTicks = 0;
                } */

// For formatting chat text colours and styles
import net.minecraft.util.EnumChatFormatting;

//      double hypotenuse = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
//   if ((hypotenuse < MoveUtil.getAllowedHorizontalDistance()-.01  || mc.thePlayer.motionX == 0 || mc.thePlayer.motionZ == 0)) {
//      MoveUtil.strafe(MoveUtil.getAllowedHorizontalDistance()-.01);

//   }

// Annotation to mark event handler methods
import wtf.demise.events.annotations.EventTarget;

//  MoveUtil.partialStrafePercent(50);
// mc.thePlayer.motionX = (mc.thePlayer.motionX * 3 + motionX2) / 4;
//   mc.thePlayer.motionZ = (mc.thePlayer.motionZ * 3 + motionZ2) / 4;
//  MoveUtil.partialStrafePercent(3.5);
//     ChatUtil.display(Math.hypot((mc.thePlayer.motionX -(mc.thePlayer.lastTickPosX-mc.thePlayer.lastLastTickPosX)),(mc.thePlayer.motionZ-(mc.thePlayer.lastTickPosZ-mc.thePlayer.lastLastTickPosZ))));


// ChatUtil.display(disable);

// Event fired on player updates (ticks)
import wtf.demise.events.impl.player.UpdateEvent;

// MoveUtil.strafe();
// MoveUtil.partialStrafePercent(50);
//  mc.thePlayer.motionX = (mc.thePlayer.motionX * 4 + motionX2) / 6;
//  mc.thePlayer.motionZ = (mc.thePlayer.motionZ * 4 + motionZ2) / 6;

// Base class for modules
import wtf.demise.features.modules.Module;

//     ChatUtil.display(mc.thePlayer.offGroundTicks);
//   mc.thePlayer.motionY += 0.075;

//   ChatUtil.display(mc.thePlayer.offGroundTicks);

// Annotation providing metadata for modules
import wtf.demise.features.modules.ModuleInfo;

//   MoveUtil.strafe(0.37);
//    MoveUtil.strafe(0.32);
//     ChatUtil.display(mc.thePlayer.motionZ);
//   mc.thePlayer.motionY = -0.082f;

// Annotation declaring metadata about this module, including its internal name and description
@ModuleInfo(
        name = "MadeByTheBi11iona1re",        // The module name, as shown in module lists
        description = "Made by The_Bi11iona1re" // Description shown to users
)
public class MadeByTheBi11iona1re extends Module { // Our custom module class extending base Module functionality


    // Event handler annotation - marks this method as a listener for update events
    @EventTarget

    //            ChatUtil.display("MC " + mc.thePlayer.getHeldItem());
    //            ChatUtil.display("Rise " + getComponent(SlotComponent.class).getItemStack());

    public void onUpdate(UpdateEvent e) { // Method triggered every update tick for the player

        //                event.setStrafeMultiplier(0.2f);
        //                event.setForwardMultiplier(0.2f);
        //                event.setCancelled(false);
        //                event.setUseItem(true);

        // Begin of conditional check to execute code only on every 20th tick (approximately every second)
        if (mc.thePlayer.ticksExisted % 20 == 0) {

                    /*
        if (mc.thePlayer.ticksSinceVelocity == 0 && jumps < 4) {
            getModule(LongJump.class).toggle();
            NotificationComponent.post("Long Jump", "Disabled Long Jump due to damage before initial jump.", 5000);
        }

         */
            //  ChatUtil.display(jumps);

            // Access the in-game GUI's chat interface and send a formatted message to a chat window
            mc.ingameGUI.getChatGUI().printChatMessage(
                    new ChatComponentText( // Constructing a new chat message component with formatted text

                            //     ChatUtil.display("s");
                            //  mc.thePlayer.motionX *= 1.03;
                            //  mc.thePlayer.motionZ *= 1.03;

                            // Start with bold formatting applied using EnumChatFormatting.BOLD constant
                            EnumChatFormatting.BOLD +

                                    // mc.timer.timerSpeed = 0.5f;
                                    //    PacketUtil.send(new C03PacketPlayer(true));
                                    //   ChatUtil.display(jumps);

                                    // Adding an empty string here for no reason, just to make things longer ""
                                    "" +

                                    // Then add AQUA colour to text
                                    EnumChatFormatting.AQUA +

                                    //        if (target == mc.thePlayer || clickMode.getValue().getName().equals("Hit Select") && target.hurtTime > (PingComponent.getPing() / 50 + 1) && mc.thePlayer.ticksSinceVelocity > 11) {
                                    //            return;
                                    //        }
                                    //
                                    //        switchTicks++;
                                    //        if (switchTicks >= switchDelay.getRandomBetween().intValue()) {
                                    //            pastTargets.add(target);
                                    //            switchTicks = 0;
                                    //        }
                                    //
                                    //        this.attack = Math.min(Math.max(this.attack, this.attack + 2), 5);
                                    //
                                    //        if (!this.noSwing.getValue() && ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                                    //            Client.INSTANCE.getEventBus().handle(new ClickEvent());
                                    //            mc.thePlayer.swingItem();
                                    //        }
                                    //
                                    //        final AttackEvent event = new AttackEvent(target);
                                    //        Client.INSTANCE.getEventBus().handle(event);
                                    //
                                    //        if (!event.isCancelled()) {
                                    //            if (this.canBlock()) {
                                    //                this.attackBlock();
                                    //            }
                                    //
                                    //            if (this.keepSprint.getValue()) {
                                    //                mc.playerController.syncCurrentPlayItem();
                                    //
                                    //                PacketUtil.send(new C02PacketUseEntity(event.getTarget(), C02PacketUseEntity.Action.ATTACK));
                                    //
                                    //                if (mc.thePlayer.fallDistance > 0 && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null) {
                                    //                    mc.thePlayer.onCriticalHit(target);
                                    //                }
                                    //            } else {
                                    //                mc.playerController.attackEntity(mc.thePlayer, target);
                                    //            }
                                    //        }
                                    //
                                    //        if (!this.noSwing.getValue() && ViaLoadingBase.getInstance().getTargetVersion().newerThan(ProtocolVersion.v1_8)) {
                                    //            Client.INSTANCE.getEventBus().handle(new ClickEvent());
                                    //            mc.thePlayer.swingItem();
                                    //        }
                                    //

                                    // The actual visible text in the chat message
                                    "Rise" +

//                final BlockDamageEvent bdEvent = new BlockDamageEvent(this.mc.thePlayer, this.mc.thePlayer.worldObj, blockToBreak);
//                Client.INSTANCE.getEventBus().handle(bdEvent);
//                float xd = SlotUtil.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, blockToBreak, getComponent(SlotComponent.class).getItemIndex());
//                usedItem = getComponent(SlotComponent.class).getItemIndex();
//                if (!mc.thePlayer.onGround) xd *= airMultipalyer.getValue().floatValue();
//                mc.playerController.curBlockDamageMP += xd;
//                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockToBreak, (int) (mc.playerController.curBlockDamageMP * 10 - 1));
//
//                if (mc.playerController.curBlockDamageMP >= 1) {
//                    mc.thePlayer.swingItem();
//                    PacketUtil.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, blockToBreak, EnumFacing.DOWN));
//                    mc.playerController.onPlayerDestroyBlock(blockToBreak, EnumFacing.DOWN);
//                    mc.playerController.curBlockDamageMP = 0;
//                }

                                    // Reset formatting to default, removing bold but keeping colour style changes separated
                                    EnumChatFormatting.RESET +

                                    //        if (packet instanceof S3EPacketTeams) {
//            final S3EPacketTeams teams = (S3EPacketTeams) packet;
//
//            for (final String name : teams.func_149310_g()) {
//                if (Arrays.asList(staff).contains(name.toLowerCase())) {
//                    count++;
//
//                    if (count == 6) {
//                        ChatUtil.display("Staff Detected " + name);
//
//                        if (autoLeave.getValue()) PacketUtil.send(new C01PacketChatMessage("/hub"));
//                    }
//                }
//            }
//        }

                                    // Add AQUA colour again for the tears I shed writing this
                                    EnumChatFormatting.AQUA +

//        if (packet instanceof S38PacketPlayerListItem) {
//            S38PacketPlayerListItem item = ((S38PacketPlayerListItem) packet);
//
//            for (S38PacketPlayerListItem.AddPlayerData player : item.func_179767_a()) {
//                if (player == null || player.getProfile() == null || player.getProfile().getName() == null) {
//                    continue;
//                } else if (!player.getProfile().getName().equals("SHOP") && !player.getProfile().getName().equals("UPGRADES")) {
//                    ChatUtil.display("Username: " + player.getProfile().getName());
//                }
//
//                if (Arrays.asList(staff).contains(player.getProfile().getName().toLowerCase())) {
//                    ChatUtil.display("Staff Detected " + player.getProfile().getName());
//
//                    if (autoLeave.getValue()) PacketUtil.send(new C01PacketChatMessage("/hub"));
//                }
//            }
//        }

                                    // Arrow symbol to separate message parts
                                    " » " +


//        if (packet instanceof S) {
//            S06PacketUpdateHealth health = ((S06PacketUpdateHealth) packet);
//
//        }

                                    // Reset formatting again to ensure the following text is default coloured
                                    EnumChatFormatting.RESET +

                                    //        if (mc.thePlayer.fallDistance > 3 && this.placePacket == null && PlayerUtil.isBlockUnder(15)) {
//            final int slot = SlotUtil.findItem(Items.water_bucket);
//
//            if (slot == -1) {
//                return;
//            }
//
//            getComponent(SlotComponent.class).setSlot(slot);
//
//            final double minRotationSpeed = 8;
//            final double maxRotationSpeed = 10;
//            final float rotationSpeed = (float) MathUtil.getRandom(minRotationSpeed, maxRotationSpeed);
//            RotationComponent.setRotations(new Vector2f(mc.thePlayer.rotationYaw, 90), rotationSpeed, MovementFix.NORMAL);
//
//            if (RotationComponent.rotations.y > 85 && !BadPacketsComponent.bad()) {
//                for (int i = 0; i < 3; i++) {
//                    final Block block = PlayerUtil.blockRelativeToPlayer(0, -i, 0);
//
//                    if (block.getMaterial() == Material.water) {
//                        break;
//                    }
//
//                    if (block.isFullBlock()) {
//                        final BlockPos position = new BlockPos(mc.thePlayer).down(i);
//
//                        Vec3 hitVec = new Vec3(position.getX() + Math.random(), position.getY() + Math.random(), position.getZ() + Math.random());
//                        final MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(RotationComponent.rotations, mc.playerController.getBlockReachDistance());
//                        if (movingObjectPosition != null && movingObjectPosition.getBlockPos().equals(position)) {
//                            hitVec = movingObjectPosition.hitVec;
//                        }
//
//                        final float f = (float) (hitVec.xCoord - (double) position.getX());
//                        final float f1 = 1.0F;
//                        final float f2 = (float) (hitVec.zCoord - (double) position.getZ());
//
//                        PacketUtil.send(this.placePacket = new C08PacketPlayerBlockPlacement(position, EnumFacing.UP.getIndex(), getComponent(SlotComponent.class).getItemStack(), f, f1, f2));
//                        PacketUtil.send(new C08PacketPlayerBlockPlacement(getComponent(SlotComponent.class).getItemStack()));
//                        break;
//                    }
//                }
//            }
//        } else if (this.placePacket != null && mc.thePlayer.onGroundTicks > 1) {
//            int slot = SlotUtil.findItem(Items.bucket);
//
//            if (slot == -1) {
//                slot = SlotUtil.findItem(Items.water_bucket);
//            }
//
//            if (slot == -1) {
//                this.placePacket = null;
//                return;
//            }
//
//            getComponent(SlotComponent.class).setSlot(slot);
//
//            final double minRotationSpeed = 8;
//            final double maxRotationSpeed = 10;
//            final float rotationSpeed = (float) MathUtil.getRandom(minRotationSpeed, maxRotationSpeed);
//            RotationComponent.setRotations(new Vector2f(mc.thePlayer.rotationYaw, 90), rotationSpeed, MovementFix.NORMAL);
//
//            if (RotationComponent.rotations.y > 85 && !BadPacketsComponent.bad()) {
//                PacketUtil.send(this.placePacket);
//                PacketUtil.send(new C08PacketPlayerBlockPlacement(getComponent(SlotComponent.class).getItemStack()));
//            }
//
//            this.placePacket = null;
//        }

                                    // Final message text that credits "Made by The_Bi11iona1re"
                                    "Made by The_Bi11iona1re"

                            //            mc.rightClickMouse();
//            if (mc.objectMouseOver.getBlockPos().equals(new BlockPos(position))/* && getComponent(SlotComponent.class).getItemStack().getItem() == Items.water_bucket*/) {
//                MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(rotations, 4.5);
//
//                Vec3 hitVec = movingObjectPosition.hitVec;
//                BlockPos hitPos = movingObjectPosition.getBlockPos();
//
//                final float f = (float) (hitVec.xCoord - (double) hitPos.getX());
//                final float f1 = (float) (hitVec.yCoord - (double) hitPos.getY());
//                final float f2 = (float) (hitVec.zCoord - (double) hitPos.getZ());
//
//                PacketUtil.send(new C08PacketPlayerBlockPlacement(hitPos, EnumFacing.UP.getIndex(), getComponent(SlotComponent.class).getItemStack(), f, f1, f2));
//                PacketUtil.send(new C08PacketPlayerBlockPlacement(getComponent(SlotComponent.class).getItemStack()));
//                ChatUtil.display("Right Clicked");
//            } else {
//                position = null;
//            }
                    )
            ); // End of printChatMessage call
        } // End of tick check if condition
        else {
            // Logging for test purposes
            //    ChatUtil.display("Difference: " + difference + ", Invalid: " + invalid);
        }
    } // End of onUpdate event handler method
} // End of MadeByTheBi11iona1re class