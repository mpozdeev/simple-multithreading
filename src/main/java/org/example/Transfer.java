package org.example;

public class Transfer {

    private final Account from;
    private final Account to;
    private final double amount;
    private final Currency currency;

    public Transfer(Account from, Account to, double amount, Currency currency) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.currency = currency;
    }

    public void execute() {
        if (from.getCurrency().equals(currency) && to.getCurrency().equals(currency)) {
            from.withdraw(amount);
            to.deposit(amount);
            printInfoByTransaction();
        } else {
            throw new IllegalArgumentException("Валюты не совпадают! Валюта для перевода " + currency +
                    ": from("+from.getCurrency() + ") ->" +
                    " to("+ to.getCurrency() +")");

        }
    }

    private void printInfoByTransaction() {
        System.out.printf("From: %s\t  To: %s\t amount: %s\t%n",
                from.getId().toString().substring(0, 8),
                to.getId().toString().substring(0, 8), amount);
    }
}