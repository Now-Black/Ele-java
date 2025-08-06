package com.easypan.TA.Parser;

import com.easypan.TA.Model.ParsedData;
import com.easypan.TA.Model.ProductInfo;
import com.easypan.TA.Model.ProductExtend;
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
public class CpdmParser {
    
    private static final String FIELD_SEPARATOR = "\u000f"; // 0x0f分隔符
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    
    /**
     * 解析CPDM文件内容
     * @param filePath 文件路径
     * @return 解析后的数据
     */
    public ParsedData parseCpdmFile(String filePath) {
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
                    // 判断是基础信息还是扩展信息
                    if (isProductBasicLine(line)) {
                        // 调用loadAPFile()逻辑：解析基础产品信息
                        ProductInfo product = parseProductBasicLine(line);
                        if (product != null) {
                            parsedData.addProduct(product);
                        }
                    } else if (isProductExtendLine(line)) {
                        // 调用loadAPPrdExtend()逻辑：解析产品扩展信息
                        ProductExtend extend = parseProductExtendLine(line);
                        if (extend != null) {
                            parsedData.addExtend(extend);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析文件第{}行失败: {}", lineNumber, line, e);
                }
            }
            
            parsedData.setTotalLines(lineNumber);
            parsedData.setParseTime(System.currentTimeMillis() - startTime);
            
            log.info("CPDM文件解析完成: {}", parsedData.getStatistics());
            
        } catch (IOException e) {
            log.error("读取CPDM文件失败: {}", filePath, e);
            throw new RuntimeException("读取CPDM文件失败", e);
        }
        
        return parsedData;
    }
    
    /**
     * loadAPFile()方法：解析产品基础信息行
     */
    private ProductInfo parseProductBasicLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR);
        
        if (fields.length < 8) {
            log.warn("产品基础信息字段数量不足: {}", line);
            return null;
        }
        
        ProductInfo product = new ProductInfo();
        
        try {
            // 基础字段解析（根据实际CPDM文件格式调整）
            product.setProductCode(getField(fields, 0));
            product.setProductName(getField(fields, 1));
            product.setProductType(getField(fields, 2));
            product.setParentCode(getField(fields, 3));
            
            // 金额字段
            product.setMinAmount(parseBigDecimal(getField(fields, 4)));
            product.setMaxAmount(parseBigDecimal(getField(fields, 5)));
            product.setCurrentAmount(parseBigDecimal(getField(fields, 6)));
            
            // 日期字段
            product.setEstablishDate(parseDate(getField(fields, 7)));
            if (fields.length > 8) {
                product.setMaturityDate(parseDate(getField(fields, 8)));
            }
            if (fields.length > 9) {
                product.setIssueDate(parseDate(getField(fields, 9)));
            }
            
            // 其他字段
            if (fields.length > 10) {
                product.setRiskLevel(getField(fields, 10));
            }
            if (fields.length > 11) {
                product.setStatus(getField(fields, 11));
            }
            if (fields.length > 12) {
                product.setCurrency(getField(fields, 12));
            }
            if (fields.length > 13) {
                product.setExpectedReturn(parseBigDecimal(getField(fields, 13)));
            }
            if (fields.length > 14) {
                product.setInvestmentType(getField(fields, 14));
            }
            if (fields.length > 15) {
                product.setTermDays(parseInteger(getField(fields, 15)));
            }
            
            log.debug("解析产品基础信息: {} - {}", product.getProductCode(), product.getProductName());
            
        } catch (Exception e) {
            log.error("解析产品基础信息失败: {}", line, e);
            return null;
        }
        
        return product;
    }
    
    /**
     * loadAPPrdExtend()方法：解析产品扩展信息行
     * 仅处理子产品的扩展信息
     */
    private ProductExtend parseProductExtendLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR);
        
        if (fields.length < 6) {
            log.warn("产品扩展信息字段数量不足: {}", line);
            return null;
        }
        
        ProductExtend extend = new ProductExtend();
        
        try {
            extend.setProductCode(getField(fields, 0));
            
            // 只处理子产品的扩展信息
            String productType = getField(fields, 1);
            if (!"子产品".equals(productType) && !"CHILD".equalsIgnoreCase(productType)) {
                log.debug("跳过非子产品的扩展信息: {}", extend.getProductCode());
                return null;
            }
            
            extend.setExtendField1(getField(fields, 2));
            extend.setExtendField2(getField(fields, 3));
            extend.setExtendField3(getField(fields, 4));
            extend.setExtendField4(getField(fields, 5));
            
            if (fields.length > 6) {
                extend.setExtendField5(getField(fields, 6));
            }
            if (fields.length > 7) {
                extend.setOperationStatus(getField(fields, 7));
            }
            if (fields.length > 8) {
                extend.setMarketChannel(getField(fields, 8));
            }
            if (fields.length > 9) {
                extend.setTargetCustomer(getField(fields, 9));
            }
            if (fields.length > 10) {
                extend.setRiskWarning(getField(fields, 10));
            }
            if (fields.length > 11) {
                extend.setPerformanceInfo(getField(fields, 11));
            }
            
            log.debug("解析产品扩展信息: {}", extend.getProductCode());
            
        } catch (Exception e) {
            log.error("解析产品扩展信息失败: {}", line, e);
            return null;
        }
        
        return extend;
    }
    
    /**
     * 判断是否为产品基础信息行
     */
    private boolean isProductBasicLine(String line) {
        // 根据实际业务规则判断，这里简化处理
        // 可以根据行的特征、字段数量、特定标识等判断
        return line.contains(FIELD_SEPARATOR) && !isProductExtendLine(line);
    }
    
    /**
     * 判断是否为产品扩展信息行  
     */
    private boolean isProductExtendLine(String line) {
        // 根据实际业务规则判断，比如特定的标识字段
        // 这里简化处理，可以根据实际格式调整
        return line.contains("EXTEND") || line.contains("扩展");
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