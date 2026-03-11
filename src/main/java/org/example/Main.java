package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(Main.class);

        Account account1 = new Account(UUID.randomUUID(), 4000, Currency.RUB);
        Account account2 = new Account(UUID.randomUUID(), 6000, Currency.RUB);

        Transfer transfer1 = new Transfer(account1, account2, 200, Currency.RUB);
        Transfer transfer2 = new Transfer(account2, account1, 200, Currency.RUB);

        List<Transfer> transfers = Arrays.asList(transfer1, transfer2);

        TransferSimulation simultaneousTransfers = new TransferSimulation(transfers, logger);
        simultaneousTransfers.executeTransfers();

        System.out.println("Account 1 balance: " + account1.getBalance());
        System.out.println("Account 2 balance: " + account2.getBalance());
    }
}

