package com.cvbuilder.repository;

import com.cvbuilder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndEmail(Long id, String email);

    // ✔ Kullanıcı e-posta ile var mı kontrol et
    boolean existsByEmail(String email);
}
