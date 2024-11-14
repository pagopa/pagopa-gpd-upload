package it.gov.pagopa.gpd.upload.job;

import io.micronaut.scheduling.annotation.Scheduled;
import it.gov.pagopa.gpd.upload.entity.Status;
import it.gov.pagopa.gpd.upload.repository.StatusRepository;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * the EVENTUAL consistency level of cosmos does not allow synchronous update of the end field,
 * this cron is intended to add fault-tolerance to the end field update mechanism
 */
@Singleton
@Slf4j
public class CronJob {
    private static final Logger LOG = LoggerFactory.getLogger(CronJob.class);

    private final StatusRepository statusRepository;

    private final long sleepTimeMillis = 200;

    @Inject
    public CronJob(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Scheduled(fixedDelay = "60s", initialDelay = "5s") // todo extract env variables
    void execute() {
        LOG.info("Start recovery job: {}", new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(new Date()));
        List<Status> statusList = statusRepository.find("SELECT c.upload FROM c WHERE c.upload['end'] = null and c.upload.current = c.upload.total");
        statusList.forEach(status -> {
            status.upload.setEnd(LocalDateTime.now());
            statusRepository.saveStatus(status);
            try {
                Thread.sleep(sleepTimeMillis);
            } catch (InterruptedException e) {
                log.error("[Exception] Cron Job: thread sleep interrupted: {}", e.getMessage());
            }
        });
        LOG.info("Termination recovery job: {}", new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(new Date()));
    }
}