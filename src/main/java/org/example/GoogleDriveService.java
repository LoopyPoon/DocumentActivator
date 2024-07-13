package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveService {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Document activator";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    public static void uploadFile(Drive service, java.io.File filePath) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(filePath.getName());

        FileContent mediaContent = new FileContent("application/vnd.oasis.opendocument.spreadsheet", filePath);
        File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        System.out.println("File ID: " + file.getId());
    }

    public static void downloadFile(Drive service, String fileId, String destinationPath) throws IOException {
        OutputStream outputStream = new FileOutputStream(destinationPath);
        HttpResponse response = service.files().get(fileId).executeMedia();
        try (InputStream inputStream = response.getContent()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        outputStream.close();
        System.out.println("File downloaded to " + destinationPath);
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

        if (args.length < 1) {
            System.out.println("Please provide the path to the file to be uploaded.");
            return;
        }

        String filePathString = args[0];
        java.io.File filePath = new java.io.File(filePathString);

        if (!filePath.exists()) {
            System.out.println("File does not exist: " + filePathString);
            return;
        }

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Загрузим файл на Google Drive
//        String fileName = "Экзамен-КС-РИЗ-220909у.ods";
//        java.io.File filePath = new java.io.File("C:\\Users\\ivanh\\OneDrive\\Рабочий стол\\FilesToDisk\\" + fileName);
//        String fileId = uploadFile(service, filePath);

        // Upload the file to Google Drive
        uploadFile(service, filePath);

        // Get the file ID of the uploaded file
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }

        if (args.length >= 2) {
            String destinationPath = args[1];
            if (!destinationPath.isEmpty()) {
                String fileId = files.get(0).getId(); // Using the first file ID as an example
                downloadFile(service, fileId, destinationPath);
            }
        }

        // Скачаем файл с Google Drive
//        downloadFile(service, fileId, "C:\\Users\\ivanh\\OneDrive\\Рабочий стол\\FilesToDisk\\downloaded-" + fileName);

        // Распечатаем имена и идентификаторы 10 файлов.
//        FileList result = service.files().list()
//                .setPageSize(10)
//                .setFields("nextPageToken, files(id, name)")
//                .execute();
//        List<File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }
    }
}
