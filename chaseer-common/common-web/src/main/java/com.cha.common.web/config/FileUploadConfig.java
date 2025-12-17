package com.cha.common.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取文件上传配置
 * 
 * @author zyx
 */
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /**
     * 上传路径
     */
    private static String baseDir;

    /**
     * 默认大小 50M
     */
    public static long maxSize = 50 * 1024 * 1024L;

    /**
     * 默认的文件名最大长度 100
     */
    public static int nameLength = 100;

    public static long getMaxSize() {
        return maxSize;
    }

    public static void setMaxSize(long maxSize) {
        FileUploadConfig.maxSize = maxSize;
    }

    public static int getNameLength() {
        return nameLength;
    }

    public static void setNameLength(int nameLength) {
        FileUploadConfig.nameLength = nameLength;
    }

    public static String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        FileUploadConfig.baseDir = baseDir;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath() {
        return getBaseDir() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath() {
        return getBaseDir() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath() {
        return getBaseDir() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath() {
        return getBaseDir() + "/upload";
    }
}
