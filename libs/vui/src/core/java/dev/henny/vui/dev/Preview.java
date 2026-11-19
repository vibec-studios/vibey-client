package dev.henny.vui.dev;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.Model;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.dom.Document;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.headless.TreeDump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Preview {
    private Preview() {
    }

    public static void main(String[] args) throws Exception {
        PreviewOptions options;
        try {
            options = PreviewOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        int errors = run(options);
        if (!options.serve()) System.exit(errors > 0 ? 1 : 0);
    }

    public static int run(PreviewOptions o) throws IOException, InterruptedException {
        Headless headless = Headless.create();
        Vui vui = headless.vui().devMode(o.serve());
        PreviewSource source = PreviewSource.of(o.file(), o.root());
        vui.registry().addRoot(source.root());
        headless.textures().root(source.textureRoot());
        Diagnostics stateDiagnostics = new Diagnostics();
        Model model = o.state() == null ? new Model()
                : ModelJson.read(o.state().toString(), Files.readString(o.state(), StandardCharsets.UTF_8), stateDiagnostics);
        Document doc = vui.open(source.id(), model);
        headless.screenshot(doc, o.width(), o.height(), o.scale(), o.output());
        System.out.print(TreeDump.dump(doc.root()));
        Diagnostics all = new Diagnostics().addAll(stateDiagnostics).addAll(doc.diagnostics());
        if (!all.isEmpty()) System.out.print(all.format());
        System.out.println("wrote " + o.output().toAbsolutePath());
        if (o.serve()) serve(vui, headless, doc, o);
        return all.errors().size();
    }

    private static void serve(Vui vui, Headless headless, Document doc, PreviewOptions o) throws IOException, InterruptedException {
        vui.reloader().watch();
        DevTarget target = new DevTarget() {
            @Override
            public List<Document> documents() {
                return vui.reloader().documents();
            }

            @Override
            public Document open(String id) {
                return vui.open(id, new Model());
            }

            @Override
            public byte[] screenshot() {
                try {
                    return headless.png(active(), o.scale());
                } catch (IOException e) {
                    return null;
                }
            }
        };
        DevBridge bridge = new DevBridge(vui, target).start(o.servePort());
        System.out.println("dev bridge on http://127.0.0.1:" + bridge.port() + "/ (Ctrl+C to stop)");
        while (true) {
            vui.tick();
            for (Document d : vui.reloader().documents()) d.frame(0.05f);
            Thread.sleep(50);
        }
    }
}
