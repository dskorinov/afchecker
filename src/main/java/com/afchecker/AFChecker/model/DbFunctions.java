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

    public static void saveUser(BotConfig config,
                                   String messageText,
                                   long userId,
                                   long messageId,
                                   long chatId,
                                   boolean isSpam) throws SQLException {
        if (isSpam) {
            return;
        }
        String tablePrefixUsr = config.getDataSourceTablePrefixUsr();
        Connection connection = dataSource.getConnection();

        String createTableQuery = "CREATE TABLE IF NOT EXISTS \"" + tablePrefixUsr + "_main\"" +
                "(user_id INTEGER PRIMARY KEY, " +
                "message_hash TEXT, " +
                "human INTEGER, " +
                "messages_count INTEGER, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        connection.createStatement().execute(createTableQuery);
        try {

            // check if user exists
            String selectQuery = "SELECT user_id, messages_count, human, message_hash FROM \""
                    + tablePrefixUsr + "_main\" WHERE user_id = ?";
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            selectStatement.setLong(1, userId);
            ResultSet result = selectStatement.executeQuery();
            String messageHash = Integer.toString(messageText.substring(0, Math.min(1000, messageText.length())).hashCode());

            if (result.next()) {
                int existingUserId = result.getInt(1);
                int messagesCount = result.getInt(2) + 1;
                int human = result.getInt(3);

                if (messagesCount < 20) {
                    messageHash = result.getString(4) + "," + messageHash;
                } else {
                    human = 1;
                }

                String updateQuery = "REPLACE INTO " + tablePrefixUsr + "_main " +
                        "(user_id, message_hash, human, messages_count) VALUES (?, ?, ?, ?)";
                PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                updateStatement.setInt(1, existingUserId);
                updateStatement.setString(2, messageHash);
                updateStatement.setInt(3, human);
                updateStatement.setInt(4, messagesCount);
                updateStatement.executeUpdate();
            } else {
                // new user: messages_count = 0, human = 0
                String insertQuery = "REPLACE INTO " + tablePrefixUsr + "_main " +
                        "(user_id, message_hash, human, messages_count) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                insertStatement.setLong(1, userId);
                insertStatement.setString(2, messageHash);
                insertStatement.setInt(3, 0);
                insertStatement.setInt(4, 0);
                insertStatement.executeUpdate();
            }

            connection.close();
        } catch (SQLException e) {
            log.error("Database error: ", e);

        }

    }

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

        String selectQuery = "SELECT message_hash FROM " + config.getDataSourceTablePrefixUsr() + "_main " +
                "WHERE user_id = ?";
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

