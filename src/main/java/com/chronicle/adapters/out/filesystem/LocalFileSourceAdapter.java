package com.chronicle.adapters.out.filesystem;

import com.chronicle.domain.port.FileSourcePort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Component
public class LocalFileSourceAdapter implements FileSourcePort {

    @Override
    public boolean folderExists(String sourceFolder) {
        return Files.isDirectory(Path.of(sourceFolder));
    }

    @Override
    public List<String> listTextFiles(String sourceFolder) {
        try (var stream = Files.list(Path.of(sourceFolder))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to list source folder " + sourceFolder, exception);
        }
    }

    @Override
    public String readTextFile(String sourceFolder, String documentName) {
        try {
            return Files.readString(Path.of(sourceFolder, documentName), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read document " + documentName, exception);
        }
    }
}
