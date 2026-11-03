package org.Proect.domian.User.service;

import org.Proect.domian.Enums.UserStatus;
import org.Proect.domian.User.model.Roles;
import org.Proect.domian.User.model.User;
import org.Proect.domian.User.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DbUserDetailsService service = new DbUserDetailsService(userRepository);

    @Test
    void mapsAdminRoleToSpringRoleAuthority() {
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(user("ADMIN")));

        var details = service.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
        assertEquals(true, details.isEnabled());
    }

    @Test
    void keepsNonAdminRolesDistinctFromAdmin() {
        when(userRepository.findByLogin("security")).thenReturn(Optional.of(user("SECURITY")));

        var details = service.loadUserByUsername("security");

        assertEquals("ROLE_SECURITY", details.getAuthorities().iterator().next().getAuthority());
        assertEquals(false, details.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    private static User user(String roleName) {
        Roles role = new Roles();
        role.setId(1);
        role.setName(roleName);
        return User.builder()
                .login(roleName.equals("ADMIN") ? "admin" : "security")
                .passwordHash("not-used-by-this-test")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
