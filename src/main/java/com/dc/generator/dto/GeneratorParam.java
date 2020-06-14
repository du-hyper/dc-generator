package com.dc.generator.dto;

import lombok.Data;

@Data
public class GeneratorParam {
    private String databaseUrl;
    private String databaseDriverName;
    private String databaseUsername;
    private String databasePassword;

    private String packageName;
    private String modelName;
    private String tableName;
}
