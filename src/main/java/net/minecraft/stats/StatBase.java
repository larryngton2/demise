package net.minecraft.stats;

import net.minecraft.event.HoverEvent;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IJsonSerializable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class StatBase {
    public final String statId;
    private final IChatComponent statName;
    public boolean isIndependent;
    private final IStatType type;
    private final IScoreObjectiveCriteria objectiveCriteria;
    private Class<? extends IJsonSerializable> field_150956_d;
    private static final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
    public static final IStatType simpleStatType = StatBase.numberFormat::format;
    private static final DecimalFormat decimalFormat = new DecimalFormat("########0.00");
    public static final IStatType timeStatType = number -> {
        double d0 = (double) number / 20.0D;
        double d1 = d0 / 60.0D;
        double d2 = d1 / 60.0D;
        double d3 = d2 / 24.0D;
        double d4 = d3 / 365.0D;
        return d4 > 0.5D ? StatBase.decimalFormat.format(d4) + " y" : (d3 > 0.5D ? StatBase.decimalFormat.format(d3) + " d" : (d2 > 0.5D ? StatBase.decimalFormat.format(d2) + " h" : (d1 > 0.5D ? StatBase.decimalFormat.format(d1) + " m" : d0 + " s")));
    };
    public static final IStatType distanceStatType = number -> {
        double d0 = (double) number / 100.0D;
        double d1 = d0 / 1000.0D;
        return d1 > 0.5D ? StatBase.decimalFormat.format(d1) + " km" : (d0 > 0.5D ? StatBase.decimalFormat.format(d0) + " m" : number + " cm");
    };
    public static final IStatType field_111202_k = number -> StatBase.decimalFormat.format((double) number * 0.1D);

    public StatBase(String statIdIn, IChatComponent statNameIn, IStatType typeIn) {
        this.statId = statIdIn;
        this.statName = statNameIn;
        this.type = typeIn;
        this.objectiveCriteria = new ObjectiveStat(this);
        IScoreObjectiveCriteria.INSTANCES.put(this.objectiveCriteria.getName(), this.objectiveCriteria);
    }

    public StatBase(String statIdIn, IChatComponent statNameIn) {
        this(statIdIn, statNameIn, simpleStatType);
    }

    public StatBase initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public StatBase registerStat() {
        if (StatList.oneShotStats.containsKey(this.statId)) {
            throw new RuntimeException("Duplicate stat id: \"" + StatList.oneShotStats.get(this.statId).statName + "\" and \"" + this.statName + "\" at id " + this.statId);
        } else {
            StatList.allStats.add(this);
            StatList.oneShotStats.put(this.statId, this);
            return this;
        }
    }

    public boolean isAchievement() {
        return false;
    }

    public String format(int p_75968_1_) {
        return this.type.format(p_75968_1_);
    }

    public IChatComponent getStatName() {
        IChatComponent ichatcomponent = this.statName.createCopy();
        ichatcomponent.getChatStyle().setColor(EnumChatFormatting.GRAY);
        ichatcomponent.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ACHIEVEMENT, new ChatComponentText(this.statId)));
        return ichatcomponent;
    }

    public IChatComponent createChatComponent() {
        IChatComponent ichatcomponent = this.getStatName();
        IChatComponent ichatcomponent1 = (new ChatComponentText("[")).appendSibling(ichatcomponent).appendText("]");
        ichatcomponent1.setChatStyle(ichatcomponent.getChatStyle());
        return ichatcomponent1;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
            StatBase statbase = (StatBase) p_equals_1_;
            return this.statId.equals(statbase.statId);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.statId.hashCode();
    }

    public String toString() {
        return "Stat{id=" + this.statId + ", nameId=" + this.statName + ", awardLocallyOnly=" + this.isIndependent + ", formatter=" + this.type + ", objectiveCriteria=" + this.objectiveCriteria + '}';
    }

    public IScoreObjectiveCriteria getCriteria() {
        return this.objectiveCriteria;
    }

    public Class<? extends IJsonSerializable> func_150954_l() {
        return this.field_150956_d;
    }

    public StatBase func_150953_b(Class<? extends IJsonSerializable> p_150953_1_) {
        this.field_150956_d = p_150953_1_;
        return this;
    }
}
