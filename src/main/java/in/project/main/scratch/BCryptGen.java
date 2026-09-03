package in.project.main.scratch;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println("BCRYPT_HASH=" + hash);
    }
}
