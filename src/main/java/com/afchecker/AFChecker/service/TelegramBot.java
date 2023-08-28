package com.afchecker.AFChecker.service;

import com.afchecker.AFChecker.model.Rules;
import com.afchecker.AFChecker.config.BotConfig;
import com.afchecker.AFChecker.model.DbFunctions;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.SQLException;

@Component
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
            String messageText = update.getMessage().getText();
            Boolean result;
            DbFunctions.setDataSource(config);
            System.out.println(update);
            try {
                long replyMessageId = 0;
                if( update.getMessage().getReplyToMessage() != null) {
                    replyMessageId = update.getMessage().getReplyToMessage().getMessageId();
                }

                result = Rules.messageCheck(config,
                        messageText,
                        update.getMessage().getFrom().getId(),
                        update.getMessage().getChatId(),
                        replyMessageId,
                        update.getMessage().getMessageId()
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            System.out.println(result);
        }

    }
}
