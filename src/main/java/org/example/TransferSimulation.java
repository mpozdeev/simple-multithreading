package org.example;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TransferSimulation {

    private final List<Transfer> transfers;
    private final Logger logger;

    public TransferSimulation(List<Transfer> transfers, Logger logger) {
        this.transfers = transfers;
        this.logger = logger;
    }

    public void executeTransfers() {
        List<Thread> threads = new ArrayList<>();
        for (Transfer transfer : transfers) {
            Thread thread = new Thread(() -> {
                try {
                    transfer.execute();
                } catch (Exception e) {
                    logger.error("Error during transfer", e);
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
}
