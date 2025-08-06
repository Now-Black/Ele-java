package com.easypan.TA.InputStream;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FileNameParser {

    // 文件命名规范：CPDM-YYYYMMDDHHMMSSFFF.txt 或 JYCS-YYYYMMDDHHMMSSFFF.txt
    private static final Pattern FILE_PATTERN = Pattern.compile("^(CPDM|JYCS)-(\\d{17})\\.txt$");

    public enum FileType {
        CPDM("产品基础信息"),
        JYCS("产品运营信息");

        private final String description;

        FileType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Data
    @Builder
    public static class ParsedFileName {
        private FileType fileType;
        private String timestamp;
        private String originalFileName;
        private boolean isValid;
        private Date parseDate;
    }

    /**
     * 解析文件名
     */
    public static ParsedFileName parseFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return ParsedFileName.builder().isValid(false).build();
        }

        Matcher matcher = FILE_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            String typeStr = matcher.group(1);
            String timestamp = matcher.group(2);

            try {
                FileType fileType = FileType.valueOf(typeStr);
                Date parseDate = parseTimestamp(timestamp);

                return ParsedFileName.builder()
                        .fileType(fileType)
                        .timestamp(timestamp)
                        .originalFileName(fileName)
                        .isValid(true)
                        .parseDate(parseDate)
                        .build();
            } catch (Exception e) {
                log.warn("解析文件名时间戳异常: {}", fileName, e);
            }
        }

        return ParsedFileName.builder()
                .originalFileName(fileName)
                .isValid(false)
                .build();
    }

    /**
     * 提取时间戳字符串
     */
    public static String extractTimestamp(String fileName) {
        ParsedFileName parsed = parseFileName(fileName);
        return parsed.isValid() ? parsed.getTimestamp() : null;
    }

    /**
     * 解析时间戳为Date对象
     */
    public static Date parseTimestamp(String timestamp) {
        try {
            // YYYYMMDDHHMMSSFFF -> yyyy-MM-dd HH:mm:ss.SSS
            if (timestamp.length() != 17) {
                throw new IllegalArgumentException("时间戳长度不正确: " + timestamp);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            return sdf.parse(timestamp);
        } catch (Exception e) {
            throw new RuntimeException("解析时间戳失败: " + timestamp, e);
        }
    }

    /**
     * 比较两个文件的时间戳，返回较新的文件
     */
    public static String getNewerFile(String fileName1, String fileName2) {
        ParsedFileName parsed1 = parseFileName(fileName1);
        ParsedFileName parsed2 = parseFileName(fileName2);

        if (!parsed1.isValid() && !parsed2.isValid()) {
            return null;
        }
        if (!parsed1.isValid()) {
            return fileName2;
        }
        if (!parsed2.isValid()) {
            return fileName1;
        }

        return parsed1.getParseDate().after(parsed2.getParseDate()) ? fileName1 : fileName2;
    }
}
