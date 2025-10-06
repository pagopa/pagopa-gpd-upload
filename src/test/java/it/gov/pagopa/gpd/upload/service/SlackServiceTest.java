package it.gov.pagopa.gpd.upload.service;

import com.slack.api.methods.SlackApiException;
import it.gov.pagopa.gpd.upload.utils.SlackNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SlackServiceTest {

    private SlackNotifier slackNotifier;
    private SlackService slackService;

    @BeforeEach
    void setUp() {
        slackNotifier = mock(SlackNotifier.class);
        slackService = new SlackService(slackNotifier);
    }

    @Test
    void uploadCsv_OK() throws SlackApiException, IOException {
        String title = "Title";
        String description = "Description";
        File file = mock(File.class);

        doNothing().when(slackNotifier).uploadCsv(title, description, file);

        assertDoesNotThrow(() -> slackService.uploadCsv(title, description, file));
        verify(slackNotifier, times(1)).uploadCsv(title, description, file);
    }


    @Test
    void uploadCsv_IOException() throws SlackApiException, IOException {
        String title = "Title";
        String description = "Description";
        File file = mock(File.class);

        doThrow(new IOException("IO Error")).when(slackNotifier).uploadCsv(title, description, file);

        assertThrows(IOException.class, () -> slackService.uploadCsv(title, description, file));
        verify(slackNotifier, times(1)).uploadCsv(title, description, file);
    }
}
