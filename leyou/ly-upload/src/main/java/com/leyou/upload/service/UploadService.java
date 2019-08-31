package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.upload.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {

    @Autowired
    private FastFileStorageClient storageClient;

    @Autowired
    private UploadProperties properties;

    //private static final List<String> ALLOW_TYPES = Arrays.asList("image/jpeg","image/png","image/bmp","image/jpg");

    public String uploadImage(MultipartFile file) {
        try {
            //校验文件类型
            String contentType = file.getContentType();
            if (!properties.getAllowTypes().contains(contentType)){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
            //校验文件内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
            //找出后缀名   找图片名的最后一个点的后边
            // String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            //上传到FastDFS
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);
            //返回路径
            return properties.getBaseUrl()+storePath.getFullPath();
        } catch (IOException e) {
            //上传失败
            log.error("上传文件失败",e);
            throw new LyException(ExceptionEnum.UPLOAD_ERROR);
        }

    }
}
