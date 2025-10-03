package it.gov.pagopa.gpd.upload.utils;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.response.files.FilesUploadV2Response;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@MicronautTest
class SlackNotifierTest {

    private static final String botToken = "test-bot-token";
    private static final String channelId = "C1234567890";

    @Mock
    Slack mockSlack;
    @Mock
    MethodsClient mockMethods;

    @Captor
    ArgumentCaptor<FilesUploadV2Request> requestCaptor;

    private SlackNotifier slackNotifier;

    private MockedStatic<Slack> mockedSlackStatic;


    @BeforeEach
    void setup() throws SlackApiException, IOException {
        MockitoAnnotations.openMocks(this);

        mockedSlackStatic = Mockito.mockStatic(Slack.class);
        mockedSlackStatic.when(Slack::getInstance).thenReturn(mockSlack);

        when(mockSlack.methods(botToken)).thenReturn(mockMethods);

        FilesUploadV2Response mockResponse = mock(FilesUploadV2Response.class);
        when(mockResponse.isOk()).thenReturn(true);

        doReturn(mockResponse)
                .when(mockMethods).filesUploadV2(any(FilesUploadV2Request.class));

        slackNotifier = new SlackNotifier(botToken, channelId);
    }

    @AfterEach
    void tearDown() {
        mockedSlackStatic.close();
    }

    @Test
    void uploadCsv_shouldCallApiWithCorrectParameters() throws IOException, SlackApiException {
        String title = "Report Giornaliero";
        String description = "Dettagli CSV del 2023-10-03";

        File mockFile = new File("/tmp/daily_report.csv");

        slackNotifier.uploadCsv(title, description, mockFile);

        verify(mockMethods, times(1)).filesUploadV2(requestCaptor.capture());

        FilesUploadV2Request capturedRequest = requestCaptor.getValue();

        List<String> expectedChannels = List.of(channelId);
        assert capturedRequest.getChannels().equals(expectedChannels) : "Channel ID is incorrect";
        assert capturedRequest.getInitialComment().equals(title) : "Initial comment (title) is incorrect";
        assert capturedRequest.getTitle().equals(description) : "File description (title) is incorrect";
        assert capturedRequest.getFilename().equals(mockFile.getName()) : "Filename is incorrect";
        assert capturedRequest.getFile().equals(mockFile) : "File object is incorrect";

        verify(mockSlack, times(1)).methods(botToken);
    }

    @Test
    void uploadCsv_shouldThrowExceptionOnSlackApiError() throws SlackApiException, IOException {
        File mockFile = new File("error_file.csv");
        Response fakeResponse = new Response.Builder()
                .code(500)
                .message("Internal Server Error")
                .request(new Request.Builder().url("http://localhost/").build())
                .protocol(Protocol.HTTP_1_1)
                .build();
        SlackApiException expectedException = new SlackApiException(fakeResponse, "failed to upload file");

        doThrow(expectedException)
                .when(mockMethods).filesUploadV2(any(FilesUploadV2Request.class));

        assertThrows(SlackApiException.class, () ->
                slackNotifier.uploadCsv("Title", "Description", mockFile)
        );

        verify(mockMethods, times(1)).filesUploadV2(any(FilesUploadV2Request.class));
    }
}