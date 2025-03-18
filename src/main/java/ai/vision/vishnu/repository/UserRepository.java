package ai.vision.vishnu.repository;

import ai.vision.vishnu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @NativeQuery("SELECT * FROM users WHERE id = :id and is_active = true")
    User findUserById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
