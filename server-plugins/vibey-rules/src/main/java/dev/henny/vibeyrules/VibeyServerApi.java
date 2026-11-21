package dev.henny.vibeyrules;

import dev.henny.vibeyclient.net.ServerPresentation;
import org.bukkit.entity.Player;

import java.util.List;

public interface VibeyServerApi {
    void setPresentation(Player viewer, ServerPresentation presentation);

    void setRules(Player viewer, List<String> disable, List<String> allow, String message);

    void resetRules(Player viewer);
}
