package com.afchecker.AFChecker.service;

import com.afchecker.AFChecker.model.Rules;
import com.afchecker.AFChecker.config.BotConfig;
import com.afchecker.AFChecker.model.DbFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

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

    public void processSpamMessage(String chatId, long replyId) {
        String text = "";
        List<ChatMember> chatAdministrators = Collections.emptyList();
        try {
            //group chats have minus at the beginning
            if (chatId.charAt(0) == '-') {
                chatAdministrators = execute(new GetChatAdministrators(chatId));
            }
        } catch (TelegramApiException e) {
            log.error("TelegramApiException error: ", e);
        }
        try {
            if (!chatAdministrators.isEmpty()) {
                for (ChatMember chatMember : chatAdministrators) {
                    if (chatMember.getUser().getUserName() != null) {
                        text += "@" + chatMember.getUser().getUserName();
                    }
                }
            }
            text += " " + config.getTagAdminsText();
            SendMessage request = new SendMessage(chatId, text);
            request.setParseMode("HTML");
            request.setReplyToMessageId(Math.toIntExact(replyId));
            execute(request);

        } catch (TelegramApiException e) {
            log.error("TelegramApiException error: ", e);
        }

    }

    @Override
    public void onUpdateReceived(Update update) {
        //check against whitelist
        if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            if (!config.getChatWhitelist().isEmpty() && !config.getChatWhitelist().contains(chatId)) {
                log.info("Skipping message. Whitelist mode is activated. Chat_id={}. Whitelist={}",
                        update.getMessage().getChatId(),
                        config.getChatWhitelist()
                );
                return;
            }
            if (update.getMessage().hasText()) {
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
                    if (isSpam) {
                        processSpamMessage(update.getMessage().getChatId().toString(),
                                update.getMessage().getMessageId());
                    }

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }
}
