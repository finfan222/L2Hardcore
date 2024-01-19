package net.sf.l2j.gameserver.model.buylist;

import lombok.Data;
import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.taskmanager.BuyListTaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Product {
    private static final CLogger LOGGER = new CLogger(Product.class.getName());

    private static final String INSERT = "INSERT INTO buylists (buylist_id,item_id,count) VALUES(?,?,?) ON DUPLICATE KEY UPDATE count=VALUES(count)";
    private static final String DELETE = "DELETE FROM buylists WHERE buylist_id=? AND item_id=?";

    private final int buyListId;
    private final Item item;
    private final int price;
    private final int limit;

    private AtomicInteger counter;

    public Product(int buyListId, StatSet set) {
        this.buyListId = buyListId;
        this.item = ItemData.getInstance().getTemplate(set.getInteger("id"));
        this.price = set.getInteger("price", 0);
        int stock = set.getInteger("limit", -1);
        if (item instanceof Armor || item instanceof Weapon) {
            int referencePrice = (int) Math.sqrt(item.getReferencePrice());
            if (referencePrice > 0) {
                int maxPrice = (int) Math.sqrt(Integer.MAX_VALUE);
                limit = maxPrice / referencePrice;
            } else {
                limit = stock;
            }
        } else {
            this.limit = stock;
        }

        if (hasLimitedStock()) {
            counter = new AtomicInteger(limit);
        }
    }

    public int getItemId() {
        return item.getItemId();
    }

    public int getCount() {
        if (counter == null) {
            return 0;
        }

        return Math.max(counter.get(), 0);
    }

    public void setCount(int currentCount) {
        if (counter == null) {
            return;
        }

        counter.set(currentCount);
    }

    public boolean decreaseCount(int val) {
        if (counter == null) {
            return false;
        }

        // We test product addition and save result, but only if count has been affected.
        boolean result = counter.addAndGet(-val) >= 0;
        if (result) {
            BuyListTaskManager.getInstance().add(this);
        }

        return result;
    }

    public boolean hasLimitedStock() {
        return limit > -1;
    }

    public void save() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setInt(1, getBuyListId());
            ps.setInt(2, getItemId());
            ps.setInt(3, getCount());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Couldn't save product for buylist id:{} and item id: {}.", e, getBuyListId(), getItemId());
        }
    }

    public void delete() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE)) {
            ps.setInt(1, getBuyListId());
            ps.setInt(2, getItemId());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Couldn't delete product for buylist id:{} and item id: {}.", e, getBuyListId(), getItemId());
        }
    }
}