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
    public int reserveBook(int patronId, int bookId) {

        String checkPatron = "SELECT status FROM patrons WHERE patronId = ?";
        String checkReservation = "SELECT * FROM reservations WHERE bookId = ? AND status IN ('IN QUEUE', 'ON HOLD')";
        String insertReservation = "INSERT INTO reservations(patronId, bookId, status, estimatedAvailableDate) VALUES (?, ?, ?, ?)";
        String updateBookStatus = "UPDATE books SET available = ?, status = ? WHERE bookId = ?";
        String getBook = "SELECT price FROM books WHERE bookId = ?";

        try (Connection conn = connect()) {

            PreparedStatement pstmt1 = conn.prepareStatement(checkPatron);
            pstmt1.setInt(1, patronId);
            ResultSet rs1 = pstmt1.executeQuery();

            if (!rs1.next() || !rs1.getString("status").equalsIgnoreCase("ACTIVE")) {
                System.out.println("Patron not active.");
                return -1;
            }

            PreparedStatement pstmt2 = conn.prepareStatement(checkReservation);
            pstmt2.setInt(1, bookId);
            ResultSet rs2 = pstmt2.executeQuery();

            if (rs2.next()) {
                System.out.println("Book already reserved.");
                return -1;
            }

            PreparedStatement pstmtBook = conn.prepareStatement(getBook);
            pstmtBook.setInt(1, bookId);
            ResultSet rsBook = pstmtBook.executeQuery();

            if (!rsBook.next()) {
                System.out.println("Book not found.");
                return -1;
            }

            double price = rsBook.getDouble("price");

            LocalDate estimatedDate = LocalDate.now().plusDays(7);

            PreparedStatement pstmt3 = conn.prepareStatement(insertReservation, Statement.RETURN_GENERATED_KEYS);
            pstmt3.setInt(1, patronId);
            pstmt3.setInt(2, bookId);
            pstmt3.setString(3, "ON HOLD");
            pstmt3.setDate(4, java.sql.Date.valueOf(estimatedDate));

            pstmt3.executeUpdate();

            PreparedStatement pstmt4 = conn.prepareStatement(updateBookStatus);
            pstmt4.setInt(1, 0);
            pstmt4.setString(2, "ON HOLD");
            pstmt4.setInt(3, bookId);
            pstmt4.executeUpdate();

            System.out.println("Reservation successful!");

            return 0;

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            return -1;
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
    public void cancelBookReservation(int reservationId, String refundAccount) {

        String checkReservation =
                "SELECT r.status, r.patronId, p.status AS patronStatus " +
                        "FROM reservations r " +
                        "JOIN patrons p ON r.patronId = p.patronId " +
                        "WHERE r.reservationId = ?";

        String getPayment =
                "SELECT amount, method, accountNumber FROM payments " +
                        "WHERE patronId = (SELECT patronId FROM reservations WHERE reservationId = ?)";

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
                System.out.println("Cannot cancel reservation. Patron is not active.");
                return;
            }

            if (reservationStatus.equalsIgnoreCase("READY FOR PICKUP")) {
                System.out.println("Cannot cancel. Book is already ready for pickup.");
                return;
            }

            PreparedStatement pstmt2 = conn.prepareStatement(getPayment);
            pstmt2.setInt(1, reservationId);
            ResultSet payRs = pstmt2.executeQuery();

            double amount = 0;
            String method = "";
            String originalAccount = "";

            if (payRs.next()) {
                amount = payRs.getDouble("amount");
                method = payRs.getString("method");
                originalAccount = payRs.getString("accountNumber");
            }

            String originalSuffix = originalAccount.length() >= 3
                    ? originalAccount.substring(originalAccount.length() - 3)
                    : originalAccount;

            if (!refundAccount.endsWith(originalSuffix)) {
                System.out.println("Refund failed: account does not match original payment method.");
                return;
            }

            PreparedStatement pstmt3 = conn.prepareStatement(deleteReservation);
            pstmt3.setInt(1, reservationId);
            pstmt3.executeUpdate();

            System.out.println("\n===== REFUND SUCCESS =====");
            System.out.println("Amount Refunded: ₱" + amount);
            System.out.println("Method: " + method);
            System.out.println("Refunded To: " + maskAccount(refundAccount));
            System.out.println("==========================");

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