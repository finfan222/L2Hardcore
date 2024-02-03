package net.sf.l2j.gameserver.taskmanager;

import it.sauronsoftware.cron4j.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.gameserver.model.buylist.Product;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public final class BuyListTaskManager implements Runnable {

    @Getter(lazy = true)
    private static final BuyListTaskManager instance = new BuyListTaskManager();

    private final List<Product> products = new CopyOnWriteArrayList<>();

    private static final String CRON_PATTERN = "0 7 * * 1"; // “At 07:00 on Monday.”

    private BuyListTaskManager() {
        Scheduler scheduler = new Scheduler();
        scheduler.schedule(CRON_PATTERN, this);
        scheduler.start();
        log.info("Buy list task manager was initialized. Next schedule of restock items: {}", CRON_PATTERN);
    }

    @Override
    public void run() {
        // List is empty, skip.
        if (products.isEmpty()) {
            return;
        }

        // Loop all characters.
        for (Product product : products) {
            product.setCount(product.getLimit());
            product.delete();
            products.remove(product);
        }
    }

    public void add(Product product) {
        products.add(product);
        product.save();
    }

}