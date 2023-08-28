package com.afchecker.AFChecker.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    @Value("${spring.datasource.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String dataSourceDriverClassName;

    @Value("${spring.datasource.username}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password}")
    private String dataSourcePassword;

    @Value("${chat_whitelist}")
    private String chatWhitelist;

    @Value("${mode}")
    private String mode;

    @Value("${spam_processing}")
    private String spamProcessing;

    @Value("${tag_admins_text}")
    private String tagAdminsText;

    @Value("${data.source.table.prefix.msg}")
    private String dataSourceTablePrefixMsg;

    @Value("${data.source.table.prefix.usr}")
    private String dataSourceTablePrefixUsr;

}
