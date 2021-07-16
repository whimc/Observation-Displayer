package edu.whimc.observations.utils.sql;

import edu.whimc.observations.Observations;
import edu.whimc.observations.utils.sql.migration.SchemaManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLConnection {

    public static final String DRIVER_CLASS = "com.mysql.jdbc.Driver";
    public static final String URL_TEMPLATE = "jdbc:mysql://%s:%s/%s";

    private Connection connection;
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final String url;
    private final int port;

    private final Observations plugin;

    public MySQLConnection(Observations plugin) {
        this.host = plugin.getConfig().getString("mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("mysql.port", 3306);
        this.database = plugin.getConfig().getString("mysql.database", "minecraft");
        this.username = plugin.getConfig().getString("mysql.username", "user");
        this.password = plugin.getConfig().getString("mysql.password", "pass");

        this.url = String.format(URL_TEMPLATE, this.host, this.port, this.database);

        this.plugin = plugin;
    }

    public boolean initialize() {
        if (getConnection() == null) {
            return false;
        }

        SchemaManager manager = new SchemaManager(this.plugin, this.connection);
        return manager.initialize();
    }

    public Connection getConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                return this.connection;
            }

            Class.forName(DRIVER_CLASS);
            this.connection = DriverManager.getConnection(this.url, this.username, this.password);
        } catch (SQLException | ClassNotFoundException e) {
            return null;
        }

        return this.connection;
    }

}
