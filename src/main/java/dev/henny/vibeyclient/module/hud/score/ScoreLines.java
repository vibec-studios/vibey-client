package dev.henny.vibeyclient.module.hud.score;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ScoreLines {
    static final int MAX = 15;
    private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparing(PlayerScoreEntry::value).reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    Component title;
    final List<Component> names = new ArrayList<>();
    final List<Component> scores = new ArrayList<>();

    boolean read() {
        names.clear();
        scores.clear();
        Objective o = objective();
        if (o == null) return false;
        Scoreboard sb = o.getScoreboard();
        title = o.getDisplayName();
        List<PlayerScoreEntry> list = new ArrayList<>();
        for (PlayerScoreEntry e : sb.listPlayerScores(o)) if (!e.isHidden()) list.add(e);
        list.sort(ORDER);
        for (int i = 0; i < Math.min(MAX, list.size()); i++) {
            PlayerScoreEntry e = list.get(i);
            names.add(PlayerTeam.formatNameForTeam(sb.getPlayersTeam(e.owner()), e.ownerName()));
            scores.add(e.formatValue(o.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)));
        }
        return true;
    }

    void sample() {
        names.clear();
        scores.clear();
        title = Component.literal("Vibey").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        String[] n = {"Kills", "Deaths", "Wins", "Coins"};
        int[] v = {12, 3, 4, 250};
        for (int i = 0; i < n.length; i++) {
            names.add(Component.literal(n[i]));
            scores.add(Component.literal(Integer.toString(v[i])).withStyle(ChatFormatting.RED));
        }
    }

    private static Objective objective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        Scoreboard sb = mc.level.getScoreboard();
        PlayerTeam team = sb.getPlayersTeam(mc.player.getScoreboardName());
        if (team != null) {
            //? if >=26.2 {
            /*DisplaySlot slot = team.getColor().map(net.minecraft.world.scores.TeamColor::displaySlot).orElse(null);
            *///?} else
            DisplaySlot slot = DisplaySlot.teamColorToSlot(team.getColor());
            Objective o = slot == null ? null : sb.getDisplayObjective(slot);
            if (o != null) return o;
        }
        return sb.getDisplayObjective(DisplaySlot.SIDEBAR);
    }
}
