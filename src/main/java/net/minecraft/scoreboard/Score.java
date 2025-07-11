package net.minecraft.scoreboard;

import net.minecraft.entity.player.EntityPlayer;

import java.util.Comparator;
import java.util.List;

public class Score {
    public static final Comparator<Score> scoreComparator = (p_compare_1_, p_compare_2_) -> p_compare_1_.getScorePoints() > p_compare_2_.getScorePoints() ? 1 : (p_compare_1_.getScorePoints() < p_compare_2_.getScorePoints() ? -1 : p_compare_2_.getPlayerName().compareToIgnoreCase(p_compare_1_.getPlayerName()));
    private final Scoreboard theScoreboard;
    private final ScoreObjective theScoreObjective;
    private final String scorePlayerName;
    private int scorePoints;
    private boolean locked;
    private boolean forceUpdate;

    public Score(Scoreboard theScoreboardIn, ScoreObjective theScoreObjectiveIn, String scorePlayerNameIn) {
        this.theScoreboard = theScoreboardIn;
        this.theScoreObjective = theScoreObjectiveIn;
        this.scorePlayerName = scorePlayerNameIn;
        this.forceUpdate = true;
    }

    public void increseScore(int amount) {
        if (this.theScoreObjective.getCriteria().isReadOnly()) {
            throw new IllegalStateException("Cannot modify read-only score");
        } else {
            this.setScorePoints(this.getScorePoints() + amount);
        }
    }

    public void decreaseScore(int amount) {
        if (this.theScoreObjective.getCriteria().isReadOnly()) {
            throw new IllegalStateException("Cannot modify read-only score");
        } else {
            this.setScorePoints(this.getScorePoints() - amount);
        }
    }

    public void func_96648_a() {
        if (this.theScoreObjective.getCriteria().isReadOnly()) {
            throw new IllegalStateException("Cannot modify read-only score");
        } else {
            this.increseScore(1);
        }
    }

    public int getScorePoints() {
        return this.scorePoints;
    }

    public void setScorePoints(int points) {
        int i = this.scorePoints;
        this.scorePoints = points;

        if (i != points || this.forceUpdate) {
            this.forceUpdate = false;
            this.getScoreScoreboard().func_96536_a(this);
        }
    }

    public ScoreObjective getObjective() {
        return this.theScoreObjective;
    }

    public String getPlayerName() {
        return this.scorePlayerName;
    }

    public Scoreboard getScoreScoreboard() {
        return this.theScoreboard;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void func_96651_a(List<EntityPlayer> p_96651_1_) {
        this.setScorePoints(this.theScoreObjective.getCriteria().setScore(p_96651_1_));
    }
}
