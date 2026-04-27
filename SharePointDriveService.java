package com.example.sharepoint.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.example.sharepoint.config.GraphProperties;
import com.example.sharepoint.dto.DriveItemDto;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SharePointDriveService {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    private final GraphServiceClient graphClient;
    private final TokenCredential tokenCredential;
    private final RestClient restClient;
    private final GraphProperties properties;

    public SharePointDriveService(GraphServiceClient graphClient,
                                  TokenCredential tokenCredential,
                                  RestClient restClient,
                                  GraphProperties properties) {
        this.graphClient = graphClient;
        this.tokenCredential = tokenCredential;
        this.restClient = restClient;
        this.properties = properties;
    }

    public String getDefaultDriveId() {
        validateSiteId();
        Drive drive = graphClient.sites()
                .bySiteId(properties.getSiteId())
                .drive()
                .get();

        if (drive == null || drive.getId() == null) {
            throw new IllegalStateException("Não foi possível obter o drive padrão do site.");
        }

        return drive.getId();
    }

    public List<DriveItemDto> listRoot() {
        String driveId = getDefaultDriveId();
        return listFolderByPath(driveId, "");
    }

    public List<DriveItemDto> listFolderByPath(String driveId, String folderPath) {
        String normalizedPath = normalizePath(folderPath);

        DriveItemCollectionResponse response;

        if (!StringUtils.hasText(normalizedPath)) {
            response = graphClient.drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId("root")
                    .children()
                    .get();
        } else {
            String requestUrl = properties.getBaseUrl()
                    + "/drives/" + driveId
                    + "/root:/"
                    + normalizedPath
                    + ":/children";

            ChildrenResponse raw = restClient.get()
                    .uri(requestUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .retrieve()
                    .body(ChildrenResponse.class);

            if (raw == null || raw.value == null) {
                return List.of();
            }

            List<DriveItemDto> result = new ArrayList<>();
            for (DriveItemRest item : raw.value) {
                result.add(new DriveItemDto(
                        item.id,
                        item.name,
                        item.folder != null,
                        item.size,
                        item.webUrl
                ));
            }
            return result;
        }

        List<DriveItemDto> result = new ArrayList<>();
        if (response != null && response.getValue() != null) {
            for (DriveItem item : response.getValue()) {
                result.add(new DriveItemDto(
                        item.getId(),
                        item.getName(),
                        item.getFolder() != null,
                        item.getSize(),
                        item.getWebUrl()
                ));
            }
        }

        return result;
    }

    public byte[] downloadFileFromDefaultDrive(String filePath) {
        String driveId = getDefaultDriveId();
        return downloadFile(driveId, filePath);
    }

    public byte[] downloadFile(String driveId, String filePath) {
        String normalizedPath = normalizeRequiredPath(filePath);

        String requestUrl = properties.getBaseUrl()
                + "/drives/" + driveId
                + "/root:/"
                + normalizedPath
                + ":/content";

        return restClient.get()
                .uri(requestUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .retrieve()
                .body(byte[].class);
    }

    public void uploadToDefaultDrive(String folderPath, String fileName, byte[] content) {
        String driveId = getDefaultDriveId();
        uploadFile(driveId, folderPath, fileName, content);
    }

    public void uploadFile(String driveId, String folderPath, String fileName, byte[] content) {
        Objects.requireNonNull(content, "content não pode ser null");

        String normalizedFolder = normalizePath(folderPath);
        String safeFileName = normalizeRequiredPath(fileName);

        String fullPath = StringUtils.hasText(normalizedFolder)
                ? normalizedFolder + "/" + safeFileName
                : safeFileName;

        String requestUrl = properties.getBaseUrl()
                + "/drives/" + driveId
                + "/root:/"
                + fullPath
                + ":/content";

        restClient.put()
                .uri(requestUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    private String getAccessToken() {
        AccessToken accessToken = tokenCredential.getToken(
                new TokenRequestContext().addScopes(GRAPH_SCOPE)
        ).block();

        if (accessToken == null || accessToken.isExpired()) {
            throw new IllegalStateException("Não foi possível obter access token do Microsoft Graph.");
        }

        return accessToken.getToken();
    }

    private void validateSiteId() {
        if (!StringUtils.hasText(properties.getSiteId())) {
            throw new IllegalStateException("app.microsoft.graph.site-id não foi configurado.");
        }
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeRequiredPath(String path) {
        String normalized = normalizePath(path);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Path obrigatório e não pode estar vazio.");
        }
        return normalized;
    }

    // DTOs internos para respostas REST simples
    private static class ChildrenResponse {
        public List<DriveItemRest> value;
    }

    private static class DriveItemRest {
        public String id;
        public String name;
        public Long size;
        public String webUrl;
        public FolderRest folder;
    }

    private static class FolderRest {
        public Integer childCount;
    }
}