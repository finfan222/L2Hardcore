package net.sf.l2j.gameserver.scripting.script.teleport;

import net.sf.l2j.Config;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Antharas;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Baium;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Sailren;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Valakas;

import java.util.List;

/**
 * This script leads behavior of multiple bosses teleporters.
 * <ul>
 * <li>13001, Heart of Warding : Teleport into Lair of Antharas</li>
 * <li>29055, Teleportation Cubic : Teleport out of Baium zone</li>
 * <li>31859, Teleportation Cubic : Teleport out of Lair of Antharas</li>
 * <li>31384, Gatekeeper of Fire Dragon : Opening some doors</li>
 * <li>31385, Heart of Volcano : Teleport into Lair of Valakas</li>
 * <li>31540, Watcher of Valakas Klein : Teleport into Hall of Flames</li>
 * <li>31686, Gatekeeper of Fire Dragon : Opens doors to Heart of Volcano</li>
 * <li>31687, Gatekeeper of Fire Dragon : Opens doors to Heart of Volcano</li>
 * <li>31759, Teleportation Cubic : Teleport out of Lair of Valakas</li>
 * <li>31862, Angelic Vortex : Baium Teleport (3 different HTMs according of situation)</li>
 * <li>32107, Teleportation Cubic : Teleport out of Sailren Nest</li>
 * <li>32109, Shilen's Stone Statue : Teleport to Sailren Nest</li>
 * </ul>
 */
public class GrandBossTeleporter extends Quest {
    private static final Location BAIUM_IN = new Location(113100, 14500, 10077);
    private static final Location[] BAIUM_OUT =
        {
            new Location(108784, 16000, -4928),
            new Location(113824, 10448, -5164),
            new Location(115488, 22096, -5168)
        };

    private static final Location SAILREN_IN = new Location(27333, -6835, -1970);
    private static final Location[] SAILREN_OUT =
        {
            new Location(10610, -24035, -3676),
            new Location(10703, -24041, -3673),
            new Location(10769, -24107, -3672)
        };

    private static int _valakasPlayersCount = 0;

    public GrandBossTeleporter() {
        super(-1, "teleport");

        addFirstTalkId(29055, 31862);
        addTalkId(13001, 29055, 31859, 31384, 31385, 31540, 31686, 31687, 31759, 31862, 32107, 32109);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = "";

        if (event.equalsIgnoreCase("baium")) {
            final int status = GrandBossManager.getInstance().getBossStatus(29020);
            if (status == Baium.AWAKE) {
                htmltext = "31862-01.htm";
            } else if (status == Baium.DEAD) {
                htmltext = "31862-04.htm";
            }
            // Player is mounted on a wyvern, cancel it.
            else if (player.isFlying()) {
                htmltext = "31862-05.htm";
            }
            // Player hasn't blooded fabric, cancel it.
            else if (!player.getInventory().hasItems(4295)) {
                htmltext = "31862-03.htm";
            }
            // All is ok, take the item and teleport the player inside.
            else {
                takeItems(player, 4295, 1);

                // allow entry for the player for the next 30 secs.
                ZoneManager.getInstance().getZoneById(110002, BossZone.class).allowPlayerEntry(player, 30);
                player.teleportTo(BAIUM_IN, 0);
            }
        } else if (event.equalsIgnoreCase("baium_story")) {
            htmltext = "31862-02.htm";
        } else if (event.equalsIgnoreCase("baium_exit")) {
            player.teleportTo(Rnd.get(BAIUM_OUT), 100);
        } else if (event.equalsIgnoreCase("31540")) {
            if (player.getInventory().hasItems(7267)) {
                takeItems(player, 7267, 1);
                player.teleportTo(183813, -115157, -3303, 0);
                player.getMemos().set("GrandBossTeleporters_Valakas", true);
            } else {
                htmltext = "31540-06.htm";
            }
        }
        return htmltext;
    }

