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

    public double getBookPrice(int bookId) {
        String sql = "SELECT price FROM books WHERE bookId = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("price");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return 0;
    }

    public void reserveBook(int patronId, int bookId) {

        String checkPatron = "SELECT status FROM patrons WHERE patronId = ?";
        String checkReservation =
                "SELECT * FROM reservations WHERE bookId = ? AND status IN ('IN QUEUE', 'ON HOLD')";
        String insertReservation =
                "INSERT INTO reservations(patronId, bookId, status, estimatedAvailableDate) VALUES (?, ?, ?, ?)";

        try (Connection conn = connect()) {

            PreparedStatement pstmt1 = conn.prepareStatement(checkPatron);
            pstmt1.setInt(1, patronId);
            ResultSet rs1 = pstmt1.executeQuery();

            if (!rs1.next() || !rs1.getString("status").trim().equalsIgnoreCase("ACTIVE")) {
                System.out.println("Cannot reserve a book. The Patron account is not active.");
                return;
            }

            PreparedStatement pstmt2 = conn.prepareStatement(checkReservation);
            pstmt2.setInt(1, bookId);
            ResultSet rs2 = pstmt2.executeQuery();

            if (rs2.next()) {
                System.out.println("Cannot reserve items as it is already on hold by another patron.");
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate estimatedDate = today.plusDays(7);

            PreparedStatement pstmt3 = conn.prepareStatement(insertReservation, Statement.RETURN_GENERATED_KEYS);
            pstmt3.setInt(1, patronId);
            pstmt3.setInt(2, bookId);
            pstmt3.setString(3, "IN QUEUE");
            pstmt3.setDate(4, java.sql.Date.valueOf(estimatedDate));

            pstmt3.executeUpdate();

            ResultSet keys = pstmt3.getGeneratedKeys();
            if (keys.next()) {
                int reservationId = keys.getInt(1);
                System.out.println("Book ID " + bookId + " reservation confirmed for Patron ID " + patronId + ".");
                System.out.println("Reservation ID: " + reservationId);
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    public boolean isBookAvailable(int bookId) {

        String sql = "SELECT * FROM reservations WHERE bookId = ? AND status IN ('ON HOLD','IN QUEUE')";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();

            return !rs.next(); // true = available, false = reserved

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public String maskAccount(String number) {
        if (number.length() <= 4) return number;

        int visible = 3;

        return number.substring(0, visible)
                + "*".repeat(number.length() - 6)
                + number.substring(number.length() - visible);
    }

    public void savePayment(int patronId, double amount, String method, String accountNumber) {

        String sql = "INSERT INTO payments(patronId, amount, method, paymentDate, accountNumber) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, patronId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, method);
            pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setString(5, maskAccount(accountNumber));

            pstmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Payment error: " + e.getMessage());
        }
    }

    public void cancelBookReservation(int reservationId) {

        String checkReservation =
                "SELECT r.status, p.status AS patronStatus " +
                        "FROM reservations r " +
                        "JOIN patrons p ON r.patronId = p.patronId " +
                        "WHERE r.reservationId = ?";

        String deleteReservation =
                "DELETE FROM reservations WHERE reservationId = ?";

        try (Connection conn = connect()) {

            PreparedStatement pstmt1 = conn.prepareStatement(checkReservation);
            pstmt1.setInt(1, reservationId);
            ResultSet rs = pstmt1.executeQuery();

            if (!rs.next()) {
                System.out.println("Reservation not found.");
                return;
            }

            String reservationStatus = rs.getString("status").trim();
            String patronStatus = rs.getString("patronStatus").trim();

            if (!patronStatus.equalsIgnoreCase("ACTIVE")) {
                System.out.println("Cannot cancel reservation. The patron account is not active.");
                return;
            }

            if (reservationStatus.equalsIgnoreCase("ON HOLD")) {
                System.out.println("Cannot cancel as the book is already ready for pickup.");
                return;
            }

            PreparedStatement pstmt2 = conn.prepareStatement(deleteReservation);
            pstmt2.setInt(1, reservationId);
            pstmt2.executeUpdate();

            System.out.println("Reservation cancelled successfully.");

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    public void viewBookReservationStatus(int patronId) {

        String checkPatron = "SELECT status FROM patrons WHERE patronId = ?";

        String sql =
                "SELECT r.reservationId, r.bookId, r.status, r.estimatedAvailableDate, " +
                        "(SELECT COUNT(*) FROM reservations r2 " +
                        " WHERE r2.bookId = r.bookId AND r2.reservationId <= r.reservationId) AS position " +
                        "FROM reservations r " +
                        "WHERE r.patronId = ? AND r.status IN ('IN QUEUE', 'ON HOLD') " +
                        "ORDER BY r.reservationId";

        try (Connection conn = connect()) {

            PreparedStatement pstmt1 = conn.prepareStatement(checkPatron);
            pstmt1.setInt(1, patronId);
            ResultSet rs1 = pstmt1.executeQuery();

            if (!rs1.next() || !rs1.getString("status").equalsIgnoreCase("ACTIVE")) {
                System.out.println("Cannot view reservations. The patron is not active.");
                return;
            }

            PreparedStatement pstmt2 = conn.prepareStatement(sql);
            pstmt2.setInt(1, patronId);
            ResultSet rs = pstmt2.executeQuery();

            boolean found = false;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

            while (rs.next()) {
                found = true;

                int reservationId = rs.getInt("reservationId");
                int bookId = rs.getInt("bookId");
                String status = rs.getString("status");
                int position = rs.getInt("position");

                String formattedId = String.format("%04d", reservationId);

                LocalDate estimatedDate = null;

                try {
                    java.sql.Date sqlDate = rs.getDate("estimatedAvailableDate");
                    if (sqlDate != null) {
                        estimatedDate = sqlDate.toLocalDate();
                    } else {
                        estimatedDate = LocalDate.now().plusDays(7L * position);
                    }
                } catch (Exception e) {
                    try {
                        Timestamp ts = rs.getTimestamp("estimatedAvailableDate");
                        if (ts != null) {
                            estimatedDate = ts.toLocalDateTime().toLocalDate();
                        } else {
                            estimatedDate = LocalDate.now().plusDays(7L * position);
                        }
                    } catch (Exception ex) {
                        estimatedDate = LocalDate.now().plusDays(7L * position);
                    }
                }

                String formattedDate = estimatedDate.format(formatter);

                System.out.println(
                        "Reservation #" + formattedId +
                                " is currently " + status +
                                " (Position " + position + "), Estimated Available of book is on " +
                                formattedDate + "."
                );
            }

            if (!found) {
                System.out.println("No active reservations found for this account.");
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
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

    public boolean isMaterialAvailable(int materialId) {

        String sql = "SELECT status FROM reference_materials WHERE materialId=?";

        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, materialId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("status").equalsIgnoreCase("AVAILABLE");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    public void savePayment2(int rpatronId, double amount2, String method2, String accountNumber2) {

        String sql = "INSERT INTO referencematerial_payments(patronId, amount, method, paymentDate, accountNumber) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, rpatronId);
            pstmt.setDouble(2, amount2);
            pstmt.setString(3, method2);
            pstmt.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setString(5, maskAccount(accountNumber2));

            pstmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Payment error: " + e.getMessage());
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