import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;

public class Repository {
    private static final String DB_URL = "jdbc:sqlite:library.db";

    public Repository() {
        createTables();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTables() {

        String visitors = "CREATE TABLE IF NOT EXISTS visitors(" +
                "visitorId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "address TEXT," +
                "purpose TEXT," +
                "checkin_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'IN')";

        String patrons = "CREATE TABLE IF NOT EXISTS patrons(" +
                "patronId INTEGER PRIMARY KEY," +
                "name TEXT," +
                "address TEXT,";

        String books = "CREATE TABLE IF NOT EXISTS books(" +
                "bookId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "author TEXT," +
                "available INTEGER)";

        String reservations = "CREATE TABLE IF NOT EXISTS reservations(" +
                "reservationId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patronId INTEGER," +
                "bookId INTEGER,";

        String referenceMaterials = "CREATE TABLE IF NOT EXISTS reference_materials(" +
                "materialId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "status TEXT)";

        String refReservations = "CREATE TABLE IF NOT EXISTS reference_reservations(" +
                "reservationId INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patronId INTEGER," +
                "materialId INTEGER," +
                "status TEXT)";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(visitors);
            stmt.execute(patrons);
            stmt.execute(books);
            stmt.execute(reservations);
            stmt.execute(referenceMaterials);
            stmt.execute(refReservations);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public int checkInVisitor(Visitor visitor) {
        int generatedId = -1;
        String sql = "INSERT INTO visitors(name, address, purpose, checkin_time, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Manila"));
            Timestamp timestamp = Timestamp.valueOf(now.toLocalDateTime());

            pstmt.setString(1, visitor.getName());
            pstmt.setString(2, visitor.getAddress());
            pstmt.setString(3, visitor.getPurpose());
            pstmt.setTimestamp(4, timestamp);
            pstmt.setString(5, "IN");

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Check-in failed, no rows affected.");

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) generatedId = rs.getInt(1);
                else throw new SQLException("Check-in failed, no ID obtained.");
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            System.out.println("Check-in time: " + now.format(formatter));

        } catch (Exception e) {
            System.out.println("Error saving visitor: " + e.getMessage());
        }

        return generatedId;
    }

    public int registerAsAPatron(Patron patron) {
        String sql = "INSERT INTO patrons(patronId, name, address, membershipType, status) VALUES(?,?,?,?,?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patron.getPatronId());
            pstmt.setString(2, patron.getName());
            pstmt.setString(3, patron.getAddress());
            pstmt.setString(4, patron.getMembershipType());
            pstmt.setString(5, "ACTIVE");

            pstmt.executeUpdate();

            return patron.getPatronId();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public void viewBooks() {

        String sql = "SELECT * FROM books";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\nAvailable Books:");

            while (rs.next()) {

                System.out.println(
                        "Book ID: " + rs.getInt("bookId") +
                                " | Title: " + rs.getString("title") +
                                " | Author: " + rs.getString("author") +
                                " | Available: " + rs.getInt("available")
                );
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewReferenceMaterials() {

        String sql = "SELECT * FROM reference_materials";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\nReference Materials:");

            while (rs.next()) {
                System.out.println(
                        "Material ID: " + rs.getInt("materialId") +
                                " | Title: " + rs.getString("title") +
                                " | Status: " + rs.getString("status")
                );
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void reserveReferenceMaterial(int patronId, int materialId) {

        String checkSql = "SELECT status FROM reference_materials WHERE materialId=?";
        String reserveSql = "INSERT INTO reference_reservations(patronId, materialId, status) VALUES (?, ?, ?)";
        String updateSql = "UPDATE reference_materials SET status='RESERVED' WHERE materialId=?";

        try (Connection conn = connect()) {

            conn.setAutoCommit(false);

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {

                check.setInt(1, materialId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {

                    String status = rs.getString("status");

                    if (status.equalsIgnoreCase("AVAILABLE")) {

                        try (PreparedStatement reserve = conn.prepareStatement(reserveSql, Statement.RETURN_GENERATED_KEYS);
                             PreparedStatement update = conn.prepareStatement(updateSql)) {

                            reserve.setInt(1, patronId);
                            reserve.setInt(2, materialId);
                            reserve.setString(3, "RESERVED");

                            int rows = reserve.executeUpdate();

                            if (rows > 0) {

                                ResultSet generatedKeys = reserve.getGeneratedKeys();
                                if (generatedKeys.next()) {
                                    int reservationId = generatedKeys.getInt(1);

                                    update.setInt(1, materialId);
                                    update.executeUpdate();

                                    conn.commit();

                                    System.out.println("Reference material successfully reserved.");
                                    System.out.println("Reservation ID: " + reservationId);
                                } else {
                                    conn.rollback();
                                    System.out.println("Failed to retrieve reservation ID.");
                                }

                            }

                        }

                    } else {
                        System.out.println("Cannot reserve. Current status: " + status);
                        conn.rollback();
                    }

                } else {
                    System.out.println("Reference material not found.");
                    conn.rollback();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred during reservation.");
        }
    }

    public void cancelReservedReferenceMaterial(int reservationId) {

        String checkSql = "SELECT materialId FROM reference_reservations WHERE reservationId=?";
        String deleteSql = "DELETE FROM reference_reservations WHERE reservationId=?";
        String updateSql = "UPDATE reference_materials SET status='AVAILABLE' WHERE materialId=?";

        try (Connection conn = connect()) {

            conn.setAutoCommit(false);

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {

                check.setInt(1, reservationId);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {

                    int materialId = rs.getInt("materialId");

                    try (PreparedStatement delete = conn.prepareStatement(deleteSql);
                         PreparedStatement update = conn.prepareStatement(updateSql)) {

                        delete.setInt(1, reservationId);
                        delete.executeUpdate();

                        update.setInt(1, materialId);
                        update.executeUpdate();

                        conn.commit();
                        System.out.println("Reservation cancelled successfully.");
                    }

                } else {
                    System.out.println("Reservation record not found.");
                    conn.rollback();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred while cancelling reservation.");
        }
    }

    public void viewReservedReferenceMaterialStatus(int patronId) {

        String sql = "SELECT reservationId, materialId, status FROM reference_reservations WHERE patronId=?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patronId);

            try (ResultSet rs = pstmt.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println(
                            "Reservation ID: " + rs.getInt("reservationId") +
                                    " | Material ID: " + rs.getInt("materialId") +
                                    " | Status: " + rs.getString("status")
                    );
                }

                if (!found) {
                    System.out.println("No reference material found with the provided information.");
                } else {
                    System.out.println("Reference material status displayed successfully.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error retrieving reference material status.");
        }
    }

    public void checkOutVisitor(int visitorId) {
        String checkSql = "SELECT * FROM visitors WHERE visitorId=?";
        String updateSql = "UPDATE visitors SET checkout_time = ?, status='OUT' WHERE visitorId=?";

        try (Connection conn = connect();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setInt(1, visitorId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Manila"));
                Timestamp timestamp = Timestamp.valueOf(now.toLocalDateTime());

                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setTimestamp(1, timestamp);
                    update.setInt(2, visitorId);
                    update.executeUpdate();

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    System.out.println("Visitor check-out recorded successfully.");
                    System.out.println("Check-out time: " + now.format(formatter));
                }
            } else {
                System.out.println("Visitor record not found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during visitor check-out.");
        }
    }
}