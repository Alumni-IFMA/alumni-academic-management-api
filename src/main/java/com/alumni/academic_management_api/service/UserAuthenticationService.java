package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class UserAuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Role role = user.getRole() != null ? user.getRole() : Role.ALUMNI;
        String password = user.getPassword() != null ? user.getPassword() : "";
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(password)
                .authorities("ROLE_" + role.name())
                .build();
    }
}
