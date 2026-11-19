package dev.henny.r3.object;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.interact.Interactable;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.panel.PointerContent;

import java.util.UUID;

public final class WorldObject implements Interactable {
    public static final float DEFAULT_PIXELS_PER_BLOCK = 64f;

    private final String id;
    private final String kind;
    private final Transform transform = new Transform();
    private final Visibility visibility = new Visibility();
    private final ObjectState state = new ObjectState();
    private InteractionRules rules = new InteractionRules();
    private Anchor anchor = FreeAnchor.INSTANCE;
    private ObjectContent content;
    private String dimension;
    private UUID owner;
    private boolean remote;
    private boolean locked;
    private boolean fullBright = true;
    private float pixelsPerBlock = DEFAULT_PIXELS_PER_BLOCK;
    private long revision;
    private int layer;

    public WorldObject(String id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    public static WorldObject create(String kind) {
        return new WorldObject(UUID.randomUUID().toString(), kind);
    }

    public String id() {
        return id;
    }

    public String kind() {
        return kind;
    }

    @Override
    public Transform transform() {
        return transform;
    }

    public Visibility visibility() {
        return visibility;
    }

    public ObjectState state() {
        return state;
    }

    @Override
    public InteractionRules rules() {
        return rules;
    }

    public WorldObject rules(InteractionRules rules) {
        this.rules = rules;
        return this;
    }

    @Override
    public Anchor anchor() {
        return anchor;
    }

    @Override
    public void anchor(Anchor anchor) {
        this.anchor = anchor == null ? FreeAnchor.INSTANCE : anchor;
    }

    public WorldObject anchoredTo(Anchor anchor) {
        anchor(anchor);
        return this;
    }

    public ObjectContent content() {
        return content;
    }

    public WorldObject content(ObjectContent content) {
        this.content = content;
        state.markLayoutDirty();
        return this;
    }

    @Override
    public PointerContent pointerContent() {
        return content instanceof PointerContent p ? p : null;
    }

    public String dimension() {
        return dimension;
    }

    public WorldObject dimension(String dimension) {
        this.dimension = dimension;
        return this;
    }

    public UUID owner() {
        return owner;
    }

    public WorldObject owner(UUID owner) {
        this.owner = owner;
        return this;
    }

    public boolean remote() {
        return remote;
    }

    public WorldObject remote(boolean remote) {
        this.remote = remote;
        return this;
    }

    @Override
    public boolean locked() {
        return locked || remote;
    }

    public boolean lockedByUser() {
        return locked;
    }

    public WorldObject locked(boolean locked) {
        this.locked = locked;
        return this;
    }

    public boolean fullBright() {
        return fullBright;
    }

    public WorldObject fullBright(boolean fullBright) {
        this.fullBright = fullBright;
        return this;
    }

    public float pixelsPerBlock() {
        return pixelsPerBlock;
    }

    public WorldObject pixelsPerBlock(float pixelsPerBlock) {
        this.pixelsPerBlock = Math.max(1f, pixelsPerBlock);
        state.markLayoutDirty();
        return this;
    }

    @Override
    public int pixelWidth() {
        return Math.max(1, Math.round(transform.width() * pixelsPerBlock));
    }

    @Override
    public int pixelHeight() {
        return Math.max(1, Math.round(transform.height() * pixelsPerBlock));
    }

    public int layer() {
        return layer;
    }

    public WorldObject layer(int layer) {
        this.layer = layer;
        return this;
    }

    public long revision() {
        return revision;
    }

    public WorldObject revision(long revision) {
        this.revision = revision;
        return this;
    }

    public long nextRevision() {
        return ++revision;
    }

    @Override
    public boolean hoverable() {
        return rules.interactive() && state.visible();
    }
}
