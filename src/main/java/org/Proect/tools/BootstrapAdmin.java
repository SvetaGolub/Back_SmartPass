package org.Proect.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BootstrapAdmin {
    private static final String LOCK_NAME = "smartpass_initial_admin_bootstrap";
    private static final Pattern MYSQL_URL = Pattern.compile("^jdbc:mysql://([^/:?#]+):(\\d+)/([^?#]+)(?:\\?.*)?$");

    private BootstrapAdmin() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            runSelfTest();
            return;
        }
        if (args.length != 1 || !"--confirm-empty-local-smartpass".equals(args[0])) {
            throw new IllegalArgumentException("Run the local PowerShell script with -ConfirmEmptyDatabase.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("No interactive terminal is available. Run this script from a local PowerShell window.");
        }

        String url = environment("DB_URL", "jdbc:mysql://localhost:3306/SmartPass?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false");
        verifyLocalTarget(url);
        String dbUser = environment("DB_USER", "root");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD is not available in the process environment.");
        }

        String confirmation = console.readLine("Type CREATE INITIAL ADMIN to continue: ");
        if (!"CREATE INITIAL ADMIN".equals(confirmation)) {
            console.printf("Cancelled; no account was created.%n");
            return;
        }

        String login = required(console, "Application login: ");
        String firstName = required(console, "First name: ");
        String lastName = required(console, "Last name: ");
        String middleName = console.readLine("Middle name (optional): ").trim();
        String phone = required(console, "Phone: ");
        String email = required(console, "Email: ");
        String position = required(console, "Position: ");
        String address = required(console, "Address: ");
        LocalDate birthday = date(console, "Birth date (YYYY-MM-DD): ");
        LocalDate stage = date(console, "Work start date (YYYY-MM-DD): ");
        char[] password = console.readPassword("Application password (8-72 UTF-8 bytes): ");
        char[] repeated = console.readPassword("Repeat application password: ");
        try {
            validatePassword(password, repeated);
            createAdmin(url, dbUser, dbPassword, login, firstName, lastName, middleName,
                    phone, email, position, address, birthday, stage, password, console);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (repeated != null) {
                Arrays.fill(repeated, '\0');
            }
        }
    }

    private static void createAdmin(String url, String dbUser, String dbPassword,
                                    String login, String firstName, String lastName, String middleName,
                                    String phone, String email, String position, String address,
                                    LocalDate birthday, LocalDate stage, char[] password,
                                    Console console) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            boolean lockHeld = false;
            try {
                try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
                    statement.setString(1, LOCK_NAME);
                    try (ResultSet result = statement.executeQuery()) {
                        lockHeld = result.next() && result.getInt(1) == 1;
                    }
                }
                if (!lockHeld) {
                    throw new IllegalStateException("Another initial-admin bootstrap is already running.");
                }

                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
                     ResultSet result = statement.executeQuery()) {
                    result.next();
                    if (result.getLong(1) != 0) {
                        throw new IllegalStateException("Bootstrap refused: users table is not empty.");
                    }
                }

                int adminRoleId = roleId(connection);
                int zoneId = chooseZone(connection, console);
                String hash = new BCryptPasswordEncoder().encode(CharBuffer.wrap(password));
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users
                            (role_id, zone_id, first_name, last_name, middle_name, number_phone,
                             email, position, address, birthday, stage, login, password_hash, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """)) {
                    statement.setInt(1, adminRoleId);
                    statement.setInt(2, zoneId);
                    statement.setString(3, firstName);
                    statement.setString(4, lastName);
                    statement.setString(5, middleName.isEmpty() ? null : middleName);
                    statement.setString(6, phone);
                    statement.setString(7, email);
                    statement.setString(8, position);
                    statement.setString(9, address);
                    statement.setTimestamp(10, Timestamp.valueOf(birthday.atStartOfDay()));
                    statement.setTimestamp(11, Timestamp.valueOf(stage.atStartOfDay()));
                    statement.setString(12, login);
                    statement.setString(13, hash);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Initial administrator insert did not create exactly one row.");
                    }
                }

                connection.commit();
                console.printf("Initial ADMIN account created. Use the supplied login at http://127.0.0.1:3000/.%n");
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                if (lockHeld) {
                    try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                        statement.setString(1, LOCK_NAME);
                        statement.executeQuery().close();
                    }
                }
            }
        }
    }

    private static int roleId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM roles WHERE UPPER(name) = 'ADMIN'")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("ADMIN role is missing; bootstrap made no changes.");
                }
                return result.getInt(1);
            }
        }
    }

    private static int chooseZone(Connection connection, Console console) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name FROM access_zones ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                ids.add(result.getInt(1));
                console.printf("%d) %s%n", result.getInt(1), result.getString(2));
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalStateException("No access zones are available; bootstrap made no changes.");
        }
        while (true) {
            String value = console.readLine("Access-zone ID: ");
            try {
                int id = Integer.parseInt(value);
                if (ids.contains(id)) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // Re-prompt for an existing zone id.
            }
            console.printf("Choose one of the listed access-zone IDs.%n");
        }
    }

    private static String required(Console console, String prompt) {
        while (true) {
            String value = console.readLine("%s", prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            console.printf("This field is required.%n");
        }
    }

    private static LocalDate date(Console console, String prompt) {
        while (true) {
            try {
                return LocalDate.parse(required(console, prompt));
            } catch (RuntimeException error) {
                console.printf("Enter a valid date in YYYY-MM-DD format.%n");
            }
        }
    }

    private static void validatePassword(char[] password, char[] repeated) {
        if (password == null || repeated == null || !Arrays.equals(password, repeated)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        int byteLength = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining();
        if (byteLength < 8 || byteLength > 72) {
            throw new IllegalArgumentException("Password must be 8-72 UTF-8 bytes for BCrypt.");
        }
    }

    private static void verifyLocalTarget(String jdbcUrl) {
        Matcher matcher = MYSQL_URL.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("DB_URL must be a MySQL JDBC URL for localhost:3306/SmartPass.");
        }
        String host = matcher.group(1).toLowerCase(Locale.ROOT);
        if (!(host.equals("localhost") || host.equals("127.0.0.1"))
                || !"3306".equals(matcher.group(2))
                || !"smartpass".equals(matcher.group(3).toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Bootstrap is restricted to localhost:3306/SmartPass.");
        }
    }

    private static String environment(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void runSelfTest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("local-bootstrap-self-test");
        if (!encoder.matches("local-bootstrap-self-test", encoded)
                || encoder.matches("incorrect-self-test", encoded)) {
            throw new IllegalStateException("BCrypt self-test failed.");
        }
        verifyLocalTarget("jdbc:mysql://localhost:3306/SmartPass");
        try {
            verifyLocalTarget("jdbc:mysql://example.invalid:3306/SmartPass");
            throw new IllegalStateException("Non-local datasource was not rejected.");
        } catch (IllegalArgumentException expected) {
            System.out.println("Bootstrap BCrypt and local-database guards passed.");
        }
    }
}
