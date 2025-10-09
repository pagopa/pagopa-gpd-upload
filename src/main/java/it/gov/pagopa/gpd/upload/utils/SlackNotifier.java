package it.gov.pagopa.gpd.upload.utils;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadV2Request;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Singleton
public class SlackNotifier {

    private final Slack slack = Slack.getInstance();

    private final String botToken;

    private final String channelId;

    public SlackNotifier(
            @Property(name="slack.token") String botToken,
            @Property(name="slack.channel") String channelId
    ) {
        this.botToken = botToken;
        this.channelId = channelId;
    }

    public void uploadCsv(
            String title,
            String fileDescription,
            File csvFile) throws IOException, SlackApiException {

        FilesUploadV2Request request = FilesUploadV2Request.builder()
                .channels(List.of(channelId))
                .title(fileDescription)
                .file(csvFile)
                .filename(csvFile.getName())
                .initialComment(title)
                .build();

        slack.methods(botToken).filesUploadV2(request);
    }
}
