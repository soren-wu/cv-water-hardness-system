package com.example.titration.module.file.controller;

import com.example.titration.common.result.R;
import com.example.titration.module.experiment.entity.ExperimentFile;
import com.example.titration.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传下载接口")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传实验文件")
    public R<ExperimentFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("experimentId") Long experimentId,
            @RequestParam(defaultValue = "KEYFRAME") String fileType) {
        return R.ok("上传成功", fileService.upload(file, experimentId, fileType));
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "下载实验文件")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ExperimentFile expFile = fileService.getFileById(id);
        Path filePath = fileService.getFilePath(id);
        Resource resource = new FileSystemResource(filePath);

        String contentType = expFile.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + expFile.getOriginalName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除实验文件")
    public R<Void> delete(@PathVariable Long id) {
        fileService.deleteFile(id);
        return R.ok("删除成功");
    }
}
