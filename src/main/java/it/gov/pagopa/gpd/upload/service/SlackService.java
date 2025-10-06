package it.gov.pagopa.gpd.upload.service;

import com.slack.api.methods.SlackApiException;
import it.gov.pagopa.gpd.upload.utils.SlackNotifier;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;

@Singleton
public class SlackService {

    private final SlackNotifier slackNotifier;

    @Inject
    public SlackService(SlackNotifier slackNotifier) {
        this.slackNotifier = slackNotifier;
    }

    public void uploadCsv(String title,
                          String fileDescription,
                          File csvFile) throws SlackApiException, IOException {
        slackNotifier.uploadCsv(title, fileDescription, csvFile);
    }
}