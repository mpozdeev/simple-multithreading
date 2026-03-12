package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Main {

    static void main() {
        Logger logger = LoggerFactory.getLogger(Main.class);

//        executeFirst(logger);
        executeSecond(logger);
    }

    private static void executeSecond(Logger logger) {
//        TransferSimulation transferSimulation = new TransferSimulation(40, 10, 10, logger);
        TransferSimulation simulation = new TransferSimulation(10, 3, 2, logger);
        simulation.executeTransferInLoop();
    }

    private static void executeFirst(Logger logger) {
        Account account1 = new Account(UUID.randomUUID(), 4000, Currency.RUB);
        Account account2 = new Account(UUID.randomUUID(), 6000, Currency.RUB);

        double totalBalanceBefore = account1.getBalance() + account2.getBalance();
        System.out.println("Total system balance before: " + totalBalanceBefore);

        Transfer transfer1 = new Transfer(account1, account2, 200, Currency.RUB);
        Transfer transfer2 = new Transfer(account2, account1, 300, Currency.RUB);

        List<Transfer> transfers = Arrays.asList(transfer1, transfer2);

        TransferSimulation simultaneousTransfers = new TransferSimulation(transfers, logger);
        simultaneousTransfers.executeTransfers();

        System.out.println("Account 1 balance: " + account1.getBalance());
        System.out.println("Account 2 balance: " + account2.getBalance());
        double totalBalanceAfter = account1.getBalance() + account2.getBalance();
        System.out.println("Total system balance after: " + totalBalanceAfter);
    }
}

