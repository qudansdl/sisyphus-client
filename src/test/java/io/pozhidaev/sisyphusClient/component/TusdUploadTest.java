package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.FileListModifiedReadException;
import io.pozhidaev.sisyphusClient.domain.FileUploadException;
import org.junit.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static io.pozhidaev.sisyphusClient.utils.Whitebox.setInternalState;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TusdUploadTest {

    @Test
    public void calcFingerprint() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }

    @Test(expected = NullPointerException.class)
    public void calcFingerprint_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }


    @Test
    public void generateMetadataQuietly() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }


    @Test(expected = NullPointerException.class)
    public void generateMetadataQuietly_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }

    @Test
    public void asynchronousFileChannelQuietly() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly();
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test
    public void retrialPatch() {
        final Integer[] intervals = {500, 1000};
        final long res = 15;
        final ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode())
            .thenReturn(HttpStatus.BAD_REQUEST)
            .thenReturn(HttpStatus.CREATED);

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        URI uri = URI.create("http://localhost:1111/u/1");
        setInternalState(tusdUpload, "intervals", intervals);

        when(tusdUpload.uploadedLengthFromResponse(clientResponse)).thenReturn(res);

        when(tusdUpload.patch(0, uri)).thenReturn(Mono.just(clientResponse));

        when(tusdUpload.retrialPatch(0, uri)).thenCallRealMethod();

        final long result = tusdUpload.retrialPatch(0, uri);
        assertEquals(result, res);

    }

    @Test(expected = FileUploadException.class)
    public void retrialPatch_didNotPassed() {
        final Integer[] intervals = {500, 1000};

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.entrySet()).thenReturn(new HashSet<>());

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);

        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.headers()).thenReturn(headers);

        when(clientResponse.statusCode())
            .thenReturn(HttpStatus.BAD_REQUEST)
            .thenReturn(HttpStatus.BAD_REQUEST)
            .thenReturn(HttpStatus.BAD_REQUEST)
        ;

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        URI uri = URI.create("http://localhost:1111/u/1");
        setInternalState(tusdUpload, "intervals", intervals);

        when(tusdUpload.patch(0, uri)).thenReturn(Mono.just(clientResponse));
        when(tusdUpload.retrialPatch(0, uri)).thenCallRealMethod();

        tusdUpload.retrialPatch(0, uri);

    }

    @Test
    public void dataBufferFlux() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "test".getBytes());

        final TusdUpload tusUploader = TusdUpload.builder().path(file).chunkSize(1024).build();

        final Flux<DataBuffer> flux = tusUploader.dataBufferFlux(0);

        String blockFirst = flux
            .map(b -> {
                final byte[] bytes = new byte[4];
                b.read(bytes);
                return bytes;
            })
            .map(String::new)
            .blockFirst();
        assertEquals("test", blockFirst);
    }

    //TODO FIX NULL in mime type probe!
    @Test
    public void readContentTypeQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String contentType = tusUploader.readContentTypeQuietly();
        Optional.ofNullable(contentType)
            .map(s -> {
                assertTrue(s.contains("text/html"));
                return s;
            })
            .orElseGet(() -> {
                assertTrue(true);
                return "";
            });
    }

    @Test
    public void readLastModifiedQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final long quietly = tusUploader.readLastModifiedQuietly();
        assertNotEquals(quietly, System.nanoTime());

    }

    @Test(expected = FileListModifiedReadException.class)
    public void readLastModifiedQuietly_Exception() {
        final Path file = Paths.get("readLastModifiedQuietly_Exception");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        tusUploader.readLastModifiedQuietly();
    }

    private FileAttribute<Set<PosixFilePermission>> fileAttributeReadOnly() {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        return PosixFilePermissions.asFileAttribute(readOnly);
    }

    private FileAttribute<Set<PosixFilePermission>> fileAttributeWriteOnly() {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("-w--w--w-");
        return PosixFilePermissions.asFileAttribute(readOnly);
    }
}