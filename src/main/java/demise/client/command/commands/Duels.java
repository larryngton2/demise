package demise.client.command.commands;


import demise.client.clickgui.demise.Terminal;
import demise.client.command.Command;
import demise.client.main.demise;
import demise.client.utils.Utils;
import demise.client.utils.profile.PlayerProfile;

public class Duels extends Command {
    public Duels()  {
        super("duels", "Fetches a player's stats", 1, 2,  new String[] {"Player name", "overall/uhc/bridge/skywars/sumo/classic/op"},  new String[] {"d", "duel", "stat", "stats", "check"});
    }

    @Override
    public void onCall(String[] args) {
        if (Utils.URLS.hypixelApiKey.isEmpty()) {
            Terminal.print("API Key is empty! Run \"setkey api_key\".");
            return;
        }
        if(args.length == 0) {
            this.incorrectArgs();
            return;
        }

        if(args.length == 1){
            String n;
            n = args[0];
            Terminal.print("Retrieving data...");
            demise.getExecutor().execute(() -> {
                PlayerProfile playerProfile = new PlayerProfile(n, Utils.Profiles.DuelsStatsMode.OVERALL);
                playerProfile.populateStats();
                if(!playerProfile.isPlayer){
                    Terminal.print(n + " does not exist");
                } else if (playerProfile.nicked) {
                    Terminal.print(n + " is nicked");
                } else {
                    double wlr = playerProfile.losses != 0 ? Utils.Java.round((double)playerProfile.wins / (double)playerProfile.losses, 2) : (double)playerProfile.wins;
                    Terminal.print(n + " overall stats:");
                    Terminal.print("Wins: " + playerProfile.wins);
                    Terminal.print("Losses: " + playerProfile.losses);
                    Terminal.print("WLR: " + wlr);
                    Terminal.print("Winstreak: " + playerProfile.winStreak);
                }
            });
        } else if (args.length == 2) {
            String stringGamemode = args[1];
            Utils.Profiles.DuelsStatsMode gameMode = null;
            for(Utils.Profiles.DuelsStatsMode mode : Utils.Profiles.DuelsStatsMode.values()){
                if(String.valueOf(mode).equalsIgnoreCase(stringGamemode))
                    gameMode = mode;
            }

            if(gameMode == null){
                Terminal.print(stringGamemode + " is not a known gamemode. See \"help duels\" for a known list of gamemode");
            } else {
                String n;
                n = args[0];
                Terminal.print("Retrieving data...");
                Utils.Profiles.DuelsStatsMode finalGameMode = gameMode;
                demise.getExecutor().execute(() -> {
                    PlayerProfile playerProfile = new PlayerProfile(n, finalGameMode);
                    playerProfile.populateStats();
                    if(!playerProfile.isPlayer){
                        Terminal.print(n + " does not exist");
                    } else if (playerProfile.nicked) {
                        Terminal.print(n + " is nicked");
                    } else {
                        double wlr = playerProfile.losses != 0 ? Utils.Java.round((double)playerProfile.wins / (double)playerProfile.losses, 2) : (double)playerProfile.wins;
                        Terminal.print(n + " " + finalGameMode + " stats:");
                        Terminal.print("Wins: " + playerProfile.wins);
                        Terminal.print("Losses: " + playerProfile.losses);
                        Terminal.print("WLR: " + wlr);
                        Terminal.print("Winstreak: " + playerProfile.winStreak);
                    }
                });
            }
        }
    }
}
