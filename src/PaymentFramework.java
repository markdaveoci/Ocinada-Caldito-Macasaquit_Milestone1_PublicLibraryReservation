abstract class PaymentFramework {

    protected double baseAmount;
    protected double discountRate;
    protected final double VATRATE = 0.12;

    public PaymentFramework(double baseAmount, double discountRate) {
        this.baseAmount = baseAmount;
        this.discountRate = discountRate;
    }

    public abstract boolean validatePayment();

    public double applyVATRate (double baseAmount) {
        return baseAmount + (baseAmount * VATRATE);
    }

    public double applyDiscountRate(double baseAmount) {
        return baseAmount - discountRate;
    }

    public void finalizeTransaction(double finalAmount) {
        System.out.println("Transaction completed!");
        System.out.println("Final Amount: " + finalAmount);
    }

    public void processInvoice() {
        System.out.println("Processing invoice...");

        if (!validatePayment()) {
            System.out.println("Payment validation failed.");
            return;
        }

        double total = applyVATRate(baseAmount);
        total = applyDiscountRate(total);

        finalizeTransaction(total);
    }
}