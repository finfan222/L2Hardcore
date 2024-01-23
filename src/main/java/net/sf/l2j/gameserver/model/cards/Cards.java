package net.sf.l2j.gameserver.model.cards;

import lombok.Setter;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.events.OnSubclassChange;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cards {

    private static final CLogger LOGGER = new CLogger(Cards.class.getSimpleName());

    public static final int MAX_CARDS = 3;
    public static final int PRICE_INSERT = 25_000;
    public static final int PRICE_SAFE_REMOVE = 1_000_000;
    public static final int PRICE_REMOVE = 100_000;

    private final Player player;

    private final CardEntity[] cards = new CardEntity[MAX_CARDS];

    @Setter
    private boolean isSafe;
    private int STR;
    private int DEX;
    private int CON;
    private int INT;
    private int MEN;
    private int WIT;

    public Cards(Player player) {
        this.player = player;
        this.player.getEventListener().subscribe().cast(OnSubclassChange.class).forEach(this::onSubclassChange);
    }

    public int getStat(String param) {
        return switch (param) {
            case "STR" -> STR;
            case "DEX" -> DEX;
            case "CON" -> CON;
            case "INT" -> INT;
            case "MEN" -> MEN;
            case "WIT" -> WIT;
            default -> throw new RuntimeException();
        };
    }

    public int getRemovePrice() {
        return isSafe ? PRICE_SAFE_REMOVE : PRICE_REMOVE;
    }

    public int getNextPrice() {
        return (getEquippedCards().size() + 1) * PRICE_INSERT;
    }

    private int findNextSlot() {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public void insertCard(CardData data) {
        int nextSlot = findNextSlot();
        if (nextSlot == -1) {
            player.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            player.sendPacket(SystemMessageId.SYMBOLS_FULL);
            return;
        }

        if (!player.reduceAdena("Card", PRICE_INSERT, player.getCurrentFolk(), true)) {
            player.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            return;
        }

        if (!player.destroyItemByItemId("Card", data.getItemId(), 1, player, true)) {
            player.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
            return;
        }

        addCard(data, nextSlot);
        CardsDao.update(cards[nextSlot]);
    }

    public void addCard(CardData data, int slotId) {
        cards[slotId] = CardEntity.builder()
            .slotId(slotId)
            .objectId(player.getObjectId())
            .classIndex(player.getClassIndex())
            .symbolId(data.getSymbolId()).build();

        recalculate(cards[slotId], false);
    }

    public void removeCard(int symbolId) {
        CardEntity card = Stream.of(cards)
            .filter(c -> c.getData().getSymbolId() == symbolId)
            .findAny()
            .orElse(null);

        if (card != null) {
            recalculate(card, true);
            player.reduceAdena("Card", getRemovePrice(), player, true);
            if (isSafe) {
                player.addItem("Card", card.getData().getItemId(), 1, player, true);
            }
            CardsDao.delete(player.getObjectId(), player.getClassIndex(), card.getSymbolId());
        }
    }

    public void recalculate(CardEntity entity, boolean removing) {
        int bonusSTR = entity.getData().getBonusSTR();
        int bonusDEX = entity.getData().getBonusDEX();
        int bonusCON = entity.getData().getBonusCON();
        int bonusINT = entity.getData().getBonusINT();
        int bonusMEN = entity.getData().getBonusMEN();
        int bonusWIT = entity.getData().getBonusWIT();
        if (bonusSTR > 0) {
            STR = removing ? STR - bonusSTR : STR + bonusSTR;
        } else if (bonusSTR < 0) {
            STR = removing ? STR + bonusSTR : STR - bonusSTR;
        }
        if (bonusDEX > 0) {
            DEX = removing ? DEX - bonusDEX : DEX + bonusDEX;
        } else if (bonusDEX < 0) {
            DEX = removing ? DEX + bonusDEX : DEX - bonusDEX;
        }
        if (bonusCON > 0) {
            CON = removing ? CON - bonusCON : CON + bonusCON;
        } else if (bonusCON < 0) {
            CON = removing ? CON + bonusCON : DEX - bonusCON;
        }
        if (bonusINT > 0) {
            INT = removing ? INT - bonusINT : INT + bonusINT;
        } else if (bonusINT < 0) {
            INT = removing ? INT + bonusINT : INT - bonusINT;
        }
        if (bonusMEN > 0) {
            MEN = removing ? MEN - bonusMEN : MEN + bonusMEN;
        } else if (bonusMEN < 0) {
            MEN = removing ? MEN + bonusMEN : MEN - bonusMEN;
        }
        if (bonusWIT > 0) {
            WIT = removing ? WIT - bonusWIT : WIT + bonusWIT;
        } else if (bonusWIT < 0) {
            WIT = removing ? WIT + bonusWIT : WIT - bonusWIT;
        }

        L2Skill skill = entity.getData().getSkill();
        if (removing) {
            player.removeSkill(skill.getId(), false);
        } else {
            player.addSkill(skill, false);
        }

        LOGGER.info("[CARDS]: Recalculating stats and skills by card.");
    }

    public List<CardEntity> getEquippedCards() {
        return Stream.of(cards).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean isFull() {
        return getEquippedCards().size() == MAX_CARDS;
    }

    @Deprecated(since = "Пока не нужен, но после тестов всё станет ясно.")
    public boolean isAlreadyInserted(CardData data) {
        return Stream.of(cards).filter(Objects::nonNull).anyMatch(c -> c.getData() == data);
    }

    public void refresh() {
        Stream.of(cards).filter(Objects::nonNull).forEach(c -> recalculate(c, false));
        player.sendPacket(new HennaInfo(player));
    }

    public boolean isEmpty() {
        return getEquippedCards().isEmpty();
    }

    private void onSubclassChange(OnSubclassChange event) {
        // recalculate cards for removing
        Stream.of(cards).filter(Objects::nonNull).forEach(card -> recalculate(card, true));
        // set cards as null in every slot
        Arrays.fill(cards, null);
        // restore new cards from DB for subclass and recalculate it
        CardsDao.restore(player);
    }
}
