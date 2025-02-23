package wtf.demise.features.modules.impl.misc;

import net.minecraft.network.play.client.C01PacketChatMessage;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.math.MathUtils;

@ModuleInfo(name = "ChatBypass", category = ModuleCategory.Misc)
public final class ChatBypass extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Hypixel"}, "Normal", this);

    private final char[] chars = new char[]{
            '͸', '͹', 'Ϳ', '΀', '΁', '΂', '΃', '΋', '΍', '΢', 'Ԥ', 'ԥ', 'Ԧ', 'ԧ', 'Ԩ', 'ԩ', 'Ԫ', 'ԫ', 'Ԭ', 'ԭ', 'Ԯ', 'ԯ', '԰', '՗', '՘', 'ՠ', 'ֈ', '֋', '֌', '֍', '֎', '֏', '֐', '׈', '׉', '׊', '׋', '׌', '׍', '׎', '׏', '׫', '׬', '׭', '׮', 'ׯ', '׵', '׶', '׷', '׸', '׹', '׺', '׻', '׼', '׽', '׾', '׿', '؄', '؅', '؜', '؝', 'ؠ', 'ٟ', '܎', '݋', '݌', '޲', '޳', '޴', '޵', '޶', '޷', '޸', '޹', '޺', '޻', '޼', '޽', '޾', '޿', '߻', '߼', '߽', '߾', '߿', 'ࠀ', 'ࠁ', 'ࠂ', 'ࠃ', 'ࠄ', 'ࠅ', 'ࠆ', 'ࠇ', 'ࠈ', 'ࠉ', 'ࠊ', 'ࠋ', 'ࠌ', 'ࠍ', 'ࠎ', 'ࠏ', 'ࠐ', 'ࠑ', 'ࠒ', 'ࠓ', 'ࠔ', 'ࠕ', 'ࠖ', 'ࠗ', '࠘', '࠙', 'ࠚ', 'ࠛ', 'ࠜ', 'ࠝ', 'ࠞ', 'ࠟ', 'ࠠ', 'ࠡ', 'ࠢ', 'ࠣ', 'ࠤ', 'ࠥ', 'ࠦ', 'ࠧ', 'ࠨ', 'ࠩ', 'ࠪ', 'ࠫ', 'ࠬ', '࠭', '࠮', '࠯', '࠰', '࠱', '࠲', '࠳', '࠴', '࠵', '࠶', '࠷', '࠸', '࠹', '࠺', '࠻', '࠼', '࠽', '࠾', '࠿', 'ࡀ', 'ࡁ', 'ࡂ', 'ࡃ', 'ࡄ', 'ࡅ', 'ࡆ', 'ࡇ', 'ࡈ', 'ࡉ', 'ࡊ', 'ࡋ', 'ࡌ', 'ࡍ', 'ࡎ', 'ࡏ', 'ࡐ', 'ࡑ', 'ࡒ', 'ࡓ', 'ࡔ', 'ࡕ', 'ࡖ', 'ࡗ', 'ࡘ', '࡙', '࡚', '࡛', '࡜', '࡝', '࡞', '࡟', 'ࡠ', 'ࡡ', 'ࡢ', 'ࡣ', 'ࡤ', 'ࡥ', 'ࡦ', 'ࡧ', 'ࡨ', 'ࡩ', 'ࡪ', '࡫', '࡬', '࡭', '࡮', '࡯', 'ࡰ', 'ࡱ', 'ࡲ', 'ࡳ', 'ࡴ', 'ࡵ', 'ࡶ', 'ࡷ', 'ࡸ', 'ࡹ', 'ࡺ', 'ࡻ', 'ࡼ', 'ࡽ', 'ࡾ', 'ࡿ', 'ࢀ', 'ࢁ', 'ࢂ', 'ࢃ', 'ࢄ', 'ࢅ', 'ࢆ', 'ࢇ', '࢈', 'ࢉ', 'ࢊ', 'ࢋ', 'ࢌ', 'ࢍ', 'ࢎ', '࢏', '࢐', '࢑', '࢒', '࢓', '࢔', '࢕', '࢖', 'ࢗ', '࢘', '࢙', '࢚', '࢛', '࢜', '࢝', '࢞', '࢟', 'ࢠ', 'ࢡ', 'ࢢ', 'ࢣ', 'ࢤ', 'ࢥ', 'ࢦ', 'ࢧ', 'ࢨ', 'ࢩ', 'ࢪ', 'ࢫ', 'ࢬ', 'ࢭ', 'ࢮ', 'ࢯ', 'ࢰ', 'ࢱ', 'ࢲ', 'ࢳ', 'ࢴ', 'ࢵ', 'ࢶ', 'ࢷ', 'ࢸ', 'ࢹ', 'ࢺ', 'ࢻ', 'ࢼ', 'ࢽ', 'ࢾ', 'ࢿ', 'ࣀ', 'ࣁ', 'ࣂ', 'ࣃ', 'ࣄ', 'ࣅ', 'ࣆ', 'ࣇ', 'ࣈ', 'ࣉ', '࣊', '࣋', '࣌', '࣍', '࣎', '࣏', '࣐', '࣑', '࣒', '࣓', 'ࣔ', 'ࣕ', 'ࣖ', 'ࣗ', 'ࣘ', 'ࣙ', 'ࣚ', 'ࣛ', 'ࣜ', 'ࣝ', 'ࣞ', 'ࣟ', '࣠', '࣡', '࣢', 'ࣣ', 'ࣤ', 'ࣥ', 'ࣦ', 'ࣧ', 'ࣨ', 'ࣩ', '࣪', '࣫', '࣬', '࣭', '࣮', '࣯', 'ࣰ', 'ࣱ', 'ࣲ', 'ࣳ', 'ࣴ', 'ࣵ', 'ࣶ', 'ࣷ', 'ࣸ', 'ࣹ', 'ࣺ', 'ࣻ', 'ࣼ', 'ࣽ', 'ࣾ', 'ࣿ', 'ऀ', 'ऺ', 'ऻ', 'ॎ', 'ॏ', 'ॕ', 'ॖ', 'ॗ', 'ॳ', 'ॴ', 'ॵ', 'ॶ', 'ॷ', 'ॸ', 'ॹ', 'ॺ', 'ঀ', '঄', '঍', '঎', '঑', '঒', '঩', '঱', '঳', '঴', '঵', '঺', '঻', '৅', '৆', '৉', '৊', '৏', '৐', '৑', '৒', '৓', '৔', '৕', '৖', '৘', '৙', '৚', '৛', '৞', '৤', '৥', '৻', 'ৼ', '৽', '৾', '৿', '਀', '਄', '਋', '਌', '਍', '਎', '਑', '਒', '਩', '਱', '਴', '਷', '਺', '਻', '਽', '੃', '੄', '੅', '੆', '੉', '੊', '੎', '੏'
    };

    @EventTarget
    public void onPacketSend(PacketEvent e) {
        if (e.getState() != PacketEvent.State.OUTGOING) return;

        if (e.getPacket() instanceof C01PacketChatMessage packet) {
            final String message = packet.getMessage();

            if (!message.startsWith("/")) {
                final StringBuilder bypass = new StringBuilder(message.length() * 2);

                for (int i = 0; i < message.length(); i++) {
                    final char character = message.charAt(i);
                    char randomChar = chars[MathUtils.randomizeInt(0, chars.length)];

                    if (mode.is("Hypixel"))
                        randomChar = 'ˌ';

                    bypass.append(character).append(randomChar);
                    bypass.append(character).append(randomChar);
                    bypass.append(character).append(randomChar);
                }

                packet.setMessage(bypass.toString());
                e.setPacket(packet);
            }
        }
    }
}