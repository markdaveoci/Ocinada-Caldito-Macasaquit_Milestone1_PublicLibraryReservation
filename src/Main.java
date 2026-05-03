import java.util.Scanner;

public class Main {

    public static class PaymentResult {
        double finalAmount;
        String method;
        String masked;
        double remainingBalance;
    }

    public static PaymentResult processPayment(
            Scanner scanner,
            PaymentFramework payment,
            String method,
            double price,
            double balance
    ) {

        PaymentResult result = new PaymentResult();

        payment.processInvoice();

        if (!payment.validatePayment()) {
            System.out.println("Insufficient balance. Transaction cancelled.");
            return null;
        }

        result.finalAmount = payment.applyDiscountRate(
                payment.applyVATRate(price)
        );

        result.remainingBalance = balance - result.finalAmount;
        result.method = method;

        if (payment instanceof GCashPayment) {
            result.masked = ((GCashPayment) payment).maskNumber();
        } else {
            result.masked = ((CardPayment) payment).maskNumber();
        }

        return result;
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Repository repo = new Repository();

        System.out.println("\nPUBLIC LIBRARY SYSTEM" + "\nWelcome to the Public Library System. Visitors are required to check in upon arrival.\n");
        System.out.println("[1] Check-in Visitor");

        System.out.print("Choose: ");
        int choice1 = scanner.nextInt();
        scanner.nextLine();

        switch (choice1) {
            case 1:
                boolean validVisitor = false;

                while (!validVisitor) {
                    System.out.println("\nCHECK IN VISITOR");

                    System.out.print("Visitor Name: ");
                    String name = scanner.nextLine();

                    System.out.print("Address: ");
                    String address = scanner.nextLine();

                    System.out.print("Purpose of Visit: ");
                    String purpose = scanner.nextLine();

                    if (name.isEmpty() || address.isEmpty() || purpose.isEmpty()) {
                        System.out.println("\nCheck-in failed. Please fill in all fields.");
                    } else {
                        Visitor visitor = new Visitor.VisitorBuilder()
                                .setName(name)
                                .setAddress(address)
                                .setPurpose(purpose)
                                .build();

                        System.out.println("\nENTRY CONFIRMATION");
                        System.out.println("Name: " + name);
                        System.out.println("Address: " + address);
                        System.out.println("Purpose: " + purpose);
                        System.out.println("Confirmation: Complete information.");
                        System.out.println("Status: IN");

                        System.out.println("\nVisitor check-in successful. You may now proceed to the Circulation Desk.");
                        System.out.print("\n");

                        int visitorId = repo.checkInVisitor(visitor);

                        System.out.println("Visitor ID: " + visitorId);

                        validVisitor = true;
                    }
                }
                break;

            default:
                System.out.println("Invalid choice.");
        }

        System.out.println("\nCIRCULATION DESK" + "\nWelcome to the Circulation Desk. All visitors are required to complete registration before accessing the Public Library Reservation System.\n");
        System.out.println("[1] Register as a Patron");

        System.out.print("Choose: ");
        int choice2 = scanner.nextInt();
        scanner.nextLine();

        switch (choice2) {
            case 1:

                boolean validPatron = false;

                while (!validPatron) {

                    System.out.println("\nREGISTRATION");

                    System.out.print("Patron Name: ");
                    String pname = scanner.nextLine();

                    System.out.print("Address: ");
                    String address2 = scanner.nextLine();

                    System.out.print("Membership Type: ");
                    String membershipType = scanner.nextLine();

                    if (pname.isEmpty() || address2.isEmpty() || membershipType.isEmpty()) {
                        System.out.println("\nRegistration failed. Required fields are missing or invalid.");
                    } else {
                        Patron patron = new Patron(pname, address2, membershipType);
                        repo.registerAsAPatron(patron);

                        System.out.println("\nPatron ID " + patron.getPatronId() + " created successfully. You may now proceed to the Public Library Reservation System.");
                        validPatron = true;
                    }
                }
                break;

            default:
                System.out.println("Invalid choice.");
        }

        boolean running = true;

        while (running) {

            System.out.println("\nPUBLIC LIBRARY RESERVATION SYSTEM");
            System.out.println("Welcome, Patron \n");
            System.out.println("[1] Reserve Book");
            System.out.println("[2] Cancel Book Reservation");
            System.out.println("[3] View Book Reservation Status");
            System.out.println("[4] Reserve Reference Material");
            System.out.println("[5] Cancel Reserved Reference Material");
            System.out.println("[6] View Reserved Reference Material Status");
            System.out.println("[7] Log-out Patron");

            System.out.print("Choose: ");
            int choice3 = scanner.nextInt();
            scanner.nextLine();

            switch (choice3) {

                case 1:

                    repo.viewBooks();

                    System.out.println("\nRESERVE BOOK");

                    System.out.print("Enter Patron ID: ");
                    int patronId = scanner.nextInt();

                    System.out.print("Enter Book ID: ");
                    int bookId = scanner.nextInt();

                    if (!repo.isBookAvailable(bookId)) {
                        System.out.println("Book already reserved.");
                        break;
                    }

                    double price = repo.getBookPrice(bookId);

                    double finalAmount = (price + (price * 0.12)) - 10;

                    System.out.println("\n===== INVOICE PREVIEW =====");
                    System.out.println("Base Price: ₱" + price);
                    System.out.println("VAT (12%): Applied");
                    System.out.println("Discount: ₱10");
                    System.out.println("FINAL AMOUNT: ₱" + finalAmount);
                    System.out.println("===========================");

                    System.out.println("\nMODE OF PAYMENT");
                    System.out.println("1. GCash");
                    System.out.println("2. Card");
                    System.out.print("Choice: ");
                    int choice = scanner.nextInt();

                    String method;
                    String accountNumber;
                    double balance;
                    PaymentFramework payment;

                    if (choice == 1) {
                        System.out.print("\nGCash Number: ");
                        accountNumber = scanner.next();

                        System.out.print("Balance: ");
                        balance = scanner.nextDouble();

                        payment = new GCashPayment(price, 10, balance, accountNumber);
                        method = "GCash";

                    } else if (choice == 2) {
                        System.out.print("\nCard Number: ");
                        accountNumber = scanner.next();

                        System.out.print("Balance: ");
                        balance = scanner.nextDouble();

                        payment = new CardPayment(price, 10, balance, accountNumber);
                        method = "Card";

                    } else {
                        System.out.println("Invalid choice.");
                        break;
                    }

                    System.out.println();

                    payment.processInvoice();

                    System.out.println();

                    if (!payment.validatePayment()) {
                        System.out.println("Payment failed.");
                        break;
                    }

                    int reservationId = repo.reserveBook(patronId, bookId);

                    if (reservationId == -1) {
                        System.out.println("Reservation failed.");
                        break;
                    }

                    repo.savePayment(patronId, finalAmount, method, accountNumber);

                    double remainingBalance = balance - finalAmount;

                    String masked;
                    if (payment instanceof GCashPayment) {
                        masked = ((GCashPayment) payment).maskNumber();
                    } else {
                        masked = ((CardPayment) payment).maskNumber();
                    }

                    System.out.println("\n===== RECEIPT =====");
                    System.out.println("Patron ID: " + patronId);
                    System.out.println("Book ID: " + bookId);
                    System.out.println("Account: " + masked);
                    System.out.println("Amount Paid: ₱" + finalAmount);
                    System.out.println("Method: " + method);
                    System.out.println("Entered Balance: ₱" + balance);
                    System.out.println("Deducted Amount: ₱" + finalAmount);
                    System.out.println("Remaining Balance: ₱" + remainingBalance);
                    System.out.println("===================");

                    break;

                case 2:

                    System.out.println("\nCANCEL BOOK RESERVATION");

                    System.out.print("Reservation ID: ");
                    int rId = scanner.nextInt();

                    System.out.print("Enter GCash/Card Number for refund: ");
                    String refundAccount = scanner.next();

                    repo.cancelBookReservation(rId, refundAccount);
                    break;


                case 3:
                    System.out.println("\nVIEW BOOK RESERVATION STATUS");
                    System.out.print("Enter Patron ID: ");
                    int pid = scanner.nextInt();
                    System.out.print("\n");

                    repo.viewBookReservationStatus(pid);
                    break;

                case 4:

                    repo.viewReferenceMaterials();

                    System.out.println("\nRESERVE REFERENCE MATERIAL");

                    System.out.print("Enter Patron ID: ");
                    int rpatronId = scanner.nextInt();

                    System.out.print("Enter Material ID: ");
                    int materialId = scanner.nextInt();

                    if (!repo.isMaterialAvailable(materialId)) {
                        System.out.println("Already reserved.");
                        break;
                    }

                    double price2 = 100;

                    double finalAmount2 = (price2 + (price2 * 0.12)) - 10;

                    System.out.println("\n===== INVOICE PREVIEW =====");
                    System.out.println("Base Price: ₱" + price2);
                    System.out.println("VAT (12%): Applied");
                    System.out.println("Discount: ₱10");
                    System.out.println("FINAL AMOUNT: ₱" + finalAmount2);
                    System.out.println("===========================");

                    System.out.println("\nMODE OF PAYMENT");
                    System.out.println("1. GCash");
                    System.out.println("2. Card");
                    System.out.print("Choice: ");
                    int choice5 = scanner.nextInt();

                    String method2;
                    String accountNumber2;
                    double balance2;
                    PaymentFramework payment2;

                    if (choice5 == 1) {
                        System.out.print("\nGCash Number: ");
                        accountNumber2 = scanner.next();

                        System.out.print("Balance: ");
                        balance2 = scanner.nextDouble();

                        payment2 = new GCashPayment(price2, 10, balance2, accountNumber2);
                        method2 = "GCash";

                    } else if (choice5 == 2) {
                        System.out.print("\nCard Number: ");
                        accountNumber2 = scanner.next();

                        System.out.print("Balance: ");
                        balance2 = scanner.nextDouble();

                        payment2 = new CardPayment(price2, 10, balance2, accountNumber2);
                        method2 = "Card";

                    } else {
                        System.out.println("Invalid choice.");
                        break;
                    }

                    System.out.println();

                    payment2.processInvoice();

                    System.out.println();

                    if (!payment2.validatePayment()) {
                        System.out.println("Payment failed.");
                        break;
                    }

                    int reservationId2 = repo.reserveReferenceMaterial(rpatronId, materialId);

                    if (reservationId2 == -1) {
                        System.out.println("Reservation failed.");
                        break;
                    }

                    repo.savePayment2(rpatronId, finalAmount2, method2, accountNumber2);

                    String masked2;
                    if (payment2 instanceof GCashPayment) {
                        masked2 = ((GCashPayment) payment2).maskNumber();
                    } else {
                        masked2 = ((CardPayment) payment2).maskNumber();
                    }

                    System.out.println("\n===== RECEIPT =====");
                    System.out.println("Patron ID: " + rpatronId);
                    System.out.println("Reservation ID: " + reservationId2);
                    System.out.println("Reference Material ID: " + materialId);
                    System.out.println("Account: " + masked2);
                    System.out.println("Amount Paid: ₱" + finalAmount2);
                    System.out.println("Method: " + method2);
                    System.out.println("Remaining Balance: ₱" + (balance2 - finalAmount2));
                    System.out.println("===================");

                    break;
                    
                case 5:

                    System.out.println("\nCANCEL RESERVED REFERENCE MATERIAL");

                    System.out.print("Enter Reservation ID: ");
                    int refReservationId = scanner.nextInt();

                    System.out.print("Enter GCash/Card Number for refund: ");
                    String refundAccount2 = scanner.next();

                    repo.cancelReservedReferenceMaterial(refReservationId, refundAccount2);

                    break;

                case 6:
                    System.out.println("\nVIEW RESERVED REFERENCE MATERIAL STATUS");
                    System.out.print("Enter Patron ID: ");
                    int patId = scanner.nextInt();
                    System.out.print("\n");

                    repo.viewReservedReferenceMaterialStatus(patId);
                    break;

                case 7:
                    System.out.print("\n");
                    System.out.println("Patron account successfully logged-out. You may now proceed for check-out.");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }

        boolean checkingOut = true;

        while (checkingOut) {
            System.out.println("\nPUBLIC LIBRARY SYSTEM");
            System.out.println("[1] Check-out Visitor");

            System.out.print("Choose: ");
            int choice4 = scanner.nextInt();
            scanner.nextLine();

            switch (choice4) {
                case 1:
                    System.out.println("\nCHECK-OUT");
                    System.out.print("Enter Visitor ID: ");
                    int visitorIdCheckout = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("\n");

                    repo.checkOutVisitor(visitorIdCheckout);

                    System.out.println("\nSystem exiting after check-out.");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
        scanner.close();
    }
}