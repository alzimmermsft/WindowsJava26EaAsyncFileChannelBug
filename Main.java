// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class Main {
    private static final String REPLACEMENT = "test";
    private static final String ORIGINAL = "hello there";
    private static final String EXPECTED = "testo there";

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        usingDirectBuffers();
        usingHeapBuffers();
    }

    private static void usingDirectBuffers() throws IOException, ExecutionException, InterruptedException {
        File file = createFileIfNotExist();
        byte[] bytes = REPLACEMENT.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer1 = ByteBuffer.allocateDirect(2);
        buffer1.put(0, bytes, 0, 2);
        buffer1.position(0);

        ByteBuffer buffer2 = ByteBuffer.allocateDirect(2);
        buffer2.put(0, bytes, 2, 2);
        buffer2.position(0);

        writeBuffersAndValidate(file, List.of(buffer1, buffer2), "direct");
    }

    private static void usingHeapBuffers() throws IOException, ExecutionException, InterruptedException {
        File file = createFileIfNotExist();
        byte[] bytes = REPLACEMENT.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer1 = ByteBuffer.wrap(bytes, 0, 2);
        ByteBuffer buffer2 = ByteBuffer.wrap(bytes, 2, 2);

        writeBuffersAndValidate(file, List.of(buffer1, buffer2), "heap");
    }

    private static void writeBuffersAndValidate(File file, List<ByteBuffer> buffers, String bufferType)
        throws IOException, ExecutionException, InterruptedException {
        try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            long position = 0;
            for (ByteBuffer buffer : buffers) {
                while (buffer.hasRemaining()) {
                    position += channel.write(buffer, position).get();
                }

                Thread.sleep(5000);
            }
        }

        String writtenResult = Files.readString(file.toPath());
        if (!EXPECTED.equals(writtenResult)) {
            System.out.printf(
                "Written file doesn't match expected output using %s ByteBuffers. Actual: %s, expected: %s%n",
                bufferType, writtenResult, EXPECTED);
        }
    }

    private static File createFileIfNotExist() {
        String fileName = UUID.randomUUID().toString();
        File file = new File("target");
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new RuntimeException("Unable to create directories: " + file.getAbsolutePath());
            }
        }

        try {
            File tempFile = Files.createTempFile(file.toPath(), fileName, "").toFile();
            Files.writeString(tempFile.toPath(), ORIGINAL);
            return tempFile;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
