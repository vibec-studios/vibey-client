package dev.henny.vui.dev;

import dev.henny.vui.reload.DirectoryRoot;
import dev.henny.vui.reload.DocumentId;
import dev.henny.vui.reload.LooseFileRoot;
import dev.henny.vui.reload.ResourceRoot;

import java.nio.file.Path;

public record PreviewSource(ResourceRoot root, Path textureRoot, String id) {

    public static PreviewSource of(Path file, Path explicitRoot) {
        Path abs = file.toAbsolutePath().normalize();
        String norm = abs.toString().replace('\\', '/');
        int assets = norm.lastIndexOf("/assets/");
        if (assets >= 0) {
            Path root = explicitRoot != null ? explicitRoot.toAbsolutePath().normalize() : Path.of(norm.substring(0, assets));
            DocumentId id = DocumentId.fromResource(norm.substring(assets + 1));
            if (id != null) return new PreviewSource(new DirectoryRoot(root), root, id.toString());
        }
        String name = abs.getFileName().toString();
        String base = name.endsWith(DocumentId.JSON) ? name.substring(0, name.length() - DocumentId.JSON.length())
                : name.endsWith(DocumentId.MARKUP) ? name.substring(0, name.length() - DocumentId.MARKUP.length()) : name;
        Path dir = abs.getParent();
        Path textures = explicitRoot != null ? explicitRoot.toAbsolutePath().normalize() : dir;
        return new PreviewSource(new LooseFileRoot(dir), textures, LooseFileRoot.NAMESPACE + ":" + base);
    }
}
