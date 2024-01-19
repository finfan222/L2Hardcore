package net.sf.l2j.gameserver.taskmanager;

import it.sauronsoftware.cron4j.Scheduler;
import lombok.Getter;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.buylist.Product;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BuyListTaskManager implements Runnable {

    private static final CLogger LOGGER = new CLogger(BuyListTaskManager.class.getSimpleName());

    @Getter(lazy = true)
    private static final BuyListTaskManager instance = new BuyListTaskManager();

    private final List<Product> products = new CopyOnWriteArrayList<>();

    private static final String CRON_PATTERN = "0 7 * * 1"; // “At 07:00 on Monday.”

    private BuyListTaskManager() {
        Scheduler scheduler = new Scheduler();
        scheduler.schedule(CRON_PATTERN, this);
        scheduler.start();
        LOGGER.info("Buy list task manager was initialized. Next schedule of restock items: {}", CRON_PATTERN);
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