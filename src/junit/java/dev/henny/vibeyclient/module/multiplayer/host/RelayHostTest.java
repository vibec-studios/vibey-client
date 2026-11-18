package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hosts against a REAL Vibey relay with exactly the hello the client sends. Regression: relay 1.3.0 answered the
 * client's {@code "client": "vibey-client"} with "Invalid client." and every hosting attempt failed, while our tests
 * ran against an outdated local relay checkout. CI starts the relay from vibec-studios/vibey-multiplayer's main branch
 * ({@code ALLOW_OFFLINE=1}) and sets {@code VIBEY_TEST_RELAY=host:port}; without it the test is skipped.
 */
class RelayHostTest {
    @Test
    void relayAcceptsTheClientsHello() throws Exception {
        String target = System.getenv("VIBEY_TEST_RELAY");
        assumeTrue(target != null && !target.isBlank(), "VIBEY_TEST_RELAY not set (needs a relay with ALLOW_OFFLINE=1)");
        int colon = target.lastIndexOf(':');
        String host = target.substring(0, colon);
        int port = Integer.parseInt(target.substring(colon + 1));

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(10_000);
            OutputStream out = s.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out.write(RelayHello.magic());
            JsonObject hello = RelayHello.message("RelayTest", UUID.randomUUID(), "1.21.11", "0.0.0-test");
            assertEquals(RelayHello.CLIENT, hello.get("client").getAsString());
            send(out, hello.toString());

            String address = null;
            for (int i = 0; i < 10 && address == null; i++) {
                String line = in.readLine();
                if (line == null) fail("relay closed the control connection without an answer");
                if (line.isBlank()) continue;
                JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
                switch (msg.get("t").getAsString()) {
                    case "challenge" -> send(out, "{\"t\":\"auth\"}"); // ALLOW_OFFLINE: no Mojang check
                    case "ready" -> address = msg.get("address").getAsString();
                    case "error" -> fail("relay rejected the client's hello: " + msg);
                    default -> { }
                }
            }
            assertTrue(address != null && !address.isBlank(), "relay never sent ready");
        }
    }

    private static void send(OutputStream out, String line) throws java.io.IOException {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
