package edu.whimc.observations.utils.sql;

import edu.whimc.observations.Observations;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.observetemplate.models.ObservationTemplate;
import edu.whimc.observations.observetemplate.models.ObservationType;
import edu.whimc.observations.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.function.Consumer;

/**
 * Handles storing position data
 *
 * @author Jack Henhapl
 */
public class Queryer {

    /**
     * Query for inserting an observation into the database.
     */
    private static final String QUERY_SAVE_OBSERVATION =
            "INSERT INTO whimc_observations " +
                    "(time, uuid, username, world, x, y, z, yaw, pitch, observation, active, expiration, category) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * Query for getting all observations from the database.
     */
    private static final String QUERY_GET_ACTIVE_OBSERVATIONS =
            "SELECT * " +
                    "FROM whimc_observations " +
                    "WHERE active = 1 AND (expiration IS NULL OR (expiration - time > 0))";

    /**
     * Query for making an observation inactive.
     */
    private static final String QUERY_MAKE_OBSERVATION_INACTIVE =
            "UPDATE whimc_observations " +
                    "SET active=0 " +
                    "WHERE rowid=? AND active=1";

    private static final String QUERY_MAKE_PLAYER_OBSERVATIONS_INACTIVE =
            "UPDATE whimc_observations " +
                    "SET active=0 " +
                    "WHERE username=? AND active=1";

    private static final String QUERY_MAKE_WORLD_OBSERVATIONS_INACTIVE =
            "UPDATE whimc_observations " +
                    "SET active=0 " +
                    "WHERE active=1 AND world=?";
    private static final String QUERY_MAKE_OBSERVATIONS_INACTIVE =
            "UPDATE whimc_observations " +
                    "SET active=0 " +
                    "WHERE username=? AND active=1 AND world=?";

    private static final String QUERY_MAKE_EXPIRED_INACTIVE =
            "UPDATE whimc_observations " +
                    "SET active=0 " +
                    "WHERE ? > expiration";

    private static final String QUERY_SET_EXPIRATION =
            "UPDATE whimc_observations " +
                    "SET expiration=? " +
                    "WHERE rowid=?";

    private static final String QUERY_GET_INACTIVE_ID =
            "SELECT * " +
                    "FROM whimc_observations " +
                    "WHERE rowid=?";

    private static final String QUERY_GET_INACTIVE_RANGE =
            "SELECT * " +
                    "FROM whimc_observations " +
                    "WHERE rowid BETWEEN ? AND ?";

    private static final String QUERY_GET_INACTIVE_TIME =
            "SELECT * " +
                    "FROM whimc_observations " +
                    "WHERE time BETWEEN ? AND ?";

    private final Observations plugin;
    private final MySQLConnection sqlConnection;

