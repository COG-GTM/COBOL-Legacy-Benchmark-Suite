package com.clbs.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ReportConfig {

    @Value("${report.output-directory:./reports}")
    private String outputDirectory;

    @Value("${report.date-format:yyyy-MM-dd}")
    private String dateFormat;

    @Value("${report.retention-days:90}")
    private int retentionDays;

    @PostConstruct
    public void init() throws IOException {
        Path reportDir = Paths.get(outputDirectory);
        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
        }
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public int getRetentionDays() {
        return retentionDays;
    }
}
