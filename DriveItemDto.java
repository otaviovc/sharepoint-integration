package com.example.sharepoint.dto;

public class DriveItemDto {

    private String id;
    private String name;
    private boolean folder;
    private Long size;
    private String webUrl;

    public DriveItemDto() {
    }

    public DriveItemDto(String id, String name, boolean folder, Long size, String webUrl) {
        this.id = id;
        this.name = name;
        this.folder = folder;
        this.size = size;
        this.webUrl = webUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}