package com.elpandor.hlh.modules.parametrage.privillege.repository;

import com.elpandor.hlh.modules.parametrage.privillege.model.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrivilegeRepository extends JpaRepository<Privilege, Integer> {
    public List<Privilege> findAllByRole_Id(int roleId);
}
