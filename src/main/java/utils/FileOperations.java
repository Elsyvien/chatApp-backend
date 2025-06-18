package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading and writing files.
 * It can be expanded as needed for the application.
 */
public class FileOperations {

    /**
     * Reads all lines of a file as UTF-8. If the file does not exist,
     * an empty list is returned.
     */
    public List<String> readAllLines(String filePath) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * Appends a single line to the given file using UTF-8 encoding.
     * The file is created if it does not already exist.
     */
    public void appendLine(String filePath, String line) throws IOException {
        Path path = Path.of(filePath);
        Files.write(path,
                (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }
}