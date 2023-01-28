package me.crazycranberry.headhunterplugin.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ScoreboardWrapper is a class that wraps Bukkit Scoreboard API
 * and makes your life easier.
 * Thank you to https://www.spigotmc.org/members/johnnykpl.74653/
 */
public class ScoreboardWrapper {

    public static final int MAX_LINES = 16;

    private final Scoreboard scoreboard;
    private final Objective objective;

    private final List<String> modifies = new ArrayList<>(MAX_LINES);

    /**
     * Grab the main Scoreboard with a default title.
     */
    public ScoreboardWrapper(String title) {
        scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Objective objectiveTmp = scoreboard.getObjective(title);
        if (objectiveTmp == null) {
            objectiveTmp = scoreboard.registerNewObjective(title, Criteria.DUMMY, title);
        }
        objective = objectiveTmp;
        objective.setDisplayName(title);
        objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
    }

    /**
     * Sets the scoreboard title.
     */
    public void setTitle(String title) {
        objective.setDisplayName(title);
    }

    /**
     * Modifies the line with §r strings in the way to add
     * a line equal to another.
     */
    private String getLineCoded(String line) {
        String result = line;
        while (modifies.contains(result))
            result += ChatColor.RESET;
        return result.substring(0, Math.min(40, result.length()));
    }

    public void updateScore(Player player, YamlConfiguration headLog) {
        ConfigurationSection cs = headLog.getConfigurationSection(player.getDisplayName());
        if (cs != null) {
            objective.getScore(player.getDisplayName()).setScore(cs.getKeys(false).size());
        }
    }

    /**
     * Adds a new line to the scoreboard. Throw an error if the lines count are higher than 16.
     */
    public void addLine(String line) {
        if (modifies.size() > MAX_LINES)
            throw new IndexOutOfBoundsException("You cannot add more than 16 lines.");
        String modified = getLineCoded(line);
        modifies.add(modified);
        objective.getScore(modified).setScore(-(modifies.size()));
    }

    /**
     * Adds a blank space to the scoreboard.
     */
    public void addBlankSpace() {
        addLine(" ");
    }

    /**
     * Sets a scoreboard line to an exact index (between 0 and 15).
     */
    public void setLine(int index, String line) {
        if (index < 0 || index >= MAX_LINES)
            throw new IndexOutOfBoundsException("The index cannot be negative or higher than 15.");
        String oldModified = modifies.get(index);
        scoreboard.resetScores(oldModified);
        String modified = getLineCoded(line);
        modifies.set(index, modified);
        objective.getScore(modified).setScore(-(index + 1));
    }

    /**
     * Gets the Bukkit Scoreboard.
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Just for debug.
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        int i = 0;
        for (String string : modifies)
            out.append(-(i + 1)).append(")-> ").append(string).append(";\n");
        return out.toString();
    }
}
