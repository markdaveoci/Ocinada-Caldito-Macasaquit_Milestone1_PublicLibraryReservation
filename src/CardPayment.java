class CardPayment extends PaymentFramework {

    private final double BALANCE;
    private final String ACCOUNT_NUMBER;

    public CardPayment(double baseAmount, double discountRate, double balance, String accountNumber) {
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

    private boolean isValidCardNumber() {
        return ACCOUNT_NUMBER.matches("^\\d{16}$");
    }

    @Override
    public boolean validatePayment() {
        System.out.println("Checking Card: " + maskNumber());

        if (!isValidCardNumber()) {
            System.out.println("Invalid card number.");
            return false;
        }

        double total = applyDiscountRate(applyVATRate(baseAmount));

        if (BALANCE < total) {
            System.out.println("Insufficient card balance.");
            return false;
        }

        return true;
    }
}