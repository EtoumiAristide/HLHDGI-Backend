package com.elpandor.hlh.modules.parametrage.sendmail.repository;

import com.elpandor.hlh.modules.parametrage.sendmail.model.SendMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SendMailRepository extends JpaRepository<SendMail, Long> {
}