    public Queryer(Observations plugin, Consumer<Queryer> callback) {
        this.plugin = plugin;
        this.sqlConnection = new MySQLConnection(plugin);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean success = sqlConnection.initialize();
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success ? this : null));
        });
    }

    /**
     * Generated a PreparedStatement for saving a new observation.
     *
     * @param connection MySQL Connection
     * @param obs        Observation to save
     * @return PreparedStatement
     * @throws SQLException
     */
    private PreparedStatement getStatement(Connection connection, Observation obs) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(QUERY_SAVE_OBSERVATION, Statement.RETURN_GENERATED_KEYS);

        String category = null;
        if (obs.getTemplate() != null) {
            category = obs.getTemplate().getType().name();
        }

        Location loc = obs.getViewLocation();
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();

        statement.setLong(1, obs.getTimestamp().getTime());
        statement.setString(2, Bukkit.getPlayer(obs.getPlayer()).getUniqueId().toString());
        statement.setString(3, obs.getPlayer());
        statement.setString(4, world);
        statement.setDouble(5, x);
        statement.setDouble(6, y);
        statement.setDouble(7, z);
        statement.setFloat(8, yaw);
        statement.setFloat(9, pitch);
        statement.setString(10, obs.getObservation());
        statement.setBoolean(11, true);
        statement.setLong(12, obs.getExpiration().getTime());
        statement.setString(13, category);

        return statement;
    }

    private void loadObservationFromResultSet(ResultSet results, int id, boolean isTemporary) throws SQLException {
        Timestamp timestamp = new Timestamp(results.getLong("time"));
        String name = results.getString("username");
        String worldName = results.getString("world");
        double x = results.getDouble("x");
        double y = results.getDouble("y");
        double z = results.getDouble("z");
        float yaw = results.getFloat("yaw");
        float pitch = results.getFloat("pitch");
        String observation = results.getString("observation");
        long expNum = results.getLong("expiration");
        Timestamp expiration = expNum == 0 ? null : new Timestamp(expNum);
        String category = results.getString("category");
        ObservationTemplate template = category == null ? null : this.plugin.getTemplateManager().getTemplate(ObservationType.valueOf(category));

        sync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Utils.debug("  - " + id + " | world '" + worldName + "' not found -> skipping");
                return;
            }
            Location loc = new Location(world, x, y, z, yaw, pitch);

            Utils.debug("  - " + id +
                    " | " + timestamp.getTime() +
                    " | " + name +
                    " | (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")" +
                    " | " + observation + " | " +
                    " | " + (expiration == null ? "n/a" : expiration.getTime()));
            Observation.loadObservation(this.plugin, id, timestamp, name, loc, observation, expiration, template, isTemporary);
        });
    }

    /**
     * Stores an observation into the database and returns the obervation's ID
     *
     * @param observation Observation to save
     * @param callback    Function to call once the observation has been saved
     */
    public void storeNewObservation(Observation observation, Consumer<Integer> callback) {
        async(() -> {
            Utils.debug("Storing observation to database:");

            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = getStatement(connection, observation)) {
                    String query = statement.toString().substring(statement.toString().indexOf(" ") + 1);
                    Utils.debug("  " + query);
                    statement.executeUpdate();

                    try (ResultSet idRes = statement.getGeneratedKeys()) {
                        idRes.next();
                        int id = idRes.getInt(1);

                        Utils.debug("Observation saved with id " + id + ".");
                        sync(callback, id);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Loads observations from the database.
     */
    public void loadObservations(Runnable callback) {
        async(() -> {
            Utils.debug("Loading observations:");
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    Utils.debug("  " + QUERY_GET_ACTIVE_OBSERVATIONS);
                    try (ResultSet results = statement.executeQuery(QUERY_GET_ACTIVE_OBSERVATIONS)) {
                        Utils.debug("Observations found:");
                        while (results.next()) {
                            int id = results.getInt("rowid");
                            loadObservationFromResultSet(results, id, false);
                        }
                        sync(callback);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Makes an observation inactive in the database.
     *
     * @param id Id of the observation
     */
    public void makeSingleObservationInactive(int id, Runnable callback) {
        Utils.debug("Making observation id " + id + " inactive:");
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_MAKE_OBSERVATION_INACTIVE)) {
                    statement.setInt(1, id);

                    Utils.debug("  " + QUERY_MAKE_OBSERVATION_INACTIVE);
                    statement.executeUpdate();
                    Utils.debug("Observation set as inactive.");
                    sync(callback);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    public String getInactiveQuery(String world, String player) {
        if (player == null) {
            return QUERY_MAKE_WORLD_OBSERVATIONS_INACTIVE;
        }
        if (world == null) {
            return QUERY_MAKE_PLAYER_OBSERVATIONS_INACTIVE;
        }
        return QUERY_MAKE_OBSERVATIONS_INACTIVE;
    }

    public void makeObservationsInactive(String world, String player, Consumer<Integer> callback) {
        String query = getInactiveQuery(world, player);

        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    int ind = 1;
                    if (player != null) {
                        statement.setString(ind++, player);
                    }
                    if (world != null) {
                        statement.setString(ind, world);
                    }
                    sync(callback, statement.executeUpdate());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void makeExpiredObservationsInactive(Consumer<Integer> callback) {
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_MAKE_EXPIRED_INACTIVE)) {
                    statement.setLong(1, System.currentTimeMillis());
                    sync(callback, statement.executeUpdate());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void reactivateObservations(Timestamp start, Timestamp end, Consumer<Integer> callback) {
        loadTemporaryObservation(QUERY_GET_INACTIVE_TIME, statement -> {
            try {
                statement.setLong(1, start.getTime());
                statement.setLong(2, end.getTime());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, callback);
    }

    public void reactivateObservations(int startId, int endId, Consumer<Integer> callback) {
        loadTemporaryObservation(QUERY_GET_INACTIVE_RANGE, statement -> {
            try {
                statement.setInt(1, startId);
                statement.setInt(2, endId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, callback);
    }

    public void reactivateObservation(int id, Consumer<Integer> callback) {
        loadTemporaryObservation(QUERY_GET_INACTIVE_ID, statement -> {
            try {
                statement.setInt(1, id);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, callback);
    }

    private void loadTemporaryObservation(String query, Consumer<PreparedStatement> prepare, Consumer<Integer> callback) {
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    prepare.accept(statement);
                    try (ResultSet results = statement.executeQuery()) {
                        int count = 0;
                        while (results.next()) {
                            int id = results.getInt("rowid");
                            if (Observation.getObservation(id) != null) {
                                continue;
                            }
                            count++;
                            loadObservationFromResultSet(results, id, true);
                        }
                        sync(callback, count);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void setExpiration(int id, Timestamp newExpiration, Runnable callback) {
        async(() -> {
            try (Connection connection = this.sqlConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(QUERY_SET_EXPIRATION)) {
                    statement.setObject(1, newExpiration == null ? null : newExpiration.getTime(), Types.BIGINT);
                    statement.setInt(2, id);
                    statement.executeUpdate();
                    sync(callback);
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        });
    }

    private <T> void sync(Consumer<T> cons, T val) {
        Bukkit.getScheduler().runTask(this.plugin, () -> cons.accept(val));
    }

    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(this.plugin, runnable);
    }

    private void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }


}
