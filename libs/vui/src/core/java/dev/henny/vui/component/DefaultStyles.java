package dev.henny.vui.component;

public final class DefaultStyles {
    private DefaultStyles() {
    }

    public static String css() {
        return LAYOUT + CONTROLS + CHOICES + OVERLAYS;
    }

    private static final String LAYOUT = """
            root { display: stack; color: $text; align-items: stretch; }
            box, column, card, label { display: column; }
            row { display: row; align-items: center; }
            stack { display: stack; }
            grid { display: grid; }
            scroll, list { display: column; overflow: scroll; }
            b { font-weight: bold; }
            divider { height: 1; background: $line; flex-shrink: 0; align-self: stretch; }
            spacer { flex-grow: 1; }
            icon { width: 9; height: 9; flex-shrink: 0; }
            image { flex-shrink: 0; }
            card { background: $panel; border: 1 $line; padding: 8; gap: 4; }
            """;

    private static final String CONTROLS = """
            button { display: row; align-items: center; justify-content: center; gap: 4; height: 18; padding: 0 8;
                     background: $surface2; border: 1 $line2; color: $text; cursor: pointer; flex-shrink: 0;
                     transition: background 150ms out, border-color 150ms out, color 150ms out; }
            button:hover, button:focus { background: $surface3; border-color: $brand; }
            button:active { background: $surface; }
            button.primary { background: $brand; border-color: $brand; color: #ffffff; }
            button.primary:hover, button.primary:focus { background: $brand-hover; border-color: $brand-hover; }
            button.ghost { background: transparent; border-color: transparent; color: $muted; }
            button.ghost:hover { color: $text; background: $surface2; }
            button.danger { color: $danger; }
            button:disabled { color: $dim; background: $surface; border-color: $line; cursor: not-allowed; }
            toggle { width: 22; height: 12; background: $surface3; border: 1 $line2; thumb-color: $muted; cursor: pointer;
                     flex-shrink: 0; transition: background 150ms out, border-color 150ms out; }
            toggle:checked { background: $brand; border-color: $brand; thumb-color: #ffffff; }
            toggle:focus { border-color: $brand-hover; }
            toggle:disabled { background: $surface; thumb-color: $dim; }
            checkbox { display: row; align-items: center; gap: 4; min-height: 9; padding-left: 13; color: $text;
                       track-color: $line2; accent-color: $brand; thumb-color: #ffffff; cursor: pointer; }
            checkbox:focus, checkbox:hover { track-color: $brand; }
            slider { width: 120; height: 10; track-color: $surface3; accent-color: $brand; thumb-color: $text; cursor: pointer; flex-shrink: 0; }
            slider:hover, slider:focus { thumb-color: $brand-hover; }
            textfield { width: 120; height: 18; padding: 0 4; background: $panel; border: 1 $line2; color: $text;
                        accent-color: $brand; cursor: text; flex-shrink: 0; transition: border-color 150ms out; }
            textfield:hover { border-color: $muted; }
            textfield:focus { border-color: $brand; }
            textfield:invalid { border-color: $danger; }
            keybind { display: row; align-items: center; justify-content: center; height: 16; min-width: 40; padding: 0 6;
                      background: $surface2; border: 1 $line2; cursor: pointer; flex-shrink: 0; }
            keybind:hover, keybind:focus { border-color: $brand; }
            keybind:open { border-color: $warn; color: $warn; }
            colorpicker { width: 16; height: 16; padding: 2; border: 1 $line2; cursor: pointer; flex-shrink: 0; }
            colorpicker:hover, colorpicker:focus { border-color: $brand; }
            progress { height: 3; track-color: $surface3; accent-color: $brand; flex-shrink: 0; }
            spinner { width: 9; height: 9; accent-color: $brand; flex-shrink: 0; }
            """;

    private static final String CHOICES = """
            select { display: row; align-items: center; height: 18; min-width: 60; padding: 0 10 0 4; gap: 4;
                     background: $surface2; border: 1 $line2; cursor: pointer; flex-shrink: 0; }
            select:hover, select:focus, select:open { border-color: $brand; }
            select-menu { display: column; background: $surface; border: 1 $line2; padding: 1; min-width: 60; max-height: 160; overflow: scroll; }
            select-option { display: row; align-items: center; height: 14; padding: 0 4; color: $muted; cursor: pointer; }
            select-option:hover { background: $surface3; color: $text; }
            select-option:selected { color: $brand2; }
            segmented { display: row; border: 1 $line2; background: $panel; flex-shrink: 0; }
            segment { display: row; align-items: center; justify-content: center; height: 16; padding: 0 6; color: $muted; cursor: pointer;
                      transition: background 150ms out, color 150ms out; }
            segment:hover { color: $text; }
            segment:selected { background: $brand; color: #ffffff; }
            segmented:focus { border-color: $brand; }
            tabs { display: row; gap: 10; border-bottom: 1 $line; flex-shrink: 0; }
            tab { padding: 3 1; border-bottom: 1 transparent; color: $muted; cursor: pointer; }
            tab:hover { color: $text; }
            tab:selected { color: $text; border-color: $brand; }
            """;

    private static final String OVERLAYS = """
            tooltip, tooltip-bubble { display: column; padding: 3 5; max-width: 180; background: $surface3; border: 1 $line2;
                                      color: $text; pointer-events: none; }
            popover { display: column; gap: 4; padding: 4; background: $surface; border: 1 $line2; }
            modal { display: stack; align-items: center; background: $scrim; }
            colorpicker-panel { width: 110; height: 80; padding: 4; background: $surface; border: 1 $line2; cursor: pointer; }
            toast { display: row; align-items: center; gap: 6; padding: 6 8; min-width: 140; background: $surface;
                    border: 1 $line2; transition: opacity 150ms out, translate-y 150ms out; }
            """;
}
