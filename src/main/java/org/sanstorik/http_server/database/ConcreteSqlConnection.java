package org.sanstorik.http_server.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ConcreteSqlConnection extends DatabaseConnection {

    /**
     * To change database type - change system variable.
     * You are allowed to use postgres, mysql etc.
     */


    /**
     * Check if user is registered in database.
     * @return id of user. If user wasn't found this returns negative number.
     */
    public int checkLogin(String username, String password) {
        int id = -1;

        try {
            PreparedStatement statement = createPreparedStatement(
                    "select id, username, password from users " +
                            "where username=? and password=?;");
            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet set = statement.executeQuery();

            if (set != null && set.next()) {
                id = set.getInt("id");
            }

            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }


    public User getUserById(int userId) {
        User user = null;

        try {
            PreparedStatement statement = createPreparedStatement(
                    "select * from users where id=?;");
            statement.setInt(1, userId);

            ResultSet set = statement.executeQuery();

            if (set != null && set.next()) {
                user = new User(
                        set.getString("username"),
                        set.getString("password"),
                        set.getString("image_url"),
                        set.getString("json_url"),
                        userId
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }


    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        try {
            PreparedStatement statement = createPreparedStatement(
                    "select * from users;"
            );

            ResultSet set = statement.executeQuery();

            if (set != null) {
                while (set.next()) {
                    users.add(new User(
                            set.getString("username"),
                            set.getString("password"),
                            set.getString("image_url"),
                            set.getString("json_url"),
                            set.getInt("id")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }


    public boolean update(int userId, String imageUrl, String jsonUrl) {
        try {
            PreparedStatement statement = createPreparedStatement(
              "update users set image_url=?, json_url=? where id=?;"
            );

            statement.setString(1, imageUrl);
            statement.setString(2, jsonUrl);
            statement.setInt(3, userId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }



    public boolean registerUser(String username, String password, String imageUrl, String jsonUrl) {
        try {
            PreparedStatement statement = createPreparedStatement(
                    "insert into users(username, password, image_url, json_url) " +
                            "values(?, ?, ?, ?);");
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, imageUrl);
            statement.setString(4, jsonUrl);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    public boolean isRegistered(String username) {
        try {
            PreparedStatement statement = createPreparedStatement(
                    "select * from users where username=?;"
            );
            statement.setString(1, username);

            ResultSet set = statement.executeQuery();
            return set != null && set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
