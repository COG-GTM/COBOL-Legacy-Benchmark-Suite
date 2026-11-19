package com.clbs.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API users. VIEWER can run inquiries, OPERATOR can also maintain portfolios, post transactions and run batch
 * jobs (the CICS/RACF READ vs UPDATE split checked by SECMGR). Passwords come from the environment.
 */
@ConfigurationProperties(prefix = "clbs.security")
public record SecurityProperties(String viewerUsername, String viewerPassword, String operatorUsername,
                                 String operatorPassword, boolean requirePasswords) {
}
