package com.afchecker.AFChecker.model;
import com.afchecker.AFChecker.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class Rules {


    public static boolean messageCheck(BotConfig config, String messageText, long userId, long chatId, long replyId, long messageId) throws SQLException {
        log.info("Message_id={} Start message check. Chat_id={}", messageId, chatId);
        boolean isMixed;

        if (DbFunctions.isHuman(config, userId)) {
            log.info("Message_id={} This is human (not spam). Stop message check.", messageId);
            return false;
        }

        if (DbFunctions.duplicateMessages(config, userId, messageText)) {
            log.info("Message_id={} This is duplicated (spam) message. Stop message check.", messageId);
            return true;
        }

        long spamProbability = 0;
        spamProbability += 10 * regexpCheck(messageText);
        log.info("Message_id={} Spam probability = {}.", messageId, spamProbability);

        isMixed = languagesMix(messageText, messageId);

        boolean isExtendedAlphabet = containsModifiedLetters(messageText, messageId);

        long addChecksSum = countUnicodeCharacters(messageText, messageId)
                + countNewlines(messageText, messageId)
                + countExclamationMarks(messageText, messageId);

        if (((isMixed || spamProbability > 0) && addChecksSum > 1) || addChecksSum == 3 || isExtendedAlphabet) {
            log.info("Message_id={} Message check done: SPAM (advanced rules).", messageId);
            return true;
        }

        log.info("Message_id={} Message check done. Chat_id={}", messageId, chatId);

        return false;
    }

    private static long regexpCheck(String messageText) {
        int matchPoints = 0;
        String cleanedText = messageText.replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "").toLowerCase();
        int wordCount = cleanedText.split("\\s+").length;

        List<String> blacklist = Arrays.asList("00$", "00 $", "активн", "ответствен", "партн", "удалён", "работ", "обуч",
                "писат", "личн", "сообщен", "крипт", "предлаг", "залив", "профит", "свобод", "сотрудничеств", "мотивац",
                "депозит", "трейдин", "пиши");

        if (wordCount > 5) {
            for (String word : blacklist) {
                matchPoints += (cleanedText.length() - cleanedText.replace(word, "").length()) / word.length();
            }
        }

        return Math.round(10.0 * matchPoints / (wordCount + 1.0));
    }

    private static boolean languagesMix(String messageText, long messageId) {
        String russianAlphabet = "^[а-яёА-ЯЁ]+$";
        String englishAlphabet = "^[a-zA-Z]+$";
        String pattern = "[^А-яЁёA-Za-z\\s]";
        String text = messageText.replaceAll(pattern, "");
        String cleanedText = text.replaceAll("\\n", " ");
        int mixedWordsCount = 0;

        for (String word : cleanedText.split(" ")) {
            if (word.length() > 4 && !word.matches(russianAlphabet) && !word.matches(englishAlphabet)) {
                mixedWordsCount++;
                if (mixedWordsCount > 1) {
                    log.info("Message_id={} Mixed languages.", messageId);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean containsModifiedLetters(String messageText, long messageId) {
        int[] modifiedRussianAlphabet = new int[]{0x1D04, 0x1D2B};
        int[] modifiedLatinAlphabet = new int[]{0x1D00, 0x1D7F};
        int count = 0;

        for (int i = 0; i < messageText.length() && i < 100; i++) {
            char currentChar = messageText.charAt(i);
            if (contains(modifiedRussianAlphabet, currentChar) || contains(modifiedLatinAlphabet, currentChar)) {
                count++;
            }
        }

        if (count > 3) {
            log.info("Message_id={} Extended alphabet detected.", messageId);
            return true;
        }

        return false;
    }

    private static boolean contains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    private static int countUnicodeCharacters(String messageText, long messageId) {
        int unicodeCharacterCount = 0;
        for (int i = 0; i < messageText.length(); i++) {
            if (Character.isLetterOrDigit(messageText.charAt(i)) && !Character.isAlphabetic(messageText.charAt(i))) {
                unicodeCharacterCount++;
            }
        }
        log.info("Message_id={} Unicode character count = {}.", messageId, unicodeCharacterCount);
        return unicodeCharacterCount;
    }

    private static int countNewlines(String messageText, long messageId) {
        int newlineCount = messageText.length() - messageText.replace("\n", "").length();
        log.info("Message_id={} Newline count = {}.", messageId, newlineCount);
        return newlineCount;
    }

    private static int countExclamationMarks(String messageText, long messageId) {
        int exclamationMarkCount = messageText.length() - messageText.replace("!", "").length();
        log.info("Message_id={} Exclamation mark count = {}.", messageId, exclamationMarkCount);
        return exclamationMarkCount;
    }
}
