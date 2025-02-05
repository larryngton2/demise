package wtf.demise.features.command;

public abstract class Command {
    public abstract String[] getAliases();

    public abstract void execute(final String[] p0) throws CommandExecutionException;

    public abstract String getUsage();
}
