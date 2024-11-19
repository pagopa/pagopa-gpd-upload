package it.gov.pagopa.gpd.upload.scheduled;

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

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");

    private final long SLEEP_TIME_MILLIS = 100;

    private final StatusRepository statusRepository;

    @Inject
    public CronJob(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Scheduled(fixedRate = "${cron.recovery.rate}") // every hour
    void execute() {
        LOG.info("[Recovery] Start cron job: {}", DATE_FORMAT.format(new Date()));
        List<Status> statusList = statusRepository.find("SELECT * FROM c WHERE c.upload['end'] = null and c.upload.current = c.upload.total");
        statusList.forEach(status -> {
            status.upload.setEnd(LocalDateTime.now());
            LOG.info("[Recovery] Recovered status with upload-id: {} at {}", status.getId(), DATE_FORMAT.format(new Date()));
            statusRepository.upsert(status);

            try {
                Thread.sleep(SLEEP_TIME_MILLIS);
            } catch (InterruptedException e) {
                log.error("[Recovery][Exception] cron job: thread sleep interrupted: {}", e.getMessage());
            }
        });
        LOG.info("[Recovery] End cron job: {}", DATE_FORMAT.format(new Date()));
    }
}