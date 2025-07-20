package com.elpandor.hlh.modules.users.repository;

import com.elpandor.hlh.modules.users.model.ERole;
import com.elpandor.hlh.modules.users.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
