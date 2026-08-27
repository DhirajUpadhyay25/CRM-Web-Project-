package in.project.main.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import in.project.main.entities.Role;

/**
 * Custom implementation of UserDetails to wrap our unified authentication logic.
 * It stores the underlying entity type (User, Employee, or Admin properties)
 * and the assigned Role.
 */
public class CustomUserDetails implements UserDetails {

    private String email;
    private String password;
    private Role role;
    private String name;
    private boolean enabled;

    public CustomUserDetails(String email, String password, Role role, String name, boolean enabled) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.name = name;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
