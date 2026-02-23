package com.denisbondd111.authservice.repository;

import com.denisbondd111.authservice.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
}
