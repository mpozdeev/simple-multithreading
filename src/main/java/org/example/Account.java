package org.example;


import java.util.Objects;
import java.util.UUID;

public class Account {

    private final UUID id;
    private double balance;
    private final Currency currency;

    public Account(UUID id, double balance, Currency currency) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
    }

    public synchronized void deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }

    public synchronized void withdraw(double amount) {
        if (amount > 0 && amount <= this.balance) {
            this.balance -= amount;
        } else {
            throw new IllegalArgumentException("На счёте: " + balance + " нужно для перевода: " + amount);
        }
    }

    public UUID getId() {
        return id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public double getBalance() {
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
