package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты доказывают два критических бага через TransferSimulation.executeTransfers().
 *
 * executeTransfers() запускает по одному потоку на Transfer,
 * каждый поток выполняет transfer.execute() ровно COUNT_CYCLE (100) раз.
 *
 * BUG #1 — Неатомарный перевод: withdraw() и deposit() — два отдельных
 *           synchronized-вызова. Между ними деньги «висят в воздухе».
 *           Суммарный баланс по всем аккаунтам должен быть неизменным —
 *           но из-за гонки он ломается.
 *
 * BUG #2 — Дедлок: встречные переводы A→B и B→A в параллельных потоках
 *           захватывают локи в противоположном порядке и вешают друг друга.
 *           executeTransfers() никогда не возвращает управление.
 */
class ConcurrencyBugsTest {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(ConcurrencyBugsTest.class);

    // -------------------------------------------------------------------------
    // BUG #1: Неатомарный перевод — потеря / задвоение денег
    // -------------------------------------------------------------------------

    /**
     * Два Transfer в противоположных направлениях: A→B и B→A.
     * TransferSimulation запускает два потока, каждый делает 100 итераций.
     *
     * Инвариант: сумма балансов A + B не должна меняться.
     * Если withdraw() одного потока и deposit() другого пересекаются —
     * инвариант нарушается.
     *
     * @RepeatedTest(10) — race condition недетерминирована,
     * повторяем чтобы повысить вероятность поимки.
     */
    @RepeatedTest(10)
    @DisplayName("BUG #1 — суммарный баланс портится при встречных переводах")
    void totalBalanceCorruptedByNonAtomicTransfer() throws InterruptedException {
        Account a = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);
        Account b = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);
        double expectedTotal = a.getBalance() + b.getBalance(); // 2_000_000

        // Два встречных перевода — оба потока будут толкаться на одних аккаунтах
        Transfer aToB = new Transfer(a, b, 100, Currency.USD);
        Transfer bToA = new Transfer(b, a, 100, Currency.USD);

        TransferSimulation simulation = new TransferSimulation(
                List.of(aToB, bToA), LOG
        );

        // executeTransfers() блокирующий — запускаем в отдельном потоке
        // чтобы иметь возможность задать timeout
        Thread simThread = new Thread(simulation::executeTransfers);
        simThread.start();
        simThread.join(30_000);

        double actualTotal = a.getBalance() + b.getBalance();

        assertEquals(expectedTotal, actualTotal,
                String.format(
                        "Суммарный баланс изменился! Ожидалось: %.0f, получено: %.0f.%n" +
                        "Причина: withdraw() и deposit() не атомарны — " +
                        "между ними другой поток успевает вмешаться.",
                        expectedTotal, actualTotal));
    }

    /**
     * Нагрузочный вариант: 5 аккаунтов, 8 встречных Transfer по кругу.
     * Каждый Transfer выполняется 100 раз в своём потоке.
     * Суммарный баланс по всем 5 аккаунтам должен остаться неизменным.
     */
    @Test
    @DisplayName("BUG #1 — суммарный баланс по всем аккаунтам не меняется (нагрузочный)")
    void totalBalanceInvariantUnderHighConcurrency() throws InterruptedException {
        int n = 5;
        double initialBalance = 10_000;

        Account[] acc = new Account[n];
        for (int i = 0; i < n; i++) {
            acc[i] = new Account(UUID.randomUUID(), initialBalance, Currency.USD);
        }
        double expectedTotal = initialBalance * n;

        // Для каждой пары соседних аккаунтов создаём два встречных Transfer
        List<Transfer> transfers = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            Account from = acc[i];
            Account to   = acc[(i + 1) % n];
            transfers.add(new Transfer(from, to, 50, Currency.USD));
            transfers.add(new Transfer(to, from, 50, Currency.USD));
        }

        TransferSimulation simulation = new TransferSimulation(transfers, LOG);

        Thread simThread = new Thread(simulation::executeTransfers);
        simThread.start();
        simThread.join(30_000);

        double actualTotal = 0;
        for (Account a : acc) actualTotal += a.getBalance();

        assertEquals(expectedTotal, actualTotal,
                String.format(
                        "Инвариант нарушен! Ожидалось: %.0f, получено: %.0f.%n" +
                        "Деньги созданы из воздуха или уничтожены из-за не-атомарного перевода.",
                        expectedTotal, actualTotal));
    }

    /**
     * Наблюдатель ловит «провал» суммарного баланса прямо во время работы
     * симуляции — в промежутке между withdraw() и deposit().
     *
     * Отдельный поток-наблюдатель непрерывно суммирует балансы,
     * пока симуляция работает. Хотя бы раз сумма должна отличаться
     * от ожидаемой.
     */
    @Test
    @DisplayName("BUG #1 — наблюдатель ловит момент, когда деньги «висят в воздухе»")
    void observerCatchesBalanceDipDuringSimulation() throws InterruptedException {
        Account a = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);
        Account b = new Account(UUID.randomUUID(), 0, Currency.USD);
        double expectedTotal = 1_000_000;

        // Только один Transfer: A → B.
        // Каждый из 100 циклов снимает 100 с A и кладёт на B.
        // Между withdraw и deposit наблюдатель должен увидеть сумму < expectedTotal.
        Transfer aToB = new Transfer(a, b, 100, Currency.USD);

        TransferSimulation simulation = new TransferSimulation(List.of(aToB), LOG);

        AtomicBoolean dipObserved = new AtomicBoolean(false);
        AtomicBoolean simDone     = new AtomicBoolean(false);

        // Поток-наблюдатель: смотрит на суммарный баланс пока симуляция жива
        Thread observer = new Thread(() -> {
            while (!simDone.get()) {
                double total = a.getBalance() + b.getBalance();
                if (total != expectedTotal) {
                    dipObserved.set(true);
                }
            }
        });

        Thread simThread = new Thread(() -> {
            simulation.executeTransfers();
            simDone.set(true);
        });

        observer.start();
        simThread.start();
        simThread.join(15_000);
        simDone.set(true);
        observer.join(1_000);

        assertTrue(dipObserved.get(),
                "Наблюдатель не поймал «провал» баланса. " +
                "Возможно, планировщик не дал потокам пересечься — запустите ещё раз.");
    }

    // -------------------------------------------------------------------------
    // BUG #2: Дедлок при встречных переводах
    // -------------------------------------------------------------------------

    /**
     * Классический дедлок через TransferSimulation.executeTransfers():
     *
     *   Thread 1 (aToB): withdraw(A) захватывает lock A, затем пытается deposit(B) → ждёт lock B
     *   Thread 2 (bToA): withdraw(B) захватывает lock B, затем пытается deposit(A) → ждёт lock A
     *
     * Оба потока ждут друг друга вечно.
     * executeTransfers() не вернёт управление никогда.
     *
     * Тест даёт симуляции 5 секунд. Если не завершилась — дедлок.
     */
    @RepeatedTest(5)
    @DisplayName("BUG #2 — дедлок: executeTransfers() зависает при встречных переводах A→B и B→A")
    void deadlockCausesSimulationToHang() throws InterruptedException {
        Account a = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);
        Account b = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);

        // Два встречных Transfer — именно эта пара провоцирует дедлок
        Transfer aToB = new Transfer(a, b, 1, Currency.USD);
        Transfer bToA = new Transfer(b, a, 1, Currency.USD);

        TransferSimulation simulation = new TransferSimulation(
                List.of(aToB, bToA), LOG
        );

        CountDownLatch finished = new CountDownLatch(1);
        Thread simThread = new Thread(() -> {
            simulation.executeTransfers();
            finished.countDown(); // до сюда дойдём только если дедлока нет
        });

        simThread.start();

        boolean completed = finished.await(5, TimeUnit.SECONDS);

        if (!completed) {
            simThread.interrupt(); // пробуем разбудить — не поможет при дедлоке
        }

        assertTrue(completed,
                "ДЕДЛОК! executeTransfers() не завершился за 5 секунд.%n" +
                "Thread aToB захватил lock на A и ждёт lock на B.%n" +
                "Thread bToA захватил lock на B и ждёт lock на A.%n" +
                "Решение: захватывать локи на оба аккаунта в фиксированном порядке (по UUID).");
    }

    /**
     * Дедлок при большем числе аккаунтов: кольцо встречных переводов.
     *   A→B, B→C, C→D, D→A  — прямое кольцо
     *   B→A, C→B, D→C, A→D  — обратное кольцо
     *
     * При параллельном исполнении любая пара соседних Transfer из разных
     * колец может взаимно заблокироваться.
     */
    @Test
    @DisplayName("BUG #2 — дедлок при кольцевых встречных переводах (нагрузочный)")
    void deadlockInRingOfOppositeTransfers() throws InterruptedException {
        int n = 4;
        Account[] acc = new Account[n];
        for (int i = 0; i < n; i++) {
            acc[i] = new Account(UUID.randomUUID(), 1_000_000, Currency.USD);
        }

        // Прямое кольцо: 0→1, 1→2, 2→3, 3→0
        // Обратное кольцо: 1→0, 2→1, 3→2, 0→3
        List<Transfer> transfers = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            transfers.add(new Transfer(acc[i], acc[(i + 1) % n], 1, Currency.USD));
            transfers.add(new Transfer(acc[(i + 1) % n], acc[i], 1, Currency.USD));
        }

        TransferSimulation simulation = new TransferSimulation(transfers, LOG);

        CountDownLatch finished = new CountDownLatch(1);
        Thread simThread = new Thread(() -> {
            simulation.executeTransfers();
            finished.countDown();
        });

        simThread.start();

        boolean completed = finished.await(5, TimeUnit.SECONDS);

        if (!completed) {
            simThread.interrupt();
        }

        assertTrue(completed,
                "ДЕДЛОК при кольцевых встречных переводах! " +
                "executeTransfers() не завершился за 5 секунд. " +
                "Встречные потоки взаимно заблокировали друг друга.");
    }
}
