package net.sf.l2j.gameserver.model.graveyard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.sf.l2j.Config;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.enums.HtmColor;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.events.OnDie;
import net.sf.l2j.gameserver.events.OnDieLethal;
import net.sf.l2j.gameserver.events.OnRevive;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author finfan
 */
public class GraveyardManager implements Runnable {

    private static final CLogger LOGGER = new CLogger(GraveyardManager.class.getSimpleName());

    @Getter(lazy = true)
    private static final GraveyardManager instance = new GraveyardManager();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    public static final int TOMBSTONE_ID = 50009;

    private final Map<Integer, Necrologue> necrologues = new ConcurrentHashMap<>();

    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Necrologue {

        public String accountName;
        public String name;
        public int level;
        public String zoneName;
        public int objectId;
        public int clanId;
        public boolean isClanLeader;
        public boolean isInParty;
        public boolean isPartyLeader;
        public boolean isInPartyMatchRoom;
        public String killerName;
        public DieReason reason;
        public int x, y, z, heading;
        public boolean isEternal;
        public LocalDateTime timestamp;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Necrologue that = (Necrologue) o;
            return objectId == that.objectId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectId);
        }

    }

    private GraveyardManager() {
        GraveyardDao.delete();
        GlobalEventListener.register(OnDie.class).forEach(this::onDie);
        GlobalEventListener.register(OnRevive.class).forEach(this::onRevive);

        ThreadPool.scheduleAtFixedRate(this, 10000, 10000);

        GraveyardDao.restore();
        LOGGER.info("Graveyard manager is initialized.");
    }

    public boolean isDeadMan(int objectId) {
        return necrologues.containsKey(objectId);
    }

    public DieReason validateDieReason(Player victim, Creature killer, DieReason reason) {
        LOGGER.info("[0][validateDieReason] {}", victim, killer, reason);

        switch (reason) {
            case DROWN, FALL:
                return reason;

            default:
                if (killer instanceof Attackable attackable) {
                    if (attackable.isGuard()) {
                        // killed by GUARD
                        reason = DieReason.GUARD;
                    } else if (attackable instanceof Monster) {
                        // killed by MONSTER
                        reason = DieReason.MONSTER;
                    }
                } else if (killer instanceof Playable playable) {
                    Player killerPlayer = playable.getActingPlayer();
                    if (killerPlayer.getKarma() > 0) {
                        // killer with karma qualifies like PK if victim is no have karma, PVP otherwise
                        reason = victim.getKarma() > 0 ? DieReason.PVP : DieReason.PK;
                    } else {
                        if (reason != DieReason.MORTAL_COMBAT) {
                            reason = DieReason.PVP;
                        }
                        // killer without karma against victim qualify like PVP (even if victim has karma)
                    }
                }
                return reason;
        }
    }

    private void onDie(OnDie event) {
        LOGGER.info("[1][GraveyardManager.onDie] {}", event);
        if (event.getVictim() instanceof Player player) {
            if (validateDeath(player, event.getKiller(), event.getReason())) {
                // add dead man to queue for future delete
                addDeadMan(event);

                // send system message about dead and next time to remove body/delete character
                player.sendPacket(SystemMessage
                    .getSystemMessage(SystemMessageId.YOU_DIED_IF_YOU_WILL_NOT_RESURRECT_UNTIL_S1_YOU_DIE_FOREVER)
                    .addString(DATE_FORMATTER.format(necrologues.get(player.getObjectId()).timestamp)));
            }
        }
    }

    private boolean validateDeath(Player player, Creature killer, DieReason reason) {
        LOGGER.info("[2][GraveyardManager.validateDeath] {}, {}, {}", player, killer, reason);

        // no action if player died in siege/peace/town/pvp zone
        if (player.isInsideZone(ZoneId.TOWN) || player.isInsideZone(ZoneId.PEACE)
            || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE)) {
            return false;
        }

        // no action if player died and his level was less than 15
        int level = player.getStatus().getLevel();
        if (level < 15) {
            return false;
        }

        // no action if NO killer and reason is NONE
        if (killer == null && reason == DieReason.NONE) {
            return false;
        }
        // no action if reason is not qualified like hardcore death
        else {
            return reason.isHardcoreDeath();
        }
    }

    private void addDeadMan(OnDie event) {
        Player player = (Player) event.getVictim();
        DieReason reason = event.getReason();
        Creature killer = event.getKiller();

        Necrologue necrologue = Necrologue.builder()
            .accountName(player.getAccountName())
            .level(player.getStatus().getLevel())
            .clanId(player.getClanId())
            .isInParty(player.isInParty())
            .isPartyLeader(player.isInParty() && player.getParty().isLeader(player))
            .objectId(player.getObjectId())
            .isClanLeader(player.isClanLeader())
            .isInPartyMatchRoom(player.isInPartyMatchRoom())
            .name(player.getName())
            .zoneName(MapRegionData.getInstance().getClosestTownName(player.getX(), player.getY())) //todo: zone name by region (squares)
            .reason(reason)
            .killerName(killer.getName())
            .x(player.getX())
            .y(player.getY())
            .z(player.getZ())
            .heading(player.getHeading())
            .isEternal(false) //todo: premium
            .timestamp(LocalDateTime.now().plusMinutes(TimeUnit.MILLISECONDS.toMinutes(Config.HARDCORE_DELAY_AFTER_DEATH)))
            .build();

        necrologues.put(player.getObjectId(), necrologue);
        LOGGER.info("[3][GraveyardManager.addDeadMan] {}", necrologue);
    }

    @Override
    public void run() {
        for (Necrologue necrologue : necrologues.values()) {
            if (!necrologue.timestamp.isBefore(LocalDateTime.now())) {
                continue;
            }

            Player player = World.getInstance().getPlayer(necrologue.objectId);
            if (player == null || !player.isOnline()) {
                deleteOffline(necrologue.objectId, necrologue.name, necrologue.clanId, necrologue.accountName);
            } else {
                deleteOnline(player);
            }

            tryCreateTombstone(necrologue);
            GlobalEventListener.notify(new OnDieLethal(necrologue.name, necrologue.reason));
            necrologues.remove(necrologue.objectId);
        }
    }

    private void deleteOnline(Player player) {
        LOGGER.info("[4][GraveyardManager.deleteOnline] {}", player);
        Clan clan = player.getClan();
        if (clan != null) {
            int objectId = player.getObjectId();
            String name = player.getName();

            // get all clan members except @player
            List<ClanMember> members = clan.getMembers()
                .stream()
                .filter(member -> member.getObjectId() != player.getObjectId())
                .toList();

            // if player is clan leader
            if (player.isClanLeader()) {
                // try to find candidates to be a leader
                // if no candidates, dissolve ally and destroy clan
                if (members.isEmpty()) {
                    clan.dissolveAlly(player);
                    ClanTable.getInstance().destroyClan(clan);
                }
                // if candidates exist, we change leader and remove player from clan with broadcast 'removePlayerFromClan' notify
                else {
                    changeClanLeader(clan, members, objectId, name);
                }
            }
            // if player is not a clan leader
            else {
                // just remove player from clan and notify clan about 'removePlayerFromClan'
                clan.broadcastToMembersExcept(null, new PledgeShowMemberListDelete(name),
                    SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(name));
                clan.removeClanMember(objectId, 0);
            }
        }

        // kick player from game
        LoginServerThread.getInstance().kickPlayer(player.getAccountName());
        // delete player from DB with all values
        GameClient.deleteCharByObjId(player.getObjectId());
    }

    private void deleteOffline(int objectId, String name, int clanId, String accountName) {
        LOGGER.info("[4][GraveyardManager.deleteOffline] {}, {}, {}", objectId, name, clanId);
        Clan clan = ClanTable.getInstance().getClan(clanId);
        if (clan != null) {
            // get all clan members except @player
            List<ClanMember> members = clan.getMembers().stream()
                .filter(member -> member.getObjectId() != objectId).toList();

            // if objectId is clan leaderId
            if (clan.getLeaderId() == objectId) {
                // try to find candidates to be a leader
                // if no candidates, dissolve ally and destroy clan
                if (members.isEmpty()) {
                    clan.dissolveAlly();
                    ClanTable.getInstance().destroyClan(clan);
                }
                // if candidates exist, we change leader and broadcast 'removePlayerFromClan' notify
                else {
                    changeClanLeader(clan, members, objectId, name);
                }
            }
            // if objectId is not a clan leaderId
            else {
                // just remove player from clan and notify clan about 'removePlayerFromClan'
                clan.broadcastToMembersExcept(null, new PledgeShowMemberListDelete(name),
                    SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(name));
                clan.removeClanMember(objectId, 0);
            }
        }

        // kick player from game
        LoginServerThread.getInstance().kickPlayer(accountName);
        // delete player from DB with all values
        GameClient.deleteCharByObjId(objectId);
    }

    private void changeClanLeader(Clan clan, List<ClanMember> members, int objectId, String name) {
        LOGGER.info("[5][GraveyardManager.changeClanLeader] {}, {}, {}, {}", clan, members, objectId, name);
        members.stream()
            .max(Comparator.comparing(ClanMember::getLevel)) // pick high level as a new clan leader
            .ifPresentOrElse(e -> {
                clan.setNewLeader(e);
                clan.broadcastToMembersExcept(null, new PledgeShowMemberListDelete(name),
                    SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(name));
                clan.removeClanMember(objectId, 0);
            }, () -> {
                throw new NullPointerException("Clan member not found for change clan leader. Error on playerObjectId=" + objectId + " death handling.");
            });
    }

    private void tryCreateTombstone(Necrologue necrologue) {
        LOGGER.info("[6][GraveyardManager.tryCreateTombstone] {}", necrologue);
        PostScript ps = PostScript.builder()
            .name(necrologue.name + " Lvl. " + necrologue.level)
            .heading(necrologue.heading)
            .x(necrologue.x)
            .y(necrologue.y)
            .z(necrologue.z)
            .date(LocalDate.now())
            .message(tryGenerateMessage(necrologue.name, necrologue.killerName, necrologue.reason, necrologue.zoneName))
            .reason(necrologue.reason)
            .isEternal(false) //todo: premium feature 1$
            .build();

        GraveyardDao.create(ps);
    }

    private String tryGenerateMessage(String playerName, String killerName, DieReason reason, String zoneName) {
        LOGGER.info("[7][GraveyardManager.tryGenerateMessage] {}, {}, {}, {}", playerName, killerName, reason, zoneName);
        StringBuilder builder = new StringBuilder();
        builder.append("<center>Здесь расстался с жизнью один из нас</center><br>");
        return switch (reason) {
            /*
             * PlayerName стал жертвой кровожадного KillerName недалеко от ZoneName, который выпотрошил его внутренности и, возможно, даже сожрал. Монстру все пожелают: «приятного аппетита», а падшему – «покойся с миром, брат»
             */
            case MONSTER -> builder.append(HtmColor.LEVEL.asColored(playerName))
                .append(" стал жертвой кровожадного ")
                .append(HtmColor.LEVEL.asColored(killerName)).append(" недалеко от ").append(HtmColor.LEVEL.asColored(zoneName)).append(", ")
                .append("который выпотрошил его внутренности и, возможно, даже сожрал. Монстру все пожелают: «приятного аппетита», а падшему – «покойся с миром, брат»").toString();
            /*
             * PlayerName не рассчитал свои силы и захлебнулся, утонув в водах ZoneName. Умение хорошо плавать и надолго задерживать дыхание - может спасти вам жизнь! Не пренебрегайте «Breath of Eva»!
             */
            case DROWN ->
                builder.append(HtmColor.LEVEL.asColored(playerName)).append(" не рассчитал свои силы и захлебнулся, утонув в водах ")
                    .append(HtmColor.LEVEL.asColored(zoneName)).append(".<br>")
                    .append("Умение хорошо плавать и надолго задерживать дыхание - может спасти вам жизнь! Не пренебрегайте «Breath of Eva»!").toString();
            /*
             * PlayerName разбился упав с высоты на просторах ZoneName. Всегда нужно смотреть под ноги и осознавать, что природа, может убивать не хуже меча!
             */
            case FALL ->
                builder.append(HtmColor.LEVEL.asColored(playerName)).append(" разбился упав с высоты на просторах ")
                    .append(HtmColor.LEVEL.asColored(zoneName)).append(".<br>")
                    .append("Всегда нужно смотреть под ноги. Невнимательность или случайность - убивает!").toString();
            /*
             * PlayerName нарушил закон окрестностей ZoneName, за что и поплатился!<br> Приговор в исполнение привел KillerName.<br>Закон в Эльморадене - одинаков и суров для всех. Нарушение закона - карается смертью.<br><br>Не нарушайте закон пожалуйста.
             */
            case GUARD -> builder.append(HtmColor.LEVEL.asColored(playerName)).append(" нарушил закон окрестностей ")
                .append(HtmColor.LEVEL.asColored(zoneName)).append(", за что и поплатился!<br>")
                .append("Приговор в исполнение привел ").append(HtmColor.LEVEL.asColored(killerName)).append(".<br>")
                .append("Закон в Эльморадене - одинаков и суров для всех. Нарушение закона - карается смертью.<br><br>Не нарушайте закон пожалуйста.").toString();
            default ->
                throw new UnsupportedOperationException(String.format("Die reason %s is not implemented.", reason));
        };
    }

    private void onRevive(OnRevive event) {
        LOGGER.info("[-1][GraveyardManager.onRevive] {}", event);
        if (event.getVictim() instanceof Player player) {
            necrologues.remove(player.getObjectId());
        }
    }

}
