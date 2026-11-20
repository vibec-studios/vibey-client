package dev.henny.vibeyclient.ui.vui.corner;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vui.bind.State;

import java.util.List;

public abstract class CornerController {
    public final State<String> focus = new State<>("");
    public final State<Integer> x = new State<>(0);
    public final State<Integer> y = new State<>(0);

    public abstract List<CornerEntry> all();

    public void activate(CornerEntry e) {
        e.entry().action().accept(ClientCompat.screen());
    }
}
