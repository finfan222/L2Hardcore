package net.sf.l2j.gameserver.model.cards;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.data.xml.IXmlReader;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CardManager implements IXmlReader {

    @Getter(lazy = true)
    private static final CardManager instance = new CardManager();

    @Getter
    private final List<CardData> cards = new ArrayList<>();

    private CardManager() {
        load();
    }

    public CardData get(int symbolId) {
        return cards.stream()
            .filter(c -> c.getSymbolId() == symbolId)
            .findAny()
            .orElseThrow(() -> new SymbolNotFoundException("Card with symbolId=" + symbolId + " not exists."));
    }

    @Override
    public void load() {
        parseFile("./data/xml/cards.xml");
        log.info("Loaded {} cards.", cards.size());
    }

    @Override
    public void parseDocument(Document doc, Path path) {

    }

    public void reload() {
        cards.clear();
        load();
    }
}
