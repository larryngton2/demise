package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Gambling", description = "I LOVE GAMBLING :money_mouth:")
public class Gambling extends Module {
    private final SliderValue betAmount = new SliderValue("Bet Amount", 100, 1, 1000, 1, this);
    private final BoolValue bet = new BoolValue("Bet (this is a button btw)", false, this);
    private double money = 1000;
    private int losses = 0;

    @EventTarget
    public void onUpdate(GameEvent e) {
        if (losses > 10) {
            setEnabled(false);
            return;
        }

        double bet = betAmount.get();
        if (bet > money) {
            setEnabled(false);
            return;
        }

        if (this.bet.get()) {
            if (Math.random() < 0.45) {
                money += bet;
                losses = 0;
            } else {
                money -= bet;
                losses++;
            }

            this.bet.set(false);
        }

        setTag(String.format("$%.2f", money));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        money = 1000;
        losses = 0;
        setTag("");
    }
}