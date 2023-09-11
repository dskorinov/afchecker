package com.afchecker.AFChecker.service;

import com.afchecker.AFChecker.model.Rules;
import com.afchecker.AFChecker.config.BotConfig;
import com.afchecker.AFChecker.model.DbFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.SQLException;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            boolean isSpam;
            DbFunctions.setDataSource(config);

            log.info("Raw message: \n{}", update);

            try {
                long replyMessageId = (update.getMessage().getReplyToMessage() != null)
                        ? update.getMessage().getReplyToMessage().getMessageId() : 0;

                isSpam = Rules.messageCheck(config,
                        update.getMessage().getText(),
                        update.getMessage().getFrom().getId(),
                        update.getMessage().getChatId(),
                        replyMessageId,
                        update.getMessage().getMessageId()
                );
                DbFunctions.saveUser(config,
                        update.getMessage().getText(),
                        update.getMessage().getFrom().getId(),
                        update.getMessage().getMessageId(),
                        update.getMessage().getChatId(),
                        isSpam
                        );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
