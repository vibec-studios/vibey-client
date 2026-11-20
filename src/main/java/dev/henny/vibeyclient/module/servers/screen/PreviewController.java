package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.core.MotdLines;
import dev.henny.vibeyclient.module.servers.core.MotdSegment;
import dev.henny.vui.bind.ListState;
import dev.henny.vui.bind.State;

import java.util.ArrayList;
import java.util.List;

public final class PreviewController {
    public record Seg(String id, String text, String color, boolean bold, boolean italic) {}

    public record Line(String id, List<Seg> segs) {}

    public final State<Boolean> open = new State<>(false);
    public final State<Float> x = new State<>(0f);
    public final State<Float> y = new State<>(0f);
    public final State<String> name = new State<>("");
    public final State<String> ping = new State<>("");
    public final State<String> players = new State<>("");
    public final State<String> version = new State<>("");
    public final State<String> sample = new State<>("");
    public final State<Boolean> bad = new State<>(false);
    public final State<Boolean> fav = new State<>(false);
    public final ListState<Line> lines = new ListState<>();

    void hide() {
        if (open.peek()) open.set(false);
    }

    void fill(PreviewData d) {
        name.set(d.name());
        ping.set(d.ping());
        players.set(d.players());
        version.set(d.version());
        sample.set(d.sample());
        bad.set(d.bad());
        fav.set(d.favourite());
        lines.replaceAll(lines(d.motd()));
        open.set(true);
    }

    void place(float px, float py) {
        if (x.peek() != px) x.set(px);
        if (y.peek() != py) y.set(py);
    }

    private static List<Line> lines(List<MotdSegment> motd) {
        List<Line> out = new ArrayList<>();
        int n = 0;
        for (MotdLines.Line l : MotdLines.split(motd)) {
            List<Seg> segs = new ArrayList<>();
            int k = 0;
            for (MotdSegment s : l.segs()) segs.add(new Seg(n + "." + k++, s.text(), s.color(), s.bold(), s.italic()));
            if (segs.isEmpty()) segs.add(new Seg(n + ".0", " ", MotdReader.DEFAULT, false, false));
            out.add(new Line(Integer.toString(n++), segs));
        }
        return out;
    }
}
