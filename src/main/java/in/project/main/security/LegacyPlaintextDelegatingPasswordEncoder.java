package in.project.main.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom PasswordEncoder that safely handles both existing plain-text passwords
 * and new BCrypt hashed passwords.
 * 
 * - When checking a password, if the stored DB password starts with the BCrypt
 *   signature ($2a$), it uses BCrypt verification.
 * - Otherwise, it falls back to a plain-text comparison.
 * - When encoding a new password (e.g. for registration), it ALWAYS uses BCrypt.
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
        
        // BCrypt hashes always start with "$2a$", "$2b$", or "$2y$"
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        } else {
            // Fallback for legacy plain-text passwords
            return rawPassword.toString().equals(encodedPassword);
        }
    }
}
