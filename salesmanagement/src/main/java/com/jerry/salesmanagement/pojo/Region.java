package com.jerry.salesmanagement.pojo;

import lombok.Data;

@Data
public class Region {
    private Integer id;         // 对应 MEDIUMINT(7)
    private Byte level;         // TINYINT(1)
    private Long parentCode;    // BIGINT(14)
    private Long areaCode;      // BIGINT(14)
    private String zipCode;     // VARCHAR(6)  => '000000'
    private String cityCode;    // CHAR(6)
    private String name;        
    private String shortName;   
    private String mergerName;  
    private String pinyin;      
    private Double lng;         
    private Double lat;         
}
