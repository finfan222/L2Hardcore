package net.sf.l2j.gameserver.model.mastery;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author finfan
 */
@Slf4j
public class MasteryManager {

    private static final String LOCKED_ABILITY_BUTTON = "<button value=\"\" action=\"\" width=32 height=32 back=\"Icon.skill_locked\" fore=\"Icon.skill_locked\">";

    @Getter(lazy = true)
    private static final MasteryManager instance = new MasteryManager();

    private static final List<String> htmlFiles = new ArrayList<>();

    private MasteryManager() {
        try (Connection con = ConnectionPool.getConnection()) {
            PreparedStatement st = con.prepareStatement("""
                CREATE TABLE IF NOT EXISTS `character_mastery` (
                  `object_id` int(10) UNSIGNED NOT NULL,
                  `points` int(10) NULL DEFAULT NULL,
                  PRIMARY KEY (`object_id`) USING BTREE,
                  CONSTRAINT `fk_mastery_to_characters` FOREIGN KEY (`object_id`) REFERENCES `characters` (`obj_Id`) ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;
                """);
            st.executeUpdate();

            st = con.prepareStatement("""
                CREATE TABLE IF NOT EXISTS `character_mastery_list` (
                  `object_id` int(10) UNSIGNED NOT NULL,
                  `mastery_type` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
                  PRIMARY KEY (`object_id`) USING BTREE,
                  CONSTRAINT `fk_mastery_list_to_characters` FOREIGN KEY (`object_id`) REFERENCES `characters` (`obj_Id`) ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;
                """);
            st.executeUpdate();
        } catch (SQLException e) {
            log.error("Error on creating mastery tables.", e);
        }

        Arrays.stream(ClassId.values()).filter(e -> e.getLevel() == 3).forEach(e -> htmlFiles.add(generateFileName(e)));
        log.info("Loaded HTML class mastery branches: {}", htmlFiles.size());
    }

    public void requestLearn(Player player, MasteryType type) {
        if (!player.getMastery().isCanLearn(player, type)) {
            return;
        }

        ConfirmDlg packet = new ConfirmDlg(SystemMessageId.ARE_YOU_SURE_YOU_WANT_TO_LEARN_THE_S1_MASTERY);
        packet.addString(type.getCapitalizedName());
        packet.addRequesterId(player.getObjectId());
        player.setDialog(new Dialog(player, packet, Map.of("masteryType", type)).send());
    }

    public void showMasteryList(Player player) {
        if (player.getStatus().getLevel() < 20) {
            player.sendMessage("Вы недостаточно опытны для осваивания мастерства профессий.");
            return;
        }

        if (player.getClassId().getLevel() < 1) {
            player.sendMessage("Вы должны получить профессию прежде чем обрести мастерство.");
            return;
        }

        NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
        String file = htmlFiles.stream().filter(f -> f.contains(player.getClassId().name().toLowerCase())).findAny().orElse(null);
        if (file == null) {
            throw new RuntimeException("Mastery tree not found for classId " + player.getClassId());
        }
        npcHtmlMessage.setFile("data/html/mastery/" + file + ".htm");
        int classLevel = player.getClassId().getLevel();
        if (classLevel < 3) {
            npcHtmlMessage.replace("%3rd.*", LOCKED_ABILITY_BUTTON + "</td>");
            if (classLevel < 2) {
                npcHtmlMessage.replace("%2nd.*", LOCKED_ABILITY_BUTTON + "</td>");
            }
        }
        player.sendPacket(npcHtmlMessage);
    }

    private String generateFileName(ClassId classId) {
        List<String> list = generateClassTree(classId, new ArrayList<>()).stream().map(ClassId::name).toList();
        StringBuilder sb = new StringBuilder();
        list.forEach(className -> sb.append(className.toLowerCase().replace("_", "")).append("_"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private List<ClassId> generateClassTree(ClassId classId, List<ClassId> tree) {
        if (classId.getLevel() == 0 || classId.getParent() == null) {
            tree.sort(Comparator.comparing(ClassId::getLevel));
            return tree;
        }

        if (!tree.contains(classId)) {
            tree.add(classId);
        }

        return generateClassTree(classId.getParent(), tree);
    }

}
