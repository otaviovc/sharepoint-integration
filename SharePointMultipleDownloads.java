import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SharePointMultipleDownloads {

    private final GraphServiceClient graphClient;

    public SharePointMultipleDownloads(GraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    public Map<String, byte[]> downloadFolderFilesAsMap(String sharePointFolderUrl) throws Exception {
        Map<String, byte[]> files = new HashMap<>();

        String shareToken = createSharingToken(sharePointFolderUrl);

        DriveItem folderItem = graphClient
                .shares()
                .bySharedDriveItemId(shareToken)
                .driveItem()
                .get();

        if (folderItem == null || folderItem.getId() == null || folderItem.getParentReference() == null) {
            throw new IllegalStateException("Não foi possível resolver a URL da pasta no SharePoint.");
        }

        String driveId = folderItem.getParentReference().getDriveId();
        String folderItemId = folderItem.getId();

        DriveItemCollectionResponse children = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(folderItemId)
                .children()
                .get();

        if (children == null || children.getValue() == null) {
            return files;
        }

        for (DriveItem child : children.getValue()) {

            // Ignora subpastas. Baixa apenas arquivos.
            if (child.getFile() == null) {
                continue;
            }

            String fileName = child.getName();
            String fileId = child.getId();

            InputStream inputStream = graphClient
                    .drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId(fileId)
                    .content()
                    .get();

            byte[] bytes = toByteArray(inputStream);

            files.put(fileName, bytes);
        }

        return files;
    }

    private String createSharingToken(String url) {
        String base64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(url.getBytes(StandardCharsets.UTF_8));

        return "u!" + base64;
    }

    private byte[] toByteArray(InputStream inputStream) throws Exception {
        try (inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toByteArray();
        }
    }
}