package com.afchecker.AFChecker.model;

import com.afchecker.AFChecker.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class DbFunctions {

    private static DataSource dataSource;

    public static void setDataSource(BotConfig config) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(config.getDataSourceUrl());
        DbFunctions.dataSource = dataSource;
    }
    public static boolean saveMessage(BotConfig config, String messageText, int userId, int messageId, int chatId, int isSpam) throws SQLException {
        if (isSpam == 1) {
            String tablePrefixMsg = config.getDataSourceTablePrefixMsg();
            Connection connection = dataSource.getConnection();

            String createTableQuery = "CREATE TABLE IF NOT EXISTS \"" + tablePrefixMsg + chatId + "\" " +
                    "(message_id INTEGER PRIMARY KEY, user_id INTEGER, message_text TEXT, deleted INTEGER, is_spam INTEGER)";
            connection.createStatement().execute(createTableQuery);

            String replaceQuery = "REPLACE INTO \"" + tablePrefixMsg + chatId + "\" (message_id, user_id, message_text, deleted, is_spam) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(replaceQuery)) {
                preparedStatement.setInt(1, messageId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, "");
                preparedStatement.setInt(4, 0);
                preparedStatement.setInt(5, isSpam);
                preparedStatement.executeUpdate();
            }
        }
        return true;
    }
/*
    public static boolean saveUser(BotConfig config, String messageText, int userId, int messageId, int chatId, int isSpam) throws SQLException {
        if (isSpam == 1) {

        }
        return false;
    }
*/
    public static boolean isHuman(BotConfig config, long userId) throws SQLException {

        Connection connection = dataSource.getConnection();

        String createTableQuery = "CREATE TABLE IF NOT EXISTS \"" + config.getDataSourceTablePrefixUsr() + "_main\" " +
                "(user_id INTEGER PRIMARY KEY, message_hash TEXT, human INTEGER, messages_count INTEGER)";
        connection.createStatement().execute(createTableQuery);

        String selectQuery = "SELECT * FROM \"" + config.getDataSourceTablePrefixUsr() + "_main\" WHERE user_id = ? AND human = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, 1);
            ResultSet result = preparedStatement.executeQuery();
            return result.next();
        }
    }

    public static boolean duplicateMessages(BotConfig config, long userId, String messageText) throws SQLException {
        Connection connection = dataSource.getConnection();

        String selectQuery = "SELECT message_hash FROM \"" + config.getDataSourceTablePrefixUsr() + "_main\" WHERE user_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
            preparedStatement.setLong(1, userId);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                String messageHash = result.getString("message_hash");
                if (messageHash.contains(Integer.toString(messageText.substring(0, Math.min(messageText.length(), 1000)).hashCode()))) {
                    return true;
                }
            }
        }
        return false;
    }
}

