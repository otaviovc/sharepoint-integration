package com.example.sharepoint.controller;

import com.example.sharepoint.dto.DriveItemDto;
import com.example.sharepoint.service.SharePointDriveService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/sharepoint")
public class SharePointController {

    private final SharePointDriveService sharePointDriveService;

    public SharePointController(SharePointDriveService sharePointDriveService) {
        this.sharePointDriveService = sharePointDriveService;
    }

    @GetMapping("/files")
    public List<DriveItemDto> list(@RequestParam(defaultValue = "") String path) {
        return sharePointDriveService.listFolderByPath(
                sharePointDriveService.getDefaultDriveId(),
                path
        );
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String path) {
        byte[] bytes = sharePointDriveService.downloadFileFromDefaultDrive(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(extractFileName(path)).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam(defaultValue = "") String folder,
                                         @RequestParam("file") MultipartFile file) throws Exception {
        sharePointDriveService.uploadToDefaultDrive(folder, file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.ok("Upload realizado com sucesso.");
    }

    private String extractFileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}