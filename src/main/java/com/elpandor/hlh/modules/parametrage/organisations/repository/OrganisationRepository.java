package com.elpandor.hlh.modules.parametrage.organisations.repository;

import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, Integer> {
    @Query("SELECT o " +
            "FROM Organisation o " +
            "WHERE o.isdelete = false " +
            "AND o.id IN :ids " +
            "ORDER BY o.id")
    List<Organisation> getAllByMultipleId(int[] ids);
}