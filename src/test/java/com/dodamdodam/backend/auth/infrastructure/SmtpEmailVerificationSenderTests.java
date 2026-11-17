package com.dodamdodam.backend.auth.infrastructure;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class SmtpEmailVerificationSenderTests {
    @Test
    void normalAndDecoyUseSameSmtpPathAndOnlyNormalContainsCode() throws Exception {
        try (var server = new SmtpServer("OK")) {
            sender(server.port()).send("recipient@example.invalid", "000042", true);
            var text = server.message.get(3, TimeUnit.SECONDS);
            var mime = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null,
                    new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            assertThat(mime.getContent().toString()).contains("000042");
        }
        try (var server = new SmtpServer("OK")) {
            sender(server.port()).send("recipient@example.invalid", "000042", false);
            var mime = new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null,
                    new ByteArrayInputStream(server.message.get(3, TimeUnit.SECONDS).getBytes(StandardCharsets.UTF_8)));
            assertThat(mime.getContent().toString()).doesNotContain("000042");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"REJECT", "LOST", "TIMEOUT"})
    void rejectionLostAcknowledgementAndReadTimeoutAreFailures(String mode) throws Exception {
        try (var server = new SmtpServer(mode)) {
            assertThatThrownBy(() -> sender(server.port()).send("recipient@example.invalid", "000042", true))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void missingSettingsInvalidTimeoutAndExternalPlaintextFailWithoutConnecting() {
        assertThatThrownBy(() -> new SmtpEmailVerificationSender(new MockEnvironment())
                .send("recipient@example.invalid", "000042", true)).isInstanceOf(RuntimeException.class);
        var env = environment(25).withProperty("app.email.smtp.host", "smtp.example.invalid");
        assertThatThrownBy(() -> new SmtpEmailVerificationSender(env).send("recipient@example.invalid", "000042", true))
                .isInstanceOf(RuntimeException.class);
        env.withProperty("app.email.smtp.host", "127.0.0.1").withProperty("app.email.smtp.timeout-ms", "0");
        assertThatThrownBy(() -> new SmtpEmailVerificationSender(env).send("recipient@example.invalid", "000042", true))
                .isInstanceOf(RuntimeException.class);
    }

    private SmtpEmailVerificationSender sender(int port) {
        return new SmtpEmailVerificationSender(environment(port));
    }

    private MockEnvironment environment(int port) {
        return new MockEnvironment().withProperty("app.email.smtp.host", "127.0.0.1")
                .withProperty("app.email.smtp.port", Integer.toString(port)).withProperty("app.email.smtp.starttls", "false")
                .withProperty("app.email.smtp.from", "sender@example.invalid").withProperty("app.email.smtp.timeout-ms", "300");
    }

    private static class SmtpServer implements AutoCloseable {
        final ServerSocket socket;
        final CompletableFuture<String> message = new CompletableFuture<>();
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        SmtpServer(String mode) throws Exception {
            socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            worker.submit(() -> {
                try (var client = socket.accept();
                     var reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                     var writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
                    client.setSoTimeout(2000);
                    writer.print("220 local test\r\n"); writer.flush();
                    String line;
                    boolean data = false;
                    var body = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (data && !line.equals(".")) { body.append(line).append("\r\n"); continue; }
                        if (data) {
                            message.complete(body.toString());
                            if (mode.equals("LOST")) { return; }
                            if (mode.equals("TIMEOUT")) { reader.readLine(); return; }
                            writer.print("250 accepted\r\n"); data = false;
                        }
                        else if (line.startsWith("DATA")) { writer.print("354 send data\r\n"); data = true; }
                        else if (line.startsWith("RCPT") && mode.equals("REJECT")) { writer.print("550 rejected\r\n"); }
                        else if (line.startsWith("QUIT")) { writer.print("221 bye\r\n"); writer.flush(); return; }
                        else { writer.print("250 OK\r\n"); }
                        writer.flush();
                    }
                }
                catch (Exception failure) { message.completeExceptionally(failure); }
            });
        }
        int port() { return socket.getLocalPort(); }
        public void close() throws Exception { socket.close(); worker.shutdownNow(); worker.awaitTermination(3, TimeUnit.SECONDS); }
    }
}
