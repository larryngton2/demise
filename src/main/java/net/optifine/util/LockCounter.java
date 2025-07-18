package net.optifine.util;

public class LockCounter {
    private int lockCount;

    public void lock() {
        ++this.lockCount;
    }

    public boolean unlock() {
        if (this.lockCount <= 0) {
            return false;
        } else {
            --this.lockCount;
            return this.lockCount == 0;
        }
    }

    public boolean isLocked() {
        return this.lockCount > 0;
    }

    public int getLockCount() {
        return this.lockCount;
    }

    public String toString() {
        return "lockCount: " + this.lockCount;
    }
}
