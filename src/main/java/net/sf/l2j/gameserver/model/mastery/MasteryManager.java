package net.sf.l2j.gameserver.model.mastery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.mastery.serialization.MasteryHandlerDeserializer;
import net.sf.l2j.gameserver.network.SystemMessageColor;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.util.ResourceUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * @author finfan
 */
@Slf4j
public class MasteryManager {

    public static final int MIN_CLASS_LEVEL = 1;
    public static final int MIN_LEARN_LEVEL = 20;
    public static final int MAX_MASTERY_LEARN = 7;
    public static final int MAX_MASTERY_TREE = MAX_MASTERY_LEARN * 3;

    @Getter(lazy = true)
    private static final MasteryManager instance = new MasteryManager();

    private static final String buttonLocked = "<button value=\"\" action=\"\" width=40 height=40 back=\"L2HC_UI.m_blocked_i00\" fore=\"L2HC_UI.m_blocked_i00\">";
    private static final String buttonUnlearned = "<button value=\"\" action=\"bypass -h mastery_learn $id\" width=40 height=40 back=\"L2HC_UI.$icon_unlearned_down\" fore=\"L2HC_UI.$icon_unlearned\">";
    private static final String buttonLearned = "<button value=\"\" action=\"bypass -h mastery_descr $id\" width=40 height=40 back=\"L2HC_UI.$icon_learned_down\" fore=\"L2HC_UI.$icon_learned\">";

    @Getter
    private final List<MasteryData> masteries;

    private MasteryManager() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule().addDeserializer(MasteryHandler.class, new MasteryHandlerDeserializer()));
        masteries = ResourceUtil.fromJson("./data/mastery_list.json", MasteryData[].class, mapper);
        log.info("Mastery loaded: {}", masteries.size());
    }

    public MasteryData getById(int id) {
        return masteries.stream().filter(e -> e.getId() == id).findAny().orElseThrow(() -> new NullPointerException("Mastery with id " + id + " not found."));
    }

    public MasteryData getByName(String name) {
        return masteries.stream().filter(e -> e.getName().equals(name)).findAny().orElseThrow(() -> new NullPointerException("Mastery with name " + name + " not found."));
    }

    public MasteryData[] getByBranchLevel(int branchLevel) {
        return masteries.stream().filter(e -> e.getBranchLevel() == branchLevel).toArray(MasteryData[]::new);
    }

    public MasteryData[] getByClassId(ClassId classId) {
        return masteries.stream().filter(e -> ArraysUtil.contains(e.getClassCond(), classId)).toArray(MasteryData[]::new);
    }

    public void requestLearn(Player player, int masteryId) {
        MasteryData data = getById(masteryId);
        if (!isCanLearn(player, data)) {
            return;
        }

        // show description of chosen talent
        player.sendMessage(data.getName() + " (не изучен): " + data.getShortDescr(), SystemMessageColor.GREY_LIGHT);

        ConfirmDlg packet = new ConfirmDlg(SystemMessageId.ARE_YOU_SURE_YOU_WANT_TO_LEARN_THE_S1_MASTERY);
        packet.addString(data.getName());
        packet.addRequesterId(player.getObjectId());
        player.setDialog(new Dialog(player, packet, Map.of("masteryData", data)).send());
    }

    public void showHtmlTree(Player player) {
        if (player.getStatus().getLevel() < MIN_LEARN_LEVEL) {
            player.sendMessage("Вы недостаточно опытны для осваивания мастерства профессий.", SystemMessageColor.RED_LIGHT);
            return;
        }

        if (player.getClassId().getLevel() < MIN_CLASS_LEVEL) {
            player.sendMessage("Вы должны получить профессию прежде чем обрести мастерство.", SystemMessageColor.RED_LIGHT);
            return;
        }

        NpcHtmlMessage html = new NpcHtmlMessage(0);
        ClassId classId = player.getClassId();
        Mastery mastery = player.getMastery();

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<title>").append(String.format("Древо Мастерства [Очки: %d]", player.getMastery().getPoints())).append("</title>");

        sb.append("<body><br><center>");
        sb.append("<table width=288 height=354>");

        Map<Integer, List<MasteryData>> tree = Arrays.stream(getByClassId(classId)).collect(groupingBy(MasteryData::getBranchLevel, LinkedHashMap::new, Collectors.toList()));
        for (List<MasteryData> list : tree.values()) {
            sb.append("<tr>");
            for (MasteryData next : list) {
                String masteryId = String.valueOf(next.getId());
                sb.append("<td>");
                if (mastery.isHasMastery(next.getId())) {
                    sb.append(buttonLearned.replace("$icon_learned", next.getIcon().getLearned())
                        .replace("$icon_learned_down", next.getIcon().getLearned() + "_down")
                        .replace("$id", masteryId));
                } else if (!next.checkRequirements(player)) {
                    sb.append(buttonLocked);
                } else {
                    sb.append(buttonUnlearned.replace("$icon_unlearned", next.getIcon().getUnlearned())
                        .replace("$icon_unlearned_down", next.getIcon().getUnlearned() + "_down")
                        .replace("$id", masteryId));
                }
                sb.append("</td>");
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</center></body>");
        sb.append("</html>");

        html.setHtml(sb.toString());
        player.sendPacket(html);
    }

    public static boolean isCanLearn(Player player, MasteryData data) {
        Mastery mastery = player.getMastery();
        if (mastery.isHasMastery(data.getId())) {
            player.sendMessage(data.getName() + " (изучен): " + data.getFullDescr(), SystemMessageColor.GREEN_LIGHT);
            return false;
        }

        if (player.getDialog() != null || player.isProcessingRequest() || player.isProcessingTransaction()) {
            player.sendMessage("Операция отклонена. Разберитесь со своими делами и попробуйте позже.", SystemMessageColor.RED_LIGHT);
            return false;
        }

        if (mastery.getPoints() < 1) {
            player.sendMessage("У вас недостаточно очков мастерства. Наберитесь опыта и попробуйте снова.", SystemMessageColor.RED_LIGHT);
            return false;
        }

        if (data.getLevelCond() > player.getStatus().getLevel()) {
            player.sendMessage("Этот вид мастерства требует " + data.getLevelCond() + " (или выше) уровень.", SystemMessageColor.RED_LIGHT);
            return false;
        }

        if (mastery.getNextIndex() + 1 == MAX_MASTERY_LEARN) {
            player.sendMessage("Вы больше не можете изучать мастерство, вы исчерпали свой лимит. Для сброса древа мастерства, вам понадобиться предмет 'Thread of Time'.", SystemMessageColor.RED_LIGHT);
            return false;
        }

        return true;
    }
}
