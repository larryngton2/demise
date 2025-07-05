package net.minecraft.util;

import net.minecraft.client.settings.GameSettings;
import wtf.demise.Demise;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.SneakSlowDownEvent;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        if (this.gameSettings.keyBindForward.isKeyDown()) {
            ++this.moveForward;
        }

        if (this.gameSettings.keyBindBack.isKeyDown()) {
            --this.moveForward;
        }

        if (this.gameSettings.keyBindLeft.isKeyDown()) {
            ++this.moveStrafe;
        }

        if (this.gameSettings.keyBindRight.isKeyDown()) {
            --this.moveStrafe;
        }

        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneak = this.gameSettings.keyBindSneak.isKeyDown();

        MoveInputEvent event = new MoveInputEvent(moveForward, moveStrafe, jump, sneak);
        Demise.INSTANCE.getEventManager().call(event);

        this.moveForward = event.getForward();
        this.moveStrafe = event.getStrafe();

        this.jump = event.isJumping();
        this.sneak = event.isSneaking();

        if (this.sneak) {
            SneakSlowDownEvent sneakSlowDownEvent = new SneakSlowDownEvent(0.3, 0.3);
            Demise.INSTANCE.getEventManager().call(sneakSlowDownEvent);

            this.moveStrafe = (float) ((double) this.moveStrafe * sneakSlowDownEvent.getStrafe());
            this.moveForward = (float) ((double) this.moveForward * sneakSlowDownEvent.getForward());
        }
    }
}
