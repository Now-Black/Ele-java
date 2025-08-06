package com.easypan.TA.Parser;

import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class JycsParser {
    
    private static final String FIELD_SEPARATOR = "\u000f"; // 0x0f分隔符
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    
    /**
     * 解析JYCS文件内容
     * @param filePath 文件路径
     * @return 解析后的数据
     */
    public ParsedData parseJycsFile(String filePath) {
        long startTime = System.currentTimeMillis();
        ParsedData parsedData = new ParsedData(filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                
                try {
                    // JYCS文件主要包含产品运营信息
                    ProductInfo product = parseJycsLine(line);
                    if (product != null) {
                        parsedData.addProduct(product);
                    }
                } catch (Exception e) {
                    log.error("解析JYCS文件第{}行失败: {}", lineNumber, line, e);
                }
            }
            
            parsedData.setTotalLines(lineNumber);
            parsedData.setParseTime(System.currentTimeMillis() - startTime);
            
            log.info("JYCS文件解析完成: {}", parsedData.getStatistics());
            
        } catch (IOException e) {
            log.error("读取JYCS文件失败: {}", filePath, e);
            throw new RuntimeException("读取JYCS文件失败", e);
        }
        
        return parsedData;
    }
    
    /**
     * 解析JYCS文件行（产品运营信息）
     */
    private ProductInfo parseJycsLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR);
        
        if (fields.length < 5) {
            log.warn("JYCS产品信息字段数量不足: {}", line);
            return null;
        }
        
        ProductInfo product = new ProductInfo();
        
        try {
            // JYCS文件的字段结构（根据实际格式调整）
            product.setProductCode(getField(fields, 0));
            product.setProductName(getField(fields, 1));
            product.setProductType(getField(fields, 2)); // 通常为子产品
            product.setParentCode(getField(fields, 3));
            
            // 运营相关信息
            product.setStatus(getField(fields, 4));
            
            if (fields.length > 5) {
                product.setCurrentAmount(parseBigDecimal(getField(fields, 5)));
            }
            if (fields.length > 6) {
                product.setExpectedReturn(parseBigDecimal(getField(fields, 6)));
            }
            if (fields.length > 7) {
                product.setRiskLevel(getField(fields, 7));
            }
            if (fields.length > 8) {
                product.setInvestmentType(getField(fields, 8));
            }
            if (fields.length > 9) {
                product.setEstablishDate(parseDate(getField(fields, 9)));
            }
            if (fields.length > 10) {
                product.setMaturityDate(parseDate(getField(fields, 10)));
            }
            if (fields.length > 11) {
                product.setCurrency(getField(fields, 11));
            }
            if (fields.length > 12) {
                product.setTermDays(parseInteger(getField(fields, 12)));
            }
            if (fields.length > 13) {
                product.setDescription(getField(fields, 13));
            }
            
            log.debug("解析JYCS产品信息: {} - {}", product.getProductCode(), product.getProductName());
            
        } catch (Exception e) {
            log.error("解析JYCS产品信息失败: {}", line, e);
            return null;
        }
        
        return product;
    }
    
    /**
     * 安全获取字段值
     */
    private String getField(String[] fields, int index) {
        if (index >= 0 && index < fields.length) {
            String value = fields[index];
            return StringUtils.isNotBlank(value) ? value.trim() : null;
        }
        return null;
    }
    
    /**
     * 解析BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("解析BigDecimal失败: {}", value);
            return null;
        }
    }
    
    /**
     * 解析Integer
     */
    private Integer parseInteger(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("解析Integer失败: {}", value);
            return null;
        }
    }
    
    /**
     * 解析日期
     */
    private Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            log.warn("解析日期失败: {}", value);
            return null;
        }
    }
}