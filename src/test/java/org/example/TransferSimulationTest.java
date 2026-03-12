package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

class TransferSimulationTest {

    private Account account1;
    private Account account2;
    private List<Account> accounts;
    private List<Transfer> transfers;

    @BeforeEach
    void setUp() {
        account1 = new Account(UUID.randomUUID(), 1000, Currency.USD);
        account2 = new Account(UUID.randomUUID(), 2000, Currency.USD);
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
    @DisplayName("Операция withdraw (снятие) корректна")
    void positiveWithdrawTest() {
        double amount = 500;
        double expected = account1.getBalance() - amount;
        account1.withdraw(amount);
        Assertions.assertEquals(expected, account1.getBalance());
    }

    @Test
    @DisplayName("Операция deposit (пополнение) корректна")
    void positiveDepositTest() {
        double amount = 500;
        double expected = account2.getBalance() + amount;
        account2.deposit(amount);
        Assertions.assertEquals(expected, account2.getBalance());
    }

    @Test
    @DisplayName("Ошибка при недостатке средств при снятии")
    void negativeWithdrawTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> account1.withdraw(1500));
    }

    @Test
    @DisplayName("Ошибка при недостатке средств при ПЕРЕВОДЕ (Transfer.execute()")
    void negativeTransferTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Transfer(account1, account2, 1500, Currency.USD).execute());
    }

    @Test
    @DisplayName("Нет ошибок при пополнении")
    void cornerCaseDepositTest() {
        AtomicInteger errorCount = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(() -> {
                try {
                    account1.deposit(10);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                errorCount.incrementAndGet();
            }
        }

        Assertions.assertEquals(0, errorCount.get());
    }

    @Test
    @DisplayName("Нет ошибок при снятии")
    void cornerCaseWithdrawTest() {
        AtomicInteger errorCount = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(() -> {
                try {
                    account2.withdraw(10);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                errorCount.incrementAndGet();
            }
        }
        Assertions.assertEquals(0, errorCount.get());
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