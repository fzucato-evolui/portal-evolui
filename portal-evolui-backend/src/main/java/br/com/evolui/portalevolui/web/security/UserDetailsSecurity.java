package br.com.evolui.portalevolui.web.security;

import br.com.evolui.portalevolui.web.beans.UserBean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetailsSecurity implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String login;

    private String name;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsSecurity(Long id, String login, String name, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsSecurity build(UserBean user) {
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority(user.getProfile().value()));

        return new UserDetailsSecurity(
                user.getId(),
                user.getLogin(),
                user.getName(),
                user.getPassword(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
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
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsSecurity user = (UserDetailsSecurity) o;
        return Objects.equals(id, user.id);
    }
}
