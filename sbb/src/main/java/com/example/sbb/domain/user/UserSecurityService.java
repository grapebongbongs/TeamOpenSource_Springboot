package com.example.sbb.domain.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSecurityService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SiteUser u = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.builder()
            .username(u.getUsername())
            .password(u.getPassword())
            .authorities(new SimpleGrantedAuthority(
                u.getRole() != null ? u.getRole() : "ROLE_USER"
            ))
            .build();
    }

}
