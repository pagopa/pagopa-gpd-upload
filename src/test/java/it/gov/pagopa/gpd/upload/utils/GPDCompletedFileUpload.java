package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

public class GPDCompletedFileUpload implements CompletedFileUpload {
    private final String filename;
    private final MediaType mediaType;
    private final byte[] content;

    public GPDCompletedFileUpload(String filename, MediaType mediaType, byte[] content) {
        this.filename = filename;
        this.mediaType = mediaType;
        this.content = (content != null ? content : new byte[0]);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public ByteBuffer getByteBuffer() throws IOException {
        return ByteBuffer.wrap(content);
    }

    @Override
    public Optional<MediaType> getContentType() {
        return Optional.of(mediaType);
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public long getDefinedSize() {
        return content.length;
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
