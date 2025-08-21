package com.elpandor.hlh.modules.parametrage.role.repository;

import com.elpandor.hlh.modules.parametrage.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}
