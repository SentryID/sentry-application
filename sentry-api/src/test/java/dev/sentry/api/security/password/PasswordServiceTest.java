package dev.sentry.api.security.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private static final String PASSWORD = "S3nha!forte";

    private final PasswordService passwordService = serviceWith(19456, 2, 1);

    private static PasswordService serviceWith(int memory, int iterations, int parallelism) {
        return new PasswordService(PasswordEncoderConfiguration.delegatingEncoder(
                PasswordEncoderConfiguration.argon2(memory, iterations, parallelism)));
    }

    // RN-F0-02.1, RN-F0-02.3
    @Test
    void hashUsesArgon2idWithAlgorithmPrefixAndOwaspParameters() {
        String hash = passwordService.hash(PASSWORD);

        assertTrue(hash.startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$"), hash);
        assertFalse(hash.contains(PASSWORD));
    }

    @Test
    void hashUsesRandomSalt() {
        assertFalse(passwordService.hash(PASSWORD).equals(passwordService.hash(PASSWORD)));
    }

    // CA-F0-02.2
    @Test
    void verifyMatchesOnlyTheCorrectPassword() {
        String hash = passwordService.hash(PASSWORD);

        assertTrue(passwordService.verify(PASSWORD, hash).matches());
        assertFalse(passwordService.verify("s3nha!forte", hash).matches());
        assertFalse(passwordService.verify("", hash).matches());
    }

    @Test
    void verifyFailsWithoutStoredHashOrPassword() {
        assertFalse(passwordService.verify(PASSWORD, null).matches());
        assertFalse(passwordService.verify(null, passwordService.hash(PASSWORD)).matches());
    }

    @Test
    void verifyDoesNotUpgradeHashWithCurrentParameters() {
        PasswordService.Verification verification = passwordService.verify(PASSWORD, passwordService.hash(PASSWORD));

        assertTrue(verification.matches());
        assertFalse(verification.needsUpgrade());
        assertNull(verification.upgradedHash());
    }

    // CA-F0-02.3
    @Test
    void verifyUpgradesHashWithWeakerMemory() {
        String weakHash = serviceWith(8192, 2, 1).hash(PASSWORD);

        PasswordService.Verification verification = passwordService.verify(PASSWORD, weakHash);

        assertTrue(verification.matches());
        assertTrue(verification.needsUpgrade());
        assertTrue(verification.upgradedHash().startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$"));
        assertTrue(passwordService.verify(PASSWORD, verification.upgradedHash()).matches());
    }

    @Test
    void verifyUpgradesHashWithFewerIterations() {
        String weakHash = serviceWith(19456, 1, 1).hash(PASSWORD);

        assertTrue(passwordService.verify(PASSWORD, weakHash).needsUpgrade());
    }

    @Test
    void verifyNeverUpgradesOnWrongPassword() {
        String weakHash = serviceWith(8192, 2, 1).hash(PASSWORD);

        PasswordService.Verification verification = passwordService.verify("wrong", weakHash);

        assertFalse(verification.matches());
        assertEquals(null, verification.upgradedHash());
    }
}
