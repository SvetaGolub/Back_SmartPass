package org.Proect.domian.User.service;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.Enums.UserStatus;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Связывает Spring Security с таблицей users. */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Имя роли из таблицы roles станет authority ROLE_<NAME>
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .roles(user.getRole().getName().toUpperCase())
                .accountLocked(user.getStatus() == UserStatus.BLOCKED)
                .build();
    }
}