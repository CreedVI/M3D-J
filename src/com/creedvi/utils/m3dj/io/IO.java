package com.creedvi.utils.m3dj.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IO {
    public static byte[] LoadFileData(String fileName) throws IOException {
        byte[] fileData = null;

        if (fileName != null) {
            Path path = Paths.get(fileName);

            try {
                fileData = Files.readAllBytes(path);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return fileData;
    }

    public static String LoadFileText(String fileName) throws IOException {
        StringBuilder text = new StringBuilder();

        if (fileName != null) {
            try {
                BufferedReader buffer = new BufferedReader(new FileReader(fileName));
                String line;
                while ((line = buffer.readLine()) != null) {
                    text.append(line);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return text.toString();
    }
}
