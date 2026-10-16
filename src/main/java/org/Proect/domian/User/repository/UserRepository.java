package org.Proect.domian.User.repository;

import org.Proect.domian.Enums.UserStatus;
import org.Proect.domian.User.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Роль подгружается сразу везде, где она нужна маппере.
    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByLogin(String login);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByNumberPhone(String numberPhone);

    Optional<User> findByNumberPhone(String numberPhone);

    @EntityGraph(attributePaths = "role")
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "role")
    List<User> findByZoneId(Integer zoneId);

    // Поиск по склеенному ФИО
    // Спецсимволы % и _ нужно экранировать до вызова (см. UserService.escapeLike).
    @EntityGraph(attributePaths = "role")
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(CONCAT(u.lastName, ' ', u.firstName, ' ', COALESCE(u.middleName, '')))
                  LIKE LOWER(CONCAT('%', :searchTerm, '%')) ESCAPE '\\'
            """)
    Page<User> searchByFullName(@Param("searchTerm") String searchTerm, Pageable pageable);
}