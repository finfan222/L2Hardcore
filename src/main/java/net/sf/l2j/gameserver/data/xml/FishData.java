package net.sf.l2j.gameserver.data.xml;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.Fish;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class loads and stores {@link Fish} infos.<br>
 * TODO Plain wrong values and system, have to be reworked entirely.
 */
public class FishData implements IXmlReader {
    private final List<Fish> _fish = new ArrayList<>();

    protected FishData() {
        load();
    }

    @Override
    public void load() {
        parseFile("./data/xml/fish.xml");
        log.info("Loaded {} fish.", _fish.size());
    }

    @Override
    public void parseDocument(Document doc, Path path) {
        forEach(doc, "list", listNode -> forEach(listNode, "fish", fishNode -> _fish.add(new Fish(parseAttributes(fishNode)))));
    }

    /**
     * Get a random {@link Fish} based on level, type and group.
     *
     * @param lvl : the fish level to check.
     * @param type : the fish type to check.
     * @param group : the fish group to check.
     * @return a Fish with good criterias.
     */
    public Fish getFish(int lvl, int type, int group) {
        return Rnd.get(_fish.stream().filter(f -> f.getLevel() == lvl && f.getType() == type && f.getGroup() == group).collect(Collectors.toList()));
    }

    public static FishData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final FishData INSTANCE = new FishData();
    }
}