package wtf.demise.features.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.math.MathUtils;

@ModuleInfo(name = "KillInsults", description = "Insults people when you kill them.", category = ModuleCategory.Misc)
public class KillInsults extends Module {
    // I can't be bothered writing shit like this
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Rise 5", "Sigma"}, "Rise 5", this);

    private EntityPlayer target;

    @EventTarget
    public void onAttackEvent(AttackEvent e) {
        final Entity entity = e.getTargetEntity();

        if (entity instanceof EntityPlayer) target = (EntityPlayer) entity;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (target != null) {
            if (!mc.theWorld.playerEntities.contains(target)) {
                if (mc.thePlayer.ticksExisted > 20) {
                    switch (mode.get()) {
                        case "Rise 5":
                            mc.thePlayer.sendChatMessage(insults[MathUtils.nextInt(0, insults.length)].replaceAll(":user:", target.getName()));
                            break;
                        case "Sigma":
                            mc.thePlayer.sendChatMessage(sigma[MathUtils.nextInt(0, sigma.length)].replaceAll(":user:", target.getName()));
                            break;
                    }
                }
                target = null;
            }
        }
    }

    private final String[] insults = {
            "Did :user: pay for that loss?",
            "Did :user:'s dad not come back after he wanted to buy some milk?",
            "Are you afraid of me",
            "Why not use demise?",
            ":user: I saw you in femboy meetup, hi!",
            "Get demise, don't be like :user:",
            "Did :user: forget to left click?",
            ":user: takes up 2 seats on the bus",
            "No demise?",
            "It is impossible to miss :user: with their man boobs",
            "Come on :user:, report me to the obese staff",
            ":user: is the type to overdose on Benadryl for a Tiktok video",
            "No wonder :user: dropped out of college",
            "Here's your ticket to spectator",
            ":user: said they would never give me up and never let me down, I am sad",
            "The latest update to demise fps booster client gave me 1000 fps and regedits for better velocity",
            ":user: became transgender just to join the 50% a day later",
            "Drink hand sanitizer so we can get rid of :user:",
            "Even the MC Virgins are less virgin than :user:",
            ":user:'s free trial of life has expired",
            ":user: is socially awkward",
            "I bet :user: believes in the flat earth",
            ":user: is the reason why society is failing",
            "Pay to lose",
            "Why would I be cheating when I am recording?",
            ":user: is such a that degenerate :user: believes EQ has more value than IQ",
            "The air could've took :user: away because of how weak :user: is",
            "Even Kurt Cobain is more alive than :user: with his wounds from a shotgun and heroin in his veins",
            ":user: is breaking down more than Nirvana after Kurt Cobain's death",
            "Does :user: buy their groceries at the dollar store?",
            "Does :user: need some pvp advice?",
            "I'd smack :user:, but that would be animal abuse",
            "I don't cheat, :user: just needs to click faster",
            "Welcome to my rape dungeon! population: :user:!",
            ":user: pressed the wrong button when they installed Minecraft?",
            "If the body is 70% water than how is :user:'s body 100% salt?",
            "demise " + Demise.INSTANCE.version + " is sexier than :user:",
            "Oh, :user: is recording? Well I am too",
            ":user: is the type of person who would brute force interpolation",
            ":user: go drown in your own salt",
            ":user: is literally heavier than Overflow",
            "Excuse me :user:, I don't speak retard",
            "Hmm, the problem :user:'s having looks like a skin color issue",
            ":user: I swear I'm on Lunar Client",
            "Hey! Wise up :user:! Don't waste your time without demise",
            ":user: didn't even stand a chance",
            "If opposites attract I hope :user: finds someone who is intelligent, honest and cultured",
            "If laughter is the best medicine, :user:'s face must be curing the world",
            ":user: is the type of person to climb over a glass wall to see what's on the other side",
            "What does :user:'s IQ and their girlfriend have in common? They're both below 5."
    };

    // im aware that demise means death btw
    private final String[] sigma = {
            "Download demise to kick ass while listening to some badass music!",
            "Why demise? Cause it is the addition of pure skill and incredible intellectual abilities",
            "You have been oofed by demise oof oof",
            "I am not racist, but I only like demise users. so git gud noobs",
            "Quick Quiz: I am larryngton's son, who am I? demise",
            "Wow! My combo is demise'n!",
            "What should I choose? demise or demise?",
            "Get demise Client, its free!",
            "Bigmama and demisemama",
            "I don't hack I just demise",
            "demise client is your new home",
            "Look a divinity! He definitely must use demise!",
            "In need of a cute present for Christmas? demise is all you need!",
            "I have a good demise config, don't blame me",
            "Don't piss me off or you will discover the true power of demise's inf reach",
            "demise never dies",
            "demise will help you! Oops, i killed you instead.",
            "#NoHaxJustdemise",
            "Order free baguettes with demise client",
            "Another demise user? Awww man",
            "demise utility client no hax 100%",
            "Hypixel wants to know demise owner's location [Accept] [Deny]",
            "I am a demise-magician, thats how I am able to do all those block game tricks",
            "Stop it, get some help! Get demise",
            "demise users belike: Hit or miss I guess I never miss!",
            "I dont hack i just have demise Gaming Chair",
            "Stop Hackustation me cuz im just demise",
            "Imagine using anything but demise",
            "No hax just beta testing the anti-cheat with demise",
            "Don't forget to report me for demise on the forums!",
            "don't use demise? ok boomer",
            "demise is better than Optifine",
            "It's not BlockFly it's Scaffold in demise!",
            "How come a noob like you not use demise?",
            "A mother becomes a true grandmother the day she gets demise " + Demise.INSTANCE.version,
            "Fly faster than light, only available in demise™",
            "Behind every demise user, is an incredibly cool human being. Trust me, cooler than you.",
            "Hello demise my old friend...",
            "#SwitchTodemise",
            "What? You've never downloaded demise? You know it's the best right?",
            "Your client sucks, just get demise",
            // this has 2 meanings...
            "demise made this world a better place, killing you with it even more"
    };
}
