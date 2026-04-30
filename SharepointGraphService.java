import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Site;
import com.microsoft.graph.models.DriveCollectionResponse;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class SharepointGraphService {

    private final GraphServiceClient graphClient;
    private final SharepointProperties props;

    public SharepointGraphService(GraphServiceClient graphClient, SharepointProperties props) {
        this.graphClient = graphClient;
        this.props = props;
    }

    public Site getSite() {
        String siteKey = props.hostname() + ":" + props.sitePath();

        return graphClient
                .sites()
                .bySiteId(siteKey)
                .get();
    }

    public List<Drive> listDrives() {
        Site site = getSite();

        DriveCollectionResponse response = graphClient
                .sites()
                .bySiteId(site.getId())
                .drives()
                .get();

        return response.getValue();
    }

    public List<DriveItem> listRootFiles(String driveId) {
        DriveItemCollectionResponse response = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root")
                .children()
                .get();

        return response.getValue();
    }

    public InputStream downloadFile(String driveId, String itemId) {
        return graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(itemId)
                .content()
                .get();
    }

    public DriveItem uploadSmallFile(
            String driveId,
            String folderItemId,
            String fileName,
            byte[] content
    ) {
        return graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(folderItemId)
                .itemWithPath(fileName)
                .content()
                .put(content);
    }
	
	public Drive getDriveByName(String driveName) {
		Site site = getSite();

		DriveCollectionResponse response = graphClient
				.sites()
				.bySiteId(site.getId())
				.drives()
				.get();

		return response.getValue()
				.stream()
				.filter(d -> driveName.equalsIgnoreCase(d.getName()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Drive not found: " + driveName));
	}
}