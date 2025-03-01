package net.sf.l2j.gameserver.data.xml;

import lombok.Getter;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class loads herbs drop rules.<br>
 * TODO parse L2OFF GF (since IL doesn't exist) and introduce the additional droplist concept directly on npc data XMLs.
 */
public class HerbDropData implements IXmlReader {

    @Getter(lazy = true)
    private static final HerbDropData instance = new HerbDropData();

    private final Map<Integer, List<DropCategory>> groups = new HashMap<>();

    private HerbDropData() {
        load();
    }

    @Override
    public void load() {
        parseFile("./data/xml/herbDrops.xml");
        log.info("Loaded {} herbs groups.", groups.size());
    }

    @Override
    public void parseDocument(Document doc, Path path) {
        forEach(doc, "list", listNode -> forEach(listNode, "group", groupNode ->
        {
            final int groupId = parseInteger(groupNode.getAttributes(), "id");
            final List<DropCategory> category = groups.computeIfAbsent(groupId, k -> new ArrayList<>());
            forEach(groupNode, "item", itemNode ->
            {
                final NamedNodeMap attrs = itemNode.getAttributes();
                final int categoryType = parseInteger(attrs, "category");
                final DropData dropDat = new DropData(parseInteger(attrs, "id"), 1, 1, parseInteger(attrs, "chance"));

                boolean catExists = false;
                for (final DropCategory cat : category) {
                    if (cat.getCategoryType() == categoryType) {
                        cat.add(dropDat, false);
                        catExists = true;
                        break;
                    }
                }

                if (!catExists) {
                    final DropCategory cat = new DropCategory(categoryType);
                    cat.add(dropDat, false);
                    category.add(cat);
                }
            });
        }));
    }

    public List<DropCategory> getHerbDroplist(int groupId) {
        return groups.get(groupId);
    }

}