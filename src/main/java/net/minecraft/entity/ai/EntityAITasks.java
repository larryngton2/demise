package net.minecraft.entity.ai;

import com.google.common.collect.Lists;
import net.minecraft.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.List;

public class EntityAITasks {
    private static final Logger logger = LogManager.getLogger();
    private final List<EntityAITasks.EntityAITaskEntry> taskEntries = Lists.newArrayList();
    private final List<EntityAITasks.EntityAITaskEntry> executingTaskEntries = Lists.newArrayList();
    private final Profiler theProfiler;
    private int tickCount;
    private final int tickRate = 3;

    public EntityAITasks(Profiler profilerIn) {
        this.theProfiler = profilerIn;
    }

    public void addTask(int priority, EntityAIBase task) {
        this.taskEntries.add(new EntityAITaskEntry(priority, task));
    }

    public void removeTask(EntityAIBase task) {
        Iterator<EntityAITasks.EntityAITaskEntry> iterator = this.taskEntries.iterator();

        while (iterator.hasNext()) {
            EntityAITasks.EntityAITaskEntry entityaitasks$entityaitaskentry = iterator.next();
            EntityAIBase entityaibase = entityaitasks$entityaitaskentry.action;

            if (entityaibase == task) {
                if (this.executingTaskEntries.contains(entityaitasks$entityaitaskentry)) {
                    entityaibase.resetTask();
                    this.executingTaskEntries.remove(entityaitasks$entityaitaskentry);
                }

                iterator.remove();
            }
        }
    }

    public void onUpdateTasks() {
        this.theProfiler.startSection("goalSetup");

        if (this.tickCount++ % this.tickRate == 0) {
            Iterator iterator = this.taskEntries.iterator();
            label38:

            while (true) {
                EntityAITasks.EntityAITaskEntry entityaitasks$entityaitaskentry;

                while (true) {
                    if (!iterator.hasNext()) {
                        break label38;
                    }

                    entityaitasks$entityaitaskentry = (EntityAITasks.EntityAITaskEntry) iterator.next();
                    boolean flag = this.executingTaskEntries.contains(entityaitasks$entityaitaskentry);

                    if (!flag) {
                        break;
                    }

                    if (!this.canUse(entityaitasks$entityaitaskentry) || !this.canContinue(entityaitasks$entityaitaskentry)) {
                        entityaitasks$entityaitaskentry.action.resetTask();
                        this.executingTaskEntries.remove(entityaitasks$entityaitaskentry);
                        break;
                    }
                }

                if (this.canUse(entityaitasks$entityaitaskentry) && entityaitasks$entityaitaskentry.action.shouldExecute()) {
                    entityaitasks$entityaitaskentry.action.startExecuting();
                    this.executingTaskEntries.add(entityaitasks$entityaitaskentry);
                }
            }
        } else {
            Iterator<EntityAITasks.EntityAITaskEntry> iterator1 = this.executingTaskEntries.iterator();

            while (iterator1.hasNext()) {
                EntityAITasks.EntityAITaskEntry entityaitasks$entityaitaskentry1 = iterator1.next();

                if (!this.canContinue(entityaitasks$entityaitaskentry1)) {
                    entityaitasks$entityaitaskentry1.action.resetTask();
                    iterator1.remove();
                }
            }
        }

        this.theProfiler.endSection();
        this.theProfiler.startSection("goalTick");

        for (EntityAITasks.EntityAITaskEntry entityaitasks$entityaitaskentry2 : this.executingTaskEntries) {
            entityaitasks$entityaitaskentry2.action.updateTask();
        }

        this.theProfiler.endSection();
    }

    private boolean canContinue(EntityAITasks.EntityAITaskEntry taskEntry) {
        return taskEntry.action.continueExecuting();
    }

    private boolean canUse(EntityAITasks.EntityAITaskEntry taskEntry) {
        for (EntityAITasks.EntityAITaskEntry entityaitasks$entityaitaskentry : this.taskEntries) {
            if (entityaitasks$entityaitaskentry != taskEntry) {
                if (taskEntry.priority >= entityaitasks$entityaitaskentry.priority) {
                    if (!this.areTasksCompatible(taskEntry, entityaitasks$entityaitaskentry) && this.executingTaskEntries.contains(entityaitasks$entityaitaskentry)) {
                        return false;
                    }
                } else if (!entityaitasks$entityaitaskentry.action.isInterruptible() && this.executingTaskEntries.contains(entityaitasks$entityaitaskentry)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean areTasksCompatible(EntityAITasks.EntityAITaskEntry taskEntry1, EntityAITasks.EntityAITaskEntry taskEntry2) {
        return (taskEntry1.action.getMutexBits() & taskEntry2.action.getMutexBits()) == 0;
    }

    static class EntityAITaskEntry {
        public final EntityAIBase action;
        public final int priority;

        public EntityAITaskEntry(int priorityIn, EntityAIBase task) {
            this.priority = priorityIn;
            this.action = task;
        }
    }
}
