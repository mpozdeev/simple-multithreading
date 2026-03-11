package org.example;

import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class TransferSimulation {

    private static final int COUNT_CYCLE = 100;

    private final int countTransactions;
    private final int countThreads;
    private int countAccounts;
    private final List<Transfer> transfers;
    private final List<Account> accounts;
    private final List<Account> accountsBefore;

    private final Logger logger;

    public TransferSimulation(int countTransactions, int countThreads, int countAccounts, Logger logger) {
        this.countTransactions = countTransactions;
        this.countThreads = countThreads;
        this.countAccounts = countAccounts;
        this.logger = logger;
        this.transfers = new ArrayList<>();
        this.accounts = new ArrayList<>();
        this.accountsBefore = new ArrayList<>();
    }

    public TransferSimulation(List<Transfer> transfers, Logger logger) {
        this.transfers = transfers;
        this.logger = logger;
        this.countTransactions = new Random().nextInt(100);
        this.countThreads = new Random().nextInt(100);
        this.accounts = new ArrayList<>();
        this.accountsBefore = new ArrayList<>();
    }

    public TransferSimulation(List<Transfer> transfers, Logger logger, int countAccounts, int countTransactions, int countThreads) {
        this.countTransactions = countTransactions;
        this.countAccounts = countAccounts;
        this.countThreads = countThreads;
        this.transfers = transfers;
        this.logger = logger;
        this.accounts = new ArrayList<>();
        this.accountsBefore = new ArrayList<>();
    }

    public void executeTransferInLoop() {
        System.out.println("Count Threads: " + countThreads);
        System.out.println("Count Transactions: " + countTransactions);
        accounts.addAll(generateAccounts(countAccounts));
        accountsBefore.addAll(accounts);

        System.out.println(accounts==accountsBefore);
        double fullBalanceBefore = executeFullBalance(accounts);

        transfers.addAll(generateTransfers(generateAccounts(countAccounts), countTransactions));

        ExecutorService executor = newFixedThreadPool(countThreads);

        for (Transfer transfer : transfers) {
            executor.submit(() -> {
                try {
                    transfer.execute();
                } catch (Exception e) {
                    logger.error("Ошибка при переводе: ", e);
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }
        double fullBalanceAfter = executeFullBalance(accounts);
        System.out.println("Total balance Before: " + fullBalanceBefore);
        System.out.println("Total balance After: " + fullBalanceAfter);
        compareBalances(accountsBefore, accounts);
    }

    public void executeTransfers() {
        List<Thread> threads = new ArrayList<>();
        for (Transfer transfer : transfers) {
            Thread thread = new Thread(() -> {
                try {
                    for (int i = 0; i < COUNT_CYCLE; i++) {
                        transfer.execute();
                    }
                } catch (Exception e) {
                    logger.error("Ошибка при переводе: ", e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error("Thread interrupted", e);
            }
        }
    }



    private double executeFullBalance(List<Account> accounts) {
        double totalBalance = 0;
        for (Account account : accounts) {
            totalBalance += account.getBalance();
        }
        return totalBalance;
    }


    private List<Transfer> generateTransfers(List<Account> accounts, int countTransfers) {
        Random random = new Random();
        int accountsSize = accounts.size();
        List<Transfer> transferList = new ArrayList<>();
        for (int i = 0; i < countTransfers; i++) {
            Account accountFrom = accounts.get(random.nextInt(accountsSize));
            Account accountTo = accounts.get(random.nextInt(accountsSize));
//            transferList.add(new Transfer(accountFrom, accountTo, random.nextInt(1_000), Currency.random()));
            transferList.add(new Transfer(accountFrom, accountTo, random.nextInt(1_000), Currency.RUB));
        }
        return transferList;
    }

    private List<Account> generateAccounts(int count) {
        Random random = new Random();
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
//            accounts.add(new Account(UUID.randomUUID(), random.nextInt(10_000), Currency.random()));
            accounts.add(new Account(UUID.randomUUID(), random.nextInt(10_000), Currency.RUB));
        }
        return accounts;
    }

    private void compareBalances(List<Account> accountsBefore, List<Account> accountsAfter) {
        Map<UUID, Double> beforeMap = accountsBefore.stream()
                .collect(Collectors.toMap(Account::getId, Account::getBalance));

        Map<UUID, Double> afterMap = accountsAfter.stream()
                .collect(Collectors.toMap(Account::getId, Account::getBalance));

        //данная секция под вопросом - хз для чего нужна?!
        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(beforeMap.keySet());
        allIds.addAll(afterMap.keySet());

        System.out.println("-------------------------------------");

        for (UUID id : allIds) {
            Double beforeBalance = beforeMap.get(id);
            Double afterBalance = afterMap.get(id);

            String beforeStr = beforeBalance.toString();
            String afterStr = afterBalance.toString();

            System.out.printf("%s\t%s\t\t%s%n",
                    id.toString().substring(0, 8), beforeStr, afterStr);
        }
    }

}
