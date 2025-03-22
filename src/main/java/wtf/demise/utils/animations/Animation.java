package wtf.demise.utils.animations;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.utils.math.TimerUtils;

public abstract class Animation {
    public TimerUtils timerUtils = new TimerUtils();
    @Setter
    protected int duration;
    @Getter
    @Setter
    protected double endPoint;
    @Getter
    protected Direction direction;

    public Animation(int ms, double endPoint) {
        this(ms, endPoint, Direction.FORWARDS);
    }

    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms; //Time in milliseconds of how long you want the animation to take.
        this.endPoint = endPoint; //The desired distance for the animated object to go.
        this.direction = direction; //Direction in which the graph is going. If backwards, will start from endPoint and go to 0.
    }

    public boolean finished(Direction direction) {
        return isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1 - ((timerUtils.getTime() / (double) duration) * endPoint);
    }

    public void reset() {
        timerUtils.reset();
    }

    public boolean isDone() {
        return timerUtils.hasTimeElapsed(duration);
    }

    public void changeDirection() {
        setDirection(direction.opposite());
    }

    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            timerUtils.setTime(System.currentTimeMillis() - (duration - Math.min(duration, timerUtils.getTime())));
        }
        return this;
    }

    protected boolean correctOutput() {
        return false;
    }

    public double getOutput() {
        if (direction.forwards()) {
            if (isDone()) {
                return endPoint;
            }

            return getEquation(timerUtils.getTime() / (double) duration) * endPoint;
        } else {
            if (isDone()) {
                return 0.0;
            }

            if (correctOutput()) {
                double revTime = Math.min(duration, Math.max(0, duration - timerUtils.getTime()));
                return getEquation(revTime / duration) * endPoint;
            }

            return (1 - getEquation(timerUtils.getTime() / (double) duration)) * endPoint;
        }
    }

    //This is where the animation equation should go, for example, a logistic function. Output should range from 0 - 1.
    //This will take the timer's time as an input, x.
    protected abstract double getEquation(double x);
}