package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class TransferSimulationTest {

    private List<Account> accounts;
    private List<Transfer> transfers;

    @BeforeEach
    void setUp() {
        accounts = new ArrayList<>();
        transfers = new ArrayList<>();

        // Generate 100 test accounts
        for (int i = 0; i < 100; i++) {
            accounts.add(new Account(UUID.randomUUID(), 5_000, Currency.random()));
        }

        // Generate transfers
        for (int i = 0; i < 100; i++) {
            transfers.add(new Transfer(
                    accounts.get(i % 99),
                    accounts.get((i + 1) % 100),
                    100,
                    Currency.random()));
        }
    }

    @Test
    void simultaneousTransfers() {
        TransferSimulation simulation = new TransferSimulation(transfers, LoggerFactory.getLogger(TransferSimulation.class.getName()));

        AtomicInteger errorCount = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        // Start all transfers in 5 threads
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> {
                try {
                    simulation.executeTransfers();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                errorCount.incrementAndGet();
            }
        }

        Assertions.assertEquals(0, errorCount.get(), "Во время передачи данных произошли ошибки");

        // Check balances
        double totalBalance = 0;
        for (Account account : accounts) {
            totalBalance += account.getBalance();
        }

        Assertions.assertEquals(500_000, totalBalance, "Общий баланс не соответствует ожидаемому значению");
    }
}