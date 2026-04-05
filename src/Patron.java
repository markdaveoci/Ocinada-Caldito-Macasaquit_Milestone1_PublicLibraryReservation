public class Patron {
    private final int patronId;
    private final String name;
    private final String address;
    private final String membershipType;
    private String status;

    public Patron(String name, String address, String membershipType) {
        this.patronId = PatronIDGenerator.generateID();
        this.name = name;
        this.address = address;
        this.membershipType = membershipType;
        this.status = "ACTIVE";
    }

    public int getPatronId() {
        return patronId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    static class PatronIDGenerator {

        public static int generateID() {
            return (int) (Math.random() * 100000);
        }
    }
}