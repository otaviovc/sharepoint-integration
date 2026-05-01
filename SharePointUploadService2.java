import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class SharePointUploadService {

    private final GraphServiceClient graphClient;

    public SharePointUploadService(GraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    public DriveItem uploadUsingFullUrl(
        byte[] fileBytes,
        String fileName,
        String pathOfFileInSharepoint
) {

    String shareId = GraphUtils.toShareId(pathOfFileInSharepoint);

    // 1. Resolve URL → DriveItem
    DriveItem existingItem = graphClient
            .shares()
            .bySharedDriveItemId(shareId)
            .driveItem()
            .get();

    String driveId = existingItem.getParentReference().getDriveId();

    // Detect if URL is folder or file
    String parentId = existingItem.getFolder() != null
            ? existingItem.getId()
            : existingItem.getParentReference().getId();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);

    // 2. Upload using path syntax (NEW WAY)
    return graphClient
            .drives()
            .byDriveId(driveId)
            .items()
            .byDriveItemId(parentId)
            .withUrl(
                graphClient.getBaseUrl()
                + "/drives/" + driveId
                + "/items/" + parentId
                + ":/" + fileName + ":/content"
            )
            .put(inputStream);
}
}