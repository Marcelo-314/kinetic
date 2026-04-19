package com.chronicle.domain.port;

import java.util.List;

public interface FileSourcePort {

    boolean folderExists(String sourceFolder);

    List<String> listTextFiles(String sourceFolder);
}
