package in.project.main.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt-only PasswordEncoder.
 * All new passwords are encoded with BCrypt.
 * Legacy plaintext passwords should be migrated via the DataSeeder migration endpoint.
 */
public class LegacyPlaintextDelegatingPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        return bcrypt.matches(rawPassword, encodedPassword);
    }
}
