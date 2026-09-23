package com.ticket.user.infrastructure;

import com.ticket.security.Role;
import com.ticket.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}
