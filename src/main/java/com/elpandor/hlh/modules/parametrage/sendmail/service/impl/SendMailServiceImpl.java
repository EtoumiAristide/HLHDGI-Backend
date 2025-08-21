package com.elpandor.hlh.modules.parametrage.sendmail.service.impl;

import com.elpandor.hlh.modules.parametrage.sendmail.mapper.SendMailMapper;
import com.elpandor.hlh.modules.parametrage.sendmail.model.SendMail;
import com.elpandor.hlh.modules.parametrage.sendmail.model.dto.SendMailDTO;
import com.elpandor.hlh.modules.parametrage.sendmail.repository.SendMailRepository;
import com.elpandor.hlh.modules.parametrage.sendmail.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SendMailServiceImpl implements SendMailService {

    @Autowired
    SendMailRepository sendMailRepository;

    @Autowired
    SendMailMapper sendMailMapper;

    @Override
    public SendMailDTO create(SendMailDTO sendMailDTO) {
//        if (sendMailDTO.getSendmailID() == 0) sendMailDTO.setSendmailID(null);
        SendMail sendMail = sendMailRepository.save(sendMailMapper.toEntity(sendMailDTO));
        return sendMailMapper.toDto(sendMail);
    }
}
