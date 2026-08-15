package com.example.titration.module.file.service;

import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.experiment.entity.ExperimentFile;
import com.example.titration.module.experiment.mapper.ExperimentFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final ExperimentFileMapper experimentFileMapper;

    @Value("${file.storage.root-path:./storage}")
    private String rootPath;

    @Value("${file.storage.max-file-size:104857600}")
    private long maxFileSize;

    public ExperimentFile upload(MultipartFile file, Long experimentId, String fileType) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(400,
                    "文件大小超过限制（最大 " + (maxFileSize / 1024 / 1024) + " MB）");
        }
        try {
            String dateDir = LocalDate.now().toString().replace("-", "");
            Path storageDir = Paths.get(rootPath, "experiments", dateDir).toAbsolutePath();
            File dir = storageDir.toFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException("创建存储目录失败");
            }
            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String uniqueName = UUID.randomUUID().toString().substring(0, 8)
                    + "_" + originalName;
            Path filePath = storageDir.resolve(uniqueName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ExperimentFile experimentFile = new ExperimentFile();
            experimentFile.setExperimentId(experimentId);
            experimentFile.setFileType(fileType);
            experimentFile.setOriginalName(originalName);
            experimentFile.setStoragePath(filePath.toString().replace("\\", "/"));
            experimentFile.setContentType(file.getContentType());
            experimentFile.setFileSize(file.getSize());
            experimentFileMapper.insert(experimentFile);

            return experimentFile;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public ExperimentFile getFileById(Long id) {
        ExperimentFile expFile = experimentFileMapper.selectById(id);
        if (expFile == null) {
            throw new BusinessException(404, "文件不存在");
        }
        return expFile;
    }

    public Path getFilePath(Long id) {
        ExperimentFile expFile = getFileById(id);
        Path filePath = Paths.get(expFile.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "文件不存在");
        }
        return filePath;
    }

    public void deleteFile(Long id) {
        ExperimentFile expFile = getFileById(id);
        try {
            Files.deleteIfExists(Paths.get(expFile.getStoragePath()));
        } catch (IOException e) {
            log.warn("删除物理文件失败: {}", expFile.getStoragePath());
        }
        experimentFileMapper.deleteById(id);
    }
}
