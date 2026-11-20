package dev.henny.r3.vui;

final class DemoDocs {
    static final String PLAYER = """
            <template>
              <column class="card" gap="4">
                <text class="title">{{ title }}</text>
                <row gap="4">
                  <button id="play" @click="play()">Play</button>
                  <button id="next" @click="next()">Next</button>
                  <button @click="stop()">Stop</button>
                </row>
                <text>{{ status }}</text>
              </column>
            </template>
            <style>
            .card { padding: 6; background: 0xE0101014; }
            .title { color: #ffffff; }
            button { padding: 3; background: #303040; }
            button:hover { background: #5060f0; }
            </style>
            """;

    private DemoDocs() {}

    static final class Controller {
        public String title = "r3";
        public String status = "idle";
        int plays;
        int nexts;
        int stops;

        public void play() {
            plays++;
            status = "playing";
        }

        public void next() {
            nexts++;
        }

        public void stop() {
            stops++;
            status = "stopped";
        }
    }
}
