package com.afchecker.AFChecker.model;

import com.afchecker.AFChecker.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.vdurmont.emoji.EmojiParser;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class Rules {
    public static final int EMOJI_THRESHOLD = 3;
    public static final int NEWLINE_THRESHOLD = 4;
    public static final int EXCLAMATION_MARK_THRESHOLD = 4;
    public static final int MIXED_WORD_THRESHOLD = 3;
    public static final List<String> BLACKLIST_LIST = Arrays.asList("00$", "00 $", "активн", "ответствен", "партн", "удалён", "работ", "обуч",
            "писат", "личн", "сообщен", "крипт", "предлаг", "залив", "профит", "свобод", "сотрудничеств", "мотивац",
            "депозит", "трейдин", "пиши", " лс ");
    public static final List<String> BLACKLIST_TAIL_LIST = Arrays.asList("подроб", "пиши", " лс ", " + ", "личн", "сообщ");

    public static boolean messageCheck(
            BotConfig config,
            String messageText,
            long userId,
            long chatId,
            long messageId
    ) throws SQLException {

        log.info("Message_id={} Start message check. Chat_id={}", messageId, chatId);

        if (DbFunctions.isHuman(config, userId)) {
            log.info("Message_id={} This is human (not spam). Stop message check.", messageId);
            return false;
        }

        if (DbFunctions.duplicateMessages(config, userId, messageText)) {
            log.info("Message_id={} This is duplicate (spam) message. Stop message check.", messageId);
            return true;
        }

        long spamProbability = regexpCheck(messageText, messageId, BLACKLIST_LIST);
        long spamProbabilityTail = 0;
        if (messageText.length() > 100) {
            spamProbabilityTail = regexpCheck(messageText.substring(messageText.length() - 100), messageId, BLACKLIST_TAIL_LIST);
        }

        int isMixed = languagesMix(messageText, messageId);

        boolean isExtendedAlphabet = containsModifiedLetters(messageText, messageId);

        long addChecksSum = ((countEmoji(messageText, messageId) > EMOJI_THRESHOLD) ? 1 : 0)
                + ((countNewlines(messageText, messageId) > NEWLINE_THRESHOLD) ? 1 : 0)
                + ((countExclamationMarks(messageText, messageId) > EXCLAMATION_MARK_THRESHOLD) ? 1 : 0)
                + ((spamProbabilityTail > 0) ? 1 : 0);

        if (((isMixed > 1 || spamProbability > 0) && addChecksSum > 1)
                || addChecksSum >= 3
                || isExtendedAlphabet
                || isMixed > MIXED_WORD_THRESHOLD) {
            log.info("Message_id={} Message check done: SPAM (advanced rules).", messageId);
            return true;
        }

        log.info("Message_id={} Message check done (not spam). Chat_id={}", messageId, chatId);

        return false;
    }

    private static long regexpCheck(String messageText, long messageId, List<String> blacklist) {
        int matchPoints = 0;
        int spamProbability;

        String cleanedText = messageText.replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "").toLowerCase();
        int wordCount = cleanedText.split("\\s+").length;

        if (wordCount > 5) {
            for (String word : blacklist) {
                if (cleanedText.contains(word)) {
                    matchPoints++;
                }
            }
        }
        spamProbability = Math.round((float) (100 * matchPoints) / (wordCount + 1));
        log.info("Message_id={} Spam probability = {}.", messageId, spamProbability);

        return spamProbability;
    }

    private static int languagesMix(String messageText, long messageId) {
        int mixedWordsCount = 0;
        String russianAlphabet = "^[а-яёА-ЯЁ]+$";
        String englishAlphabet = "^[a-zA-Z]+$";
        String pattern = "[^А-яЁёA-Za-z\\s]";

        //clean message
        String text = messageText.replaceAll(pattern, "");
        String cleanedText = text.replaceAll("\\n", " ");
        //TODO: may be return count of mixed words and split this into two rules?
        for (String word : cleanedText.split(" ")) {
            if (word.length() > 4 && !word.matches(russianAlphabet) && !word.matches(englishAlphabet)) {
                mixedWordsCount++;
                if (mixedWordsCount > MIXED_WORD_THRESHOLD) {
                    log.info("Message_id={} Mixed languages.", messageId);
                    return mixedWordsCount;
                }
            }
        }

        return mixedWordsCount;
    }

    private static boolean containsModifiedLetters(String messageText, long messageId) {
        int modifiedRussianAlphabetStart = 0x1D04;
        int modifiedRussianAlphabetEnd = 0x1D2B;
        int modifiedLatinAlphabetStart = 0x1D00;
        int modifiedLatinAlphabetEnd = 0x1D7F;
        int count = 0;

        //check only first 100 characters
        for (int i = 0; i < messageText.length() && i < 100; i++) {
            int currentChar = messageText.charAt(i);
            if (currentChar >= modifiedRussianAlphabetStart && currentChar <= modifiedRussianAlphabetEnd
                    || currentChar >= modifiedLatinAlphabetStart && currentChar <= modifiedLatinAlphabetEnd) {
                count++;
            }
        }

        if (count > 2) {
            log.info("Message_id={} Extended alphabet detected.", messageId);
            return true;
        }

        return false;
    }

    private static int countEmoji(String messageText, long messageId) {
        int emojiCount = EmojiParser.extractEmojis(messageText).size();

        log.info("Message_id={} Emoji count = {}. Threshold: {}.", messageId, emojiCount, EMOJI_THRESHOLD);
        return emojiCount;
    }

    private static int countNewlines(String messageText, long messageId) {
        int newlineCount = 0;

        for (int i = 0; i < messageText.length(); i++) {
            if (messageText.charAt(i) == '\n') {
                newlineCount++;
            }
        }
        log.info("Message_id={} Newline count = {}. Threshold: {}.", messageId, newlineCount, NEWLINE_THRESHOLD);
        return newlineCount;
    }

    private static int countExclamationMarks(String messageText, long messageId) {
        int exclamationMarkCount = 0;

        for (int i = 0; i < messageText.length(); i++) {
            if (messageText.charAt(i) == '!') {
                exclamationMarkCount++;
            }
        }
        log.info("Message_id={} Exclamation mark count = {}. Threshold: {}.", messageId, exclamationMarkCount, EXCLAMATION_MARK_THRESHOLD);
        return exclamationMarkCount;
    }
}
