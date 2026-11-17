package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class EmailVerificationKeysTests {
    @TempDir Path directory;

    @Test
    void separateRawKeysLoadAndMissingMailKeyDoesNotDisableGlobalRate() throws Exception {
        var env = new MockEnvironment().withProperty("app.security.rate-limit-key-file", file("rate", 32, (byte) 1));
        var rateOnly = new EmailVerificationKeys(env);
        assertThat(rateOnly.rateHasher()).isNotNull();
        assertThatThrownBy(rateOnly::codeHasher).isInstanceOf(AuthStateUnavailableException.class);
        env.withProperty("app.email.code-key-file", file("code", 32, (byte) 2));
        assertThat(new EmailVerificationKeys(env).codeHasher().hash("000042")).hasSize(32);
    }

    @Test
    void missingWrongLengthOrSameKeyFailsClosed() throws Exception {
        assertThatThrownBy(new EmailVerificationKeys(new MockEnvironment())::rateHasher)
                .isInstanceOf(AuthStateUnavailableException.class);
        var env = new MockEnvironment().withProperty("app.security.rate-limit-key-file", file("short", 31, (byte) 1));
        assertThatThrownBy(new EmailVerificationKeys(env)::rateHasher).isInstanceOf(AuthStateUnavailableException.class);
        env.withProperty("app.security.rate-limit-key-file", file("same", 32, (byte) 1))
                .withProperty("app.email.code-key-file", env.getProperty("app.security.rate-limit-key-file"));
        assertThatThrownBy(new EmailVerificationKeys(env)::codeHasher).isInstanceOf(AuthStateUnavailableException.class);
    }

    private String file(String name, int length, byte value) throws Exception {
        var bytes = new byte[length];
        Arrays.fill(bytes, value);
        return Files.write(directory.resolve(name), bytes).toString();
    }
}
