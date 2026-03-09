package br.com.evolui.portalevolui.web.util;

import com.amazonaws.util.IOUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

public class FileSaverUtil {
    public static String getFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1)).get();
    }

    public static String saveBase64File(String folder, String fileName, String base64) throws IOException {
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
        }
        base64 = base64.replaceFirst("^data:[^;]*;base64,?","");
        byte[] decodedImg = Base64.getDecoder()
                .decode(base64.getBytes(StandardCharsets.UTF_8));
        Path destinationFile = Paths.get(folder, fileName);
        Files.write(destinationFile, decodedImg);
        return destinationFile.toString();
    }

    public static String saveByteArrayFile(String folder, String fileName, byte[] content) throws IOException {
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
        }
        Path destinationFile = Paths.get(folder, fileName);
        Files.write(destinationFile, content);
        return destinationFile.toString();
    }

    public static String saveUrlFile(String folder, String fileName, String url) throws Exception {
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
        }
        Path destinationFile = Paths.get(folder, fileName);
        Files.write(destinationFile, urlToByteArray(url));
        return destinationFile.toString();
    }

    public static void saveInputStreamFile(String folder, String fileName, InputStream is) throws IOException {
        File directory = new File(folder);
        if (! directory.exists()){
            directory.mkdirs();
        }
        Path destinationFile = Paths.get(folder, fileName);
        Files.write(destinationFile, IOUtils.toByteArray(is));
    }

    public static void deleteFile(String folder, String fileName) throws IOException {
        Path destinationFile = Paths.get(folder, fileName);
        Files.delete(destinationFile);
    }

    public static void deleteFile(String completeFileName) throws IOException {
        Path destinationFile = Paths.get(completeFileName);
        Files.delete(destinationFile);
    }

    public static Path deleteFolder(String folder) throws IOException {
        Path destinationFile = Paths.get(folder);
        FileSystemUtils.deleteRecursively(destinationFile);
        return destinationFile;
    }

    public static void moveFolder(String folderFrom, String folderTo) throws IOException {
        Path fromFile = Paths.get(folderFrom);
        Path toFile = Paths.get(folderTo);
        FileSystemUtils.copyRecursively(fromFile, toFile);
    }

    public static boolean folderIsEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }

    public static String urlToBase64(String url) throws Exception {
        byte[] content = urlToByteArray(url);
        return Base64.getEncoder().encodeToString(content);
    }

    public static byte[] urlToByteArray(String url) throws Exception {
        InputStream is = null;
        try {
            is = convertStringToURL(url).openStream ();
            byte[] content = IOUtils.toByteArray(is);
            return content;
        }
        finally {
            if (is != null) { is.close(); }
        }
    }

    public static URL convertStringToURL(String url) throws Exception {
        if (url.startsWith("/s-link") || (url.startsWith("/server-files"))) {
            url = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() + url;
        }
        return new URL(url);
    }

}
