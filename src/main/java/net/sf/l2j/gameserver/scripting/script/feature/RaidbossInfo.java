package net.sf.l2j.gameserver.scripting.script.feature;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.scripting.Quest;

import java.util.HashMap;
import java.util.Map;

public class RaidbossInfo extends Quest {
    private static final String BOSS_CLASS_TYPE = "RaidBoss";

    private static final Map<Integer, Location> RADARS = new HashMap<>();

    private static final int[] NPCs =
        {
            31729,
            31730,
            31731,
            31732,
            31733,
            31734,
            31735,
            31736,
            31737,
            31738,
            31775,
            31776,
            31777,
            31778,
            31779,
            31780,
            31781,
            31782,
            31783,
            31784,
            31785,
            31786,
            31787,
            31788,
            31789,
            31790,
            31791,
            31792,
            31793,
            31794,
            31795,
            31796,
            31797,
            31798,
            31799,
            31800,
            31801,
            31802,
            31803,
            31804,
            31805,
            31806,
            31807,
            31808,
            31809,
            31810,
            31811,
            31812,
            31813,
            31814,
            31815,
            31816,
            31817,
            31818,
            31819,
            31820,
            31821,
            31822,
            31823,
            31824,
            31825,
            31826,
            31827,
            31828,
            31829,
            31830,
            31831,
            31832,
            31833,
            31834,
            31835,
            31836,
            31837,
            31838,
            31839,
            31840,
            31841
        };

    public RaidbossInfo() {
        super(-1, "feature");

        addTalkId(NPCs);

        // Add all Raid Bosses locations.
        for (Spawn spawn : SpawnTable.getInstance().getSpawns()) {
            if (spawn.getTemplate().isType(BOSS_CLASS_TYPE)) {
                RADARS.put(spawn.getNpcId(), spawn.getLoc());
            }
        }
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        if (!StringUtil.isDigit(event)) {
            return event;
        }

        final Location loc = RADARS.get(Integer.parseInt(event));
        if (loc != null) {
            player.getRadarList().addMarker(loc);
        }

        return null;
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        return "info.htm";
    }
}