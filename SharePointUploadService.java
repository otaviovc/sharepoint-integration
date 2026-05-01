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
            String title,
            String fileName,
            String pathOfFileInSharepoint
    ) {

        // 1. Convert URL → shareId
        String shareId = GraphUtils.toShareId(pathOfFileInSharepoint);

        // 2. Resolve URL → DriveItem
        DriveItem existingItem = graphClient
                .shares()
                .bySharedDriveItemId(shareId)
                .driveItem()
                .get();

        String driveId = existingItem.getParentReference().getDriveId();
        String parentId = existingItem.getParentReference().getId();

        // 3. Convert byte[] → InputStream
        InputStream inputStream = new ByteArrayInputStream(fileBytes);

        // 4. Upload file
        DriveItem uploadedItem = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(parentId)
                .itemWithPath(fileName)
                .content()
                .put(inputStream);

        // 5. (Optional) Update metadata (title / name)
        if (title != null && !title.isEmpty()) {
            DriveItem update = new DriveItem();
            update.setName(title);

            uploadedItem = graphClient
                    .drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId(uploadedItem.getId())
                    .patch(update);
        }

        return uploadedItem;
    }
}