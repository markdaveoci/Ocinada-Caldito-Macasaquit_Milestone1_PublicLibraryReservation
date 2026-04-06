public class Reservation {
    public class Reservation {

        private final int reservationId;
        private final int patronId;
        private final int bookId;
        private final String status;

        public Reservation(int reservationId, int patronId, int bookId, String status) {
            this.reservationId = reservationId;
            this.patronId = patronId;
            this.bookId = bookId;
            this.status = status;
        }

        public int getReservationId() {
            return reservationId;
        }

        public int getPatronId() {
            return patronId;
        }

        public int getBookId() {
            return bookId;
        }

        public String getStatus() {
            return status;
        }
    }



}