package me.dartanman.duels.game;

import me.dartanman.duels.Duels;
import me.dartanman.duels.game.arenas.Arena;
import me.dartanman.duels.stats.db.StatisticsDatabase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.crafteconomy.blockchain.CraftBlockchainPlugin;
import com.crafteconomy.blockchain.api.IntegrationAPI;
import com.crafteconomy.blockchain.core.types.FaucetTypes;

import java.util.UUID;

public class Game
{
    private final Arena arena;

    public Game(Arena arena)
    {
        this.arena = arena;
    }

    public void start()
    {
        arena.setGameState(GameState.PLAYING);

        Player playerOne = Bukkit.getPlayer(arena.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(arena.getPlayerTwo());

        assert playerOne != null;
        assert playerTwo != null;

        playerOne.teleport(arena.getSpawnOne());
        playerTwo.teleport(arena.getSpawnTwo());

        applyKits();
    }

    private void applyKits()
    {
        Player playerOne = Bukkit.getPlayer(arena.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(arena.getPlayerTwo());

        assert playerOne != null;
        assert playerTwo != null;

        arena.getKitManager().giveKit(playerOne);
        arena.getKitManager().giveKit(playerTwo);
    }

    public void kill(Player player)
    {
        Player playerOne = Bukkit.getPlayer(arena.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(arena.getPlayerTwo());

        assert playerOne != null;
        assert playerTwo != null;

        StatisticsDatabase db = arena.getStatisticsDatabase();

        Player winner, loser;
        if(playerOne.getUniqueId().equals(player.getUniqueId())) {
            winner = playerTwo;
            loser = playerOne;
        } else {            
            winner = playerOne;
            loser = playerTwo;   
        }
        long uTokenWinAmount = arena.getuTokenBetAmount() * 2;
        announceWinner(db, winner, loser, uTokenWinAmount);
        
        int healthLeft = (int) winner.getHealth();

        // run command from servertools to announce        
        String cmd = "announce winner " + Arena.getPlayerName(winner.getUniqueId()) + " " + Arena.getPlayerName(loser.getUniqueId()) + " " + (uTokenWinAmount/1_000_000) + " " + healthLeft;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        

        arena.reset();
    }

    public void announceWinner(StatisticsDatabase db, Player winner, Player loser, long uTokenWinAmount) {
        IntegrationAPI api = CraftBlockchainPlugin.getAPI();

        UUID winnerUUID = winner.getUniqueId();
        UUID loserUUID = loser.getUniqueId();        

        db.setWins(winnerUUID, db.getWins(winnerUUID) + 1);
        db.setLosses(loserUUID, db.getLosses(loserUUID) + 1);        
        

        // Faucet funds to winner
        api.faucetUCraft(api.getWallet(winnerUUID), "You were got 2 TOKENS for winning.", uTokenWinAmount).thenAccept((tx) -> {
            winner.sendMessage("You got " +uTokenWinAmount/1_000_000 + " " + api.getTokenName().toUpperCase() + " for winning. You should see this in your account within 15 seconds");
        });

        // TODO: Query redis pubsub for array of paid/interacted accounts (do via API)

    }
}
