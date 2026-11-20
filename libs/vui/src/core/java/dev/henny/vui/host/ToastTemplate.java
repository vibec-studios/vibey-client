package dev.henny.vui.host;

final class ToastTemplate {
    static final String SOURCE = """
            <template>
              <column id="toasts">
                <toast v-for="t in toasts" :key="t.id" :class="t.kind" @click="dismiss(t)">
                  <icon v-if="t.icon" :src="t.iconTexture" :name="t.iconName"/>
                  <column class="toast-body">
                    <text class="toast-title">{{ t.title }}</text>
                    <text v-if="t.text" class="toast-text">{{ t.text }}</text>
                  </column>
                  <button v-if="t.action" class="toast-action" @click.stop="act(t)">{{ t.action }}</button>
                </toast>
              </column>
            </template>
            <style>
            #toasts { position: absolute; anchor: bottom-right; right: 6; bottom: 6; gap: 4; align-items: end; }
            .toast-body { gap: 1; grow: 1; }
            .toast-title { color: $text; }
            .toast-text { color: $muted; }
            .toast-action { height: 14; padding: 0 5; }
            toast:enter, toast:leave { opacity: 0; translate-y: 8; }
            toast.ok { border-color: $ok; }
            toast.warn { border-color: $warn; }
            toast.error { border-color: $danger; }
            </style>
            """;

    private ToastTemplate() {
    }
}