    @Override
    public String onFirstTalk(Npc npc, Player player) {
        String htmltext = "";

        switch (npc.getNpcId()) {
            case 29055:
                htmltext = "29055-01.htm";
                break;

            case 31862:
                final int status = GrandBossManager.getInstance().getBossStatus(29020);
                if (status == Baium.AWAKE) {
                    htmltext = "31862-01.htm";
                } else if (status == Baium.DEAD) {
                    htmltext = "31862-04.htm";
                } else {
                    htmltext = "31862-00.htm";
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        String htmltext = "";

        int status;
        switch (npc.getNpcId()) {
            case 13001:
                status = GrandBossManager.getInstance().getBossStatus(Antharas.ANTHARAS);
                if (status == Antharas.FIGHTING) {
                    htmltext = "13001-02.htm";
                } else if (status == Antharas.DEAD) {
                    htmltext = "13001-01.htm";
                } else if (status == Antharas.DORMANT || status == Antharas.WAITING) {
                    if (player.getInventory().hasItems(3865)) {
                        takeItems(player, 3865, 1);
                        ZoneManager.getInstance().getZoneById(110001, BossZone.class).allowPlayerEntry(player, 30);

                        player.teleportTo(175300 + Rnd.get(-350, 350), 115180 + Rnd.get(-1000, 1000), -7709, 0);

                        if (status == Antharas.DORMANT) {
                            GrandBossManager.getInstance().setBossStatus(Antharas.ANTHARAS, Antharas.WAITING);
                            ScriptData.getInstance().getQuest("Antharas").startQuestTimer("beginning", null, null, Config.WAIT_TIME_ANTHARAS);
                        }
                    } else {
                        htmltext = "13001-03.htm";
                    }
                }
                break;

            case 31859:
                player.teleportTo(79800 + Rnd.get(600), 151200 + Rnd.get(1100), -3534, 0);
                break;

            case 31385:
                status = GrandBossManager.getInstance().getBossStatus(Valakas.VALAKAS);
                if (status == Valakas.DORMANT || status == Valakas.WAITING) {
                    if (_valakasPlayersCount >= 200) {
                        htmltext = "31385-03.htm";
                    } else if (player.getMemos().containsKey("GrandBossTeleporters_Valakas")) {
                        player.getMemos().unset("GrandBossTeleporters_Valakas");
                        ZoneManager.getInstance().getZoneById(110010, BossZone.class).allowPlayerEntry(player, 30);

                        player.teleportTo(204328, -111874, 70, 300);

                        _valakasPlayersCount++;

                        if (status == Valakas.DORMANT) {
                            GrandBossManager.getInstance().setBossStatus(Valakas.VALAKAS, Valakas.WAITING);
                            ScriptData.getInstance().getQuest("Valakas").startQuestTimer("beginning", null, null, Config.WAIT_TIME_VALAKAS);
                        }
                    } else {
                        htmltext = "31385-04.htm";
                    }
                } else if (status == Valakas.FIGHTING) {
                    htmltext = "31385-02.htm";
                } else {
                    htmltext = "31385-01.htm";
                }
                break;

            case 31384:
                DoorData.getInstance().getDoor(24210004).openMe();
                break;

            case 31686:
                DoorData.getInstance().getDoor(24210006).openMe();
                break;

            case 31687:
                DoorData.getInstance().getDoor(24210005).openMe();
                break;

            case 31540:
                if (_valakasPlayersCount < 50) {
                    htmltext = "31540-01.htm";
                } else if (_valakasPlayersCount < 100) {
                    htmltext = "31540-02.htm";
                } else if (_valakasPlayersCount < 150) {
                    htmltext = "31540-03.htm";
                } else if (_valakasPlayersCount < 200) {
                    htmltext = "31540-04.htm";
                } else {
                    htmltext = "31540-05.htm";
                }
                break;

            case 31759:
                player.teleportTo(150037, -57720, -2976, 250);
                break;

            case 32107:
                player.teleportTo(Rnd.get(SAILREN_OUT), 100);
                break;

            case 32109:
                if (!player.isInParty()) {
                    htmltext = "32109-03.htm";
                } else if (!player.getParty().isLeader(player)) {
                    htmltext = "32109-01.htm";
                } else {
                    if (player.getInventory().hasItems(8784)) {
                        status = GrandBossManager.getInstance().getBossStatus(Sailren.SAILREN);
                        if (status == Sailren.DORMANT) {
                            final List<Player> party = player.getParty().getMembers();

                            // Check players conditions.
                            for (Player member : party) {
                                if (member.getStatus().getLevel() < 70) {
                                    return "32109-06.htm";
                                }

                                if (!MathUtil.checkIfInRange(1000, player, member, true)) {
                                    return "32109-07.htm";
                                }
                            }

                            // Take item from party leader.
                            takeItems(player, 8784, 1);

                            final BossZone nest = ZoneManager.getInstance().getZoneById(110011, BossZone.class);

                            // Teleport players.
                            for (Player member : party) {
                                if (nest != null) {
                                    nest.allowPlayerEntry(member, 30);
                                    member.teleportTo(SAILREN_IN, 100);
                                }
                            }
                            GrandBossManager.getInstance().setBossStatus(Sailren.SAILREN, Sailren.FIGHTING);
                            ScriptData.getInstance().getQuest("Sailren").startQuestTimer("beginning", null, null, 60000);
                        } else if (status == Sailren.DEAD) {
                            htmltext = "32109-04.htm";
                        } else {
                            htmltext = "32109-05.htm";
                        }
                    } else {
                        htmltext = "32109-02.htm";
                    }
                }
                break;
        }

        return htmltext;
    }
}