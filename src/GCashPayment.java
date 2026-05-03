class GCashPayment extends PaymentFramework {

    private final double BALANCE;
    private final String ACCOUNT_NUMBER;

    public GCashPayment(double baseAmount, double discountRate, double balance, String accountNumber) {
        super(baseAmount, discountRate);
        this.BALANCE = balance;
        this.ACCOUNT_NUMBER = accountNumber;
    }

    public String maskNumber() {
        if (ACCOUNT_NUMBER.length() <= 4) return ACCOUNT_NUMBER;

        int visible = 3;
        int maskedLength = ACCOUNT_NUMBER.length() - (visible * 2);

        return ACCOUNT_NUMBER.substring(0, visible)
                + "*".repeat(maskedLength)
                + ACCOUNT_NUMBER.substring(ACCOUNT_NUMBER.length() - visible);
    }

    private boolean isValidGCashNumber() {
        return ACCOUNT_NUMBER.matches("^09\\d{9}$");
    }

    @Override
    public boolean validatePayment() {
        System.out.println("Checking GCash account: " + maskNumber());

        if (!isValidGCashNumber()) {
            System.out.println("Invalid GCash number.");
            return false;
        }

        double total = applyDiscountRate(applyVATRate(baseAmount));

        if (BALANCE < total) {
            System.out.println("Insufficient GCash balance.");
            return false;
        }

        return true;
    }
}