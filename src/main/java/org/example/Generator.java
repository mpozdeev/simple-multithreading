package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Generator {

    public List<Account> generateAccounts(int count) {
        Random random = new Random();
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(new Account(UUID.randomUUID(), random.nextInt(10_000), Currency.random()));
        }
        return accounts;
    }

    public List<Account> generateAccounts(int count, Currency currency) {
        Random random = new Random();
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(new Account(UUID.randomUUID(), random.nextInt(10_000), currency));
        }
        return accounts;
    }

}
