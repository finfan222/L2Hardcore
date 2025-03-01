package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.data.manager.DayNightManager;
import net.sf.l2j.gameserver.data.manager.FenceManager;
import net.sf.l2j.gameserver.data.manager.RaidBossManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Fence;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminSpawn implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
        {
            "admin_list_spawns",
            "admin_show_spawns",
            "admin_spawn",
            "admin_spawn_index",
            "admin_unspawnall",
            "admin_respawnall",
            "admin_spawn_reload",
            "admin_npc_index",
            "admin_spawn_once",
            "admin_show_npcs",
            "admin_spawnnight",
            "admin_spawnday",
            "admin_spawnfence",
            "admin_deletefence",
            "admin_listfence",
            "admin_delete"
        };

    @Override
    public void useAdminCommand(String command, Player player) {
        if (command.startsWith("admin_list_spawns")) {
            int npcId = 0;

            try {
                String[] params = command.split(" ");
                Pattern pattern = Pattern.compile("[0-9]*");
                Matcher regexp = pattern.matcher(params[1]);

                if (regexp.matches()) {
                    npcId = Integer.parseInt(params[1]);
                } else {
                    params[1] = params[1].replace('_', ' ');
                    npcId = NpcData.getInstance().getTemplateByName(params[1]).getNpcId();
                }
            } catch (Exception e) {
                // If the parameter wasn't ok, then take the current target.
                final WorldObject targetWorldObject = player.getTarget();
                if (targetWorldObject instanceof Npc) {
                    npcId = ((Npc) targetWorldObject).getNpcId();
                }
            }

            // Load static Htm.
            final NpcHtmlMessage html = new NpcHtmlMessage(0);
            html.setFile("data/html/admin/listspawns.htm");

            // Generate data.
            final StringBuilder sb = new StringBuilder();

            int index = 0, x, y, z;
            String name = "";

            for (Spawn spawn : SpawnTable.getInstance().getSpawns()) {
                if (npcId == spawn.getNpcId()) {
                    index++;
                    name = spawn.getTemplate().getName();

                    final Npc npc = spawn.getNpc();
                    if (npc != null) {
                        x = npc.getX();
                        y = npc.getY();
                        z = npc.getZ();
                    } else {
                        x = spawn.getLocX();
                        y = spawn.getLocY();
                        z = spawn.getLocZ();
                    }
                    StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_teleport ", x, " ", y, " ", z, "\">", index, " - (", x, " ", y, " ", z, ")", "</a></td></tr>");
                }
            }

            if (index == 0) {
                html.replace("%npcid%", "?");
                html.replace("%list%", "<tr><td>The parameter you entered as npcId is invalid.</td></tr>");
            } else {
                html.replace("%npcid%", name + " (" + npcId + ")");
                html.replace("%list%", sb.toString());
            }

            player.sendPacket(html);
        } else if (command.equals("admin_show_spawns")) {
            sendFile(player, "spawns.htm");
        } else if (command.startsWith("admin_spawn_index")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                st.nextToken();
                int level = Integer.parseInt(st.nextToken());
                int from = 0;
                try {
                    from = Integer.parseInt(st.nextToken());
                } catch (NoSuchElementException nsee) {
                }
                showMonsters(player, level, from);
            } catch (Exception e) {
                sendFile(player, "spawns.htm");
            }
        } else if (command.equals("admin_show_npcs")) {
            sendFile(player, "npcs.htm");
        } else if (command.startsWith("admin_npc_index")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                st.nextToken();
                String letter = st.nextToken();
                int from = 0;
                try {
                    from = Integer.parseInt(st.nextToken());
                } catch (NoSuchElementException nsee) {
                }
                showNpcs(player, letter, from);
            } catch (Exception e) {
                sendFile(player, "npcs.htm");
            }
        } else if (command.startsWith("admin_unspawnall")) {
            World.toAllOnlinePlayers(SystemMessage.getSystemMessage(SystemMessageId.NPC_SERVER_NOT_OPERATING));
            RaidBossManager.getInstance().cleanUp(false);
            DayNightManager.getInstance().cleanUp();
            World.getInstance().deleteVisibleNpcSpawns();
            AdminData.getInstance().broadcastMessageToGMs("NPCs' unspawn is now complete.");
        } else if (command.startsWith("admin_spawnday")) {
            DayNightManager.getInstance().spawnCreatures(false);
            AdminData.getInstance().broadcastMessageToGMs("Spawning day creatures spawns.");
        } else if (command.startsWith("admin_spawnnight")) {
            DayNightManager.getInstance().spawnCreatures(true);
            AdminData.getInstance().broadcastMessageToGMs("Spawning night creatures spawns.");
        } else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload")) {
            // make sure all spawns are deleted
            RaidBossManager.getInstance().cleanUp(false);
            DayNightManager.getInstance().cleanUp();
            World.getInstance().deleteVisibleNpcSpawns();
            // now respawn all
            NpcData.getInstance().reload();
            SpawnTable.getInstance().reload();
            RaidBossManager.getInstance().reload();
            SevenSignsManager.getInstance().spawnSevenSignsNPC();
            AdminData.getInstance().broadcastMessageToGMs("NPCs' respawn is now complete.");
        } else if (command.startsWith("admin_spawnfence")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                st.nextToken();
                int type = Integer.parseInt(st.nextToken());
                int sizeX = (Integer.parseInt(st.nextToken()) / 100) * 100;
                int sizeY = (Integer.parseInt(st.nextToken()) / 100) * 100;
                int height = 1;
                if (st.hasMoreTokens()) {
                    height = Math.min(Integer.parseInt(st.nextToken()), 3);
                }

                FenceManager.getInstance().addFence(player.getX(), player.getY(), player.getZ(), type, sizeX, sizeY, height);

                listFences(player);
            } catch (Exception e) {
                player.sendMessage("Usage: //spawnfence <type> <width> <length> [height]");
            }
        } else if (command.startsWith("admin_deletefence")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            st.nextToken();
            try {
                WorldObject worldObject = World.getInstance().getObject(Integer.parseInt(st.nextToken()));
                if (worldObject instanceof Fence) {
                    FenceManager.getInstance().removeFence((Fence) worldObject);

                    if (st.hasMoreTokens()) {
                        listFences(player);
                    }
                } else {
                    player.sendPacket(SystemMessageId.INVALID_TARGET);
                }
            } catch (Exception e) {
                player.sendMessage("Usage: //deletefence <objectId>");
            }
        } else if (command.startsWith("admin_listfence")) {
            listFences(player);
        } else if (command.startsWith("admin_spawn")) {
            StringTokenizer st = new StringTokenizer(command, " ");
            try {
                String cmd = st.nextToken();
                String id = st.nextToken();
                int respawnTime = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 60;

                if (cmd.equalsIgnoreCase("admin_spawn_once")) {
                    spawn(player, id, respawnTime, false);
                } else {
                    spawn(player, id, respawnTime, true);
                }
            } catch (Exception e) {
                sendFile(player, "spawns.htm");
            }
        } else if (command.startsWith("admin_delete")) {
            final WorldObject targetWorldObject = player.getTarget();
            if (!(targetWorldObject instanceof Npc)) {
                player.sendPacket(SystemMessageId.INVALID_TARGET);
                return;
            }

            final Npc targetNpc = (Npc) targetWorldObject;

            final Spawn spawn = targetNpc.getSpawn();
            if (spawn != null) {
                spawn.setRespawnState(false);

                if (RaidBossManager.getInstance().getBossSpawn(spawn.getNpcId()) != null) {
                    RaidBossManager.getInstance().deleteSpawn(spawn);
                } else {
                    SpawnTable.getInstance().deleteSpawn(spawn, true);
                }
            }
            targetNpc.deleteMe();

            player.sendMessage("Deleted " + targetNpc.getName() + " from " + targetNpc.getObjectId() + ".");
        }
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    private void spawn(Player player, String monsterId, int respawnTime, boolean permanent) {
        final WorldObject targetWorldObject = getTarget(WorldObject.class, player, true);

        NpcTemplate template;

        // First parameter was an ID number
        if (monsterId.matches("[0-9]*")) {
            template = NpcData.getInstance().getTemplate(Integer.parseInt(monsterId));
        }
        // First parameter wasn't just numbers, so go by name not ID
        else {
            monsterId = monsterId.replace('_', ' ');
            template = NpcData.getInstance().getTemplateByName(monsterId);
        }

        try {
            final Spawn spawn = new Spawn(template);
            spawn.setLoc(targetWorldObject.getX(), targetWorldObject.getY(), targetWorldObject.getZ(), player.getHeading());
            spawn.setRespawnDelay(respawnTime);

            if (template.isType("RaidBoss")) {
                if (RaidBossManager.getInstance().getBossSpawn(spawn.getNpcId()) != null) {
                    player.sendMessage("You cannot spawn another instance of " + template.getName() + ".");
                    return;
                }

                spawn.setRespawnMinDelay(43200);
                spawn.setRespawnMaxDelay(129600);
                RaidBossManager.getInstance().addNewSpawn(spawn, 0, 0, 0, permanent);
            } else {
                SpawnTable.getInstance().addSpawn(spawn, permanent);
                spawn.doSpawn(false);

                if (permanent) {
                    spawn.setRespawnState(true);
                }
            }

            if (!permanent) {
                spawn.setRespawnState(false);
            }

            player.sendMessage("Spawned " + template.getName() + ".");

        } catch (Exception e) {
            player.sendPacket(SystemMessageId.APPLICANT_INFORMATION_INCORRECT);
        }
    }

    private static void showMonsters(Player player, int level, int from) {
        final List<NpcTemplate> mobs = NpcData.getInstance().getTemplates(t -> t.isType("Monster") && t.getLevel() == level);
        final StringBuilder sb = new StringBuilder(200 + mobs.size() * 100);

        StringUtil.append(sb, "<html><title>Spawn Monster:</title><body><p> Level : ", level, "<br>Total Npc's : ", mobs.size(), "<br>");

        int i = from;
        for (int j = 0; i < mobs.size() && j < 50; i++, j++) {
            StringUtil.append(sb, "<a action=\"bypass -h admin_spawn ", mobs.get(i).getNpcId(), "\">", mobs.get(i).getName(), "</a><br1>");
        }

        if (i == mobs.size()) {
            sb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        } else {
            StringUtil.append(sb, "<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index ", level, " ", i, "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        player.sendPacket(html);
    }

    private static void showNpcs(Player player, String starting, int from) {
        final List<NpcTemplate> npcs = NpcData.getInstance().getTemplates(t -> t.isType("Folk") && t.getName().startsWith(starting));
        final StringBuilder sb = new StringBuilder(200 + npcs.size() * 100);

        StringUtil.append(sb, "<html><title>Spawn Monster:</title><body><p> There are ", npcs.size(), " Npcs whose name starts with ", starting, ":<br>");

        int i = from;
        for (int j = 0; i < npcs.size() && j < 50; i++, j++) {
            StringUtil.append(sb, "<a action=\"bypass -h admin_spawn ", npcs.get(i).getNpcId(), "\">", npcs.get(i).getName(), "</a><br1>");
        }

        if (i == npcs.size()) {
            sb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        } else {
            StringUtil.append(sb, "<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index ", starting, " ", i, "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
        }

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        player.sendPacket(html);
    }

    private static void listFences(Player player) {
        final List<Fence> fences = FenceManager.getInstance().getFences();
        final StringBuilder sb = new StringBuilder();

        sb.append("<html><body>Total Fences: " + fences.size() + "<br><br>");
        for (Fence fence : fences) {
            sb.append("<a action=\"bypass -h admin_deletefence " + fence.getObjectId() + " 1\">Fence: " + fence.getObjectId() + " [" + fence.getX() + " " + fence.getY() + " " + fence.getZ() + "]</a><br>");
        }
        sb.append("</body></html>");

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(sb.toString());
        player.sendPacket(html);
    }
}