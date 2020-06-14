package com.dc.generator.web;

import com.dc.generator.config.DataSourceConfig;
import com.dc.generator.config.GlobalConfig;
import com.dc.generator.config.PackageConfig;
import com.dc.generator.config.StrategyConfig;
import com.dc.generator.config.TemplateConfig;
import com.dc.generator.config.rules.NamingStrategy;
import com.dc.generator.dto.GeneratorParam;
import com.dc.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/code/generator")
public class CodeGenerator {

    @Value("${db-url}")
    private String dbUrl;
    @Value("${db-driver-name}")
    private String dbDriverName;
    @Value("${db-username}")
    private String dbUserName;
    @Value("${db-password}")
    private String dbPassword;
    @Value("${model-name}")
    private String modelName;
    @Value("${package-name}")
    private String packageName;
    @Value("${table-name}")
    private String tableName;

    /**
     * 生成代码
     *
     * @param param    数据库与模块配置，默认从配置文件读取，如参数有值，从参数中读取
     * @param response
     */
    @RequestMapping("/")
    public void generator(GeneratorParam param, HttpServletResponse response) {
        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();
        // 全局配置
        mpg.setGlobalConfig(this.buildGlobal());
        // 数据源配置
        mpg.setDataSource(this.buildDataSource(param));
        // 包配置
        mpg.setPackageInfo(this.buildPackage(param));
        // 配置模板
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setXml(null);
        mpg.setTemplate(templateConfig);
        // 策略配置
        mpg.setStrategy(this.buildStrategy(param));
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        // 生成临时文件
        mpg.execute();
        // 打包下载
        this.zipDowland(response);

    }

    /**
     * 打包并下载zip文件
     * @param response
     */
    private void zipDowland(HttpServletResponse response) {
        log.info("----------------打包并下载文件begin---------------");
        try {
            String tempPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "temp";
            File tempFile = new File(tempPath);
            List<String> fileNameList = this.getFile(tempFile);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + URLEncoder.encode("CODE" + LocalDate.now() + ".zip", "UTF-8"));

            ZipOutputStream zipout = new ZipOutputStream(response.getOutputStream());
            for (String fileName : fileNameList) {
                File file = new File(fileName);
                InputStream bufferIn = new FileInputStream(new File(fileName));
                byte[] bs = new byte[1024];
                Arrays.fill(bs, (byte) 0);
                // 创建压缩文件内的文件【zip文件内目录重命名为com.xx.xx.controller/service/entity/mapper】
                zipout.putNextEntry(new ZipEntry(file, fileName.split(tempPath)[1]));
                int len = -1;
                while ((len = bufferIn.read(bs)) > 0) {
                    zipout.write(bs, 0, len);
                }
                bufferIn.close();
            }
            zipout.close();
            this.deleteFile(tempFile);
        } catch (IOException e) {
            log.error("打包并下载异常: {}", e);
        }
        log.info("----------------打包并下载文件end---------------");
    }

    /**
     * 递归删除临时文件
     * @param tempFile
     * @return
     */
    private Boolean deleteFile(File tempFile) {
        if(tempFile.isDirectory()) {
            File[] fileList = tempFile.listFiles();
            for(File file : fileList) {
                deleteFile(file);
            }
        }
        return tempFile.delete();
    }

    /**
     * 递归获取文件夹下所有文件
     * @param tempFile
     * @return
     */
    private List<String> getFile(File tempFile) {
        File[] fileList = tempFile.listFiles();
        List<String> fileNames = new ArrayList<>();
        for (File file : fileList) {
            if (file.isDirectory()) {
                fileNames.addAll(getFile(file));
            }
            if (file.isFile()) {
                fileNames.add(file.getPath());
            }
        }
        return fileNames;
    }

    /**
     * 全局配置
     */
    private GlobalConfig buildGlobal() {
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "temp");
        gc.setAuthor("dc");
        gc.setOpen(false);
        gc.setSwagger2(true); // 实体属性 Swagger2 注解
        return gc;
    }

    /**
     * 数据源配置
     */
    private DataSourceConfig buildDataSource(GeneratorParam param) {
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl(StringUtils.isBlank(param.getDatabaseUrl()) ? dbUrl : param.getDatabaseUrl());
        dsc.setDriverName(StringUtils.isBlank(param.getDatabaseDriverName()) ? dbDriverName : param.getDatabaseDriverName());
        dsc.setUsername(StringUtils.isBlank(param.getDatabaseUsername()) ? dbUserName : param.getDatabaseUsername());
        dsc.setPassword(StringUtils.isBlank(param.getDatabasePassword()) ? dbPassword : param.getDatabasePassword());
        return dsc;
    }

    /**
     * 包配置
     */
    private PackageConfig buildPackage(GeneratorParam param) {
        PackageConfig pc = new PackageConfig();
        pc.setModuleName(StringUtils.isBlank(param.getModelName()) ? modelName : param.getModelName());
        pc.setParent(StringUtils.isBlank(param.getPackageName()) ? packageName : param.getPackageName());
        return pc;
    }

    /**
     * 策略配置
     */
    private StrategyConfig buildStrategy(GeneratorParam param) {
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(true);
        // 写于父类中的公共字段
        strategy.setSuperEntityColumns("id");
        strategy.setInclude((StringUtils.isBlank(param.getTableName()) ? tableName : param.getTableName()).split(","));
        strategy.setControllerMappingHyphenStyle(true);
        strategy.setTablePrefix(StringUtils.isBlank(param.getModelName()) ? modelName : param.getModelName() + "_");
        return strategy;
    }
}
