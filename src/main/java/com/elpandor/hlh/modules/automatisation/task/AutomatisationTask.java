package com.elpandor.hlh.modules.automatisation.task;

import com.elpandor.hlh.modules.automatisation.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class AutomatisationTask {

    private final TicketService ticketService;

    //    @Scheduled(cron = "0 0 8 * * 1") //Passage tous les lundis à 88h
//    @Scheduled(cron = "0 0 7 15 * *") //Passage tous les 15 de chaque mois à 7H00
//    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 5L)  //Passage chaque 5 minutes
    private void checkFileAndSendFNE() {
        log.info("Start Task - Checking file and send FN ...");
        try {
            ticketService.runAutomatisation();
            log.info("End Task - Checking file and send FN ...");
        } catch (Exception e) {
            log.error("Task Error while Checking file and send FN .", e);
            e.printStackTrace();
        }
    }
}
