package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.Config;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.container.player.Appearance;
import net.sf.l2j.gameserver.model.actor.container.player.SubClass;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.holder.Timestamp;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author finfan
 */
public class PlayerDao {

    private static final CLogger LOGGER = new CLogger(PlayerDao.class.getSimpleName());

    private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,nobless,power_grade) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE obj_id=?";
    private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=? WHERE obj_id=?";
    private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
    private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
    private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
    private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
    private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE char_obj_id=? AND class_index=? ORDER BY buff_index ASC";
    private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";
    private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
    private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";
    private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";
    private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
    private static final String ADD_OR_UPDATE_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,class_index) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE skill_level=VALUES(skill_level)";
    private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
    private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
    private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
    private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
    private static final String UPDATE_TARGET_RECOM_HAVE = "UPDATE characters SET rec_have=? WHERE obj_Id=?";
    private static final String UPDATE_CHAR_RECOM_LEFT = "UPDATE characters SET rec_left=? WHERE obj_Id=?";
    private static final String UPDATE_NOBLESS = "UPDATE characters SET nobless=? WHERE obj_Id=?";

    public static void update(Player player) {
        // Get the exp, level, and sp of base class to store in base table
        final int currentClassIndex = player.getClassIndex();

        player._classIndex = 0;

        PlayerStatus status = player.getStatus();
        Appearance appearance = player.getAppearance();
        final long exp = status.getExp();
        final int level = status.getLevel();
        final int sp = status.getSp();

        player._classIndex = currentClassIndex;

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER)) {
            ps.setInt(1, level);
            ps.setInt(2, status.getMaxHp());
            ps.setDouble(3, status.getHp());
            ps.setInt(4, status.getMaxCp());
            ps.setDouble(5, status.getCp());
            ps.setInt(6, status.getMaxMp());
            ps.setDouble(7, status.getMp());
            ps.setInt(8, appearance.getFace());
            ps.setInt(9, appearance.getHairStyle());
            ps.setInt(10, appearance.getHairColor());
            ps.setInt(11, appearance.getSex().ordinal());
            ps.setInt(12, player.getHeading());

            if (!player.isInObserverMode()) {
                ps.setInt(13, player.getX());
                ps.setInt(14, player.getY());
                ps.setInt(15, player.getZ());
            } else {
                ps.setInt(13, player.getSavedLocation().getX());
                ps.setInt(14, player.getSavedLocation().getY());
                ps.setInt(15, player.getSavedLocation().getZ());
            }

            ps.setLong(16, exp);
            ps.setLong(17, player.getExpBeforeDeath());
            ps.setInt(18, sp);
            ps.setInt(19, player.getKarma());
            ps.setInt(20, player.getPvpKills());
            ps.setInt(21, player.getPkKills());
            ps.setInt(22, player.getClanId());
            ps.setInt(23, player.getRace().ordinal());
            ps.setInt(24, player.getClassId().getId());
            ps.setLong(25, player.getDeleteTimer());
            ps.setString(26, player.getTitle());
            ps.setInt(27, player.getAccessLevel().getLevel());
            ps.setInt(28, player.isOnlineInt());
            ps.setInt(29, player.isIn7sDungeon() ? 1 : 0);
            ps.setInt(30, player.getClanPrivileges());
            ps.setInt(31, player.wantsPeace() ? 1 : 0);
            ps.setInt(32, player.getBaseClass());

            long totalOnlineTime = player.onlineTime;
            if (player.onlineBeginTime > 0) {
                totalOnlineTime += (System.currentTimeMillis() - player.onlineBeginTime) / 1000;
            }

            ps.setLong(33, totalOnlineTime);
            ps.setInt(34, player.getPunishment().getType().ordinal());
            ps.setLong(35, player.getPunishment().getTimer());
            ps.setInt(36, player.isNoble() ? 1 : 0);
            ps.setLong(37, player.getPowerGrade());
            ps.setInt(38, player.getPledgeType());
            ps.setInt(39, player.getLvlJoinedAcademy());
            ps.setLong(40, player.getApprentice());
            ps.setLong(41, player.getSponsor());
            ps.setInt(42, player.getAllianceWithVarkaKetra());
            ps.setLong(43, player.getClanJoinExpiryTime());
            ps.setLong(44, player.getClanCreateExpiryTime());
            ps.setString(45, player.getName());
            ps.setLong(46, player.getDeathPenaltyBuffLevel());
            ps.setInt(47, player.getObjectId());

            ps.execute();
        } catch (final Exception e) {
            LOGGER.error("Couldn't store player base data.", e);
        }
    }

    public static Player restore(int objectId) {
        Player player = null;

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_CHARACTER)) {
            ps.setInt(1, objectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final int activeClassId = rs.getInt("classid");
                    final PlayerTemplate template = PlayerData.getInstance().getTemplate(activeClassId);
                    final Appearance app = new Appearance(rs.getByte("face"), rs.getByte("hairColor"), rs.getByte("hairStyle"), Sex.VALUES[rs.getInt("sex")]);

                    player = new Player(objectId, template, rs.getString("account_name"), app);
                    player.setName(rs.getString("char_name"));
                    player.setLastAccess(rs.getLong("lastAccess"));

                    player.getStatus().setExp(rs.getLong("exp"));
                    player.getStatus().setLevel(rs.getByte("level"));
                    player.getStatus().setSp(rs.getInt("sp"));

                    player.setExpBeforeDeath(rs.getLong("expBeforeDeath"));
                    player.setWantsPeace(rs.getInt("wantspeace") == 1);
                    player.setKarma(rs.getInt("karma"));
                    player.setPvpKills(rs.getInt("pvpkills"));
                    player.setPkKills(rs.getInt("pkkills"));
                    player.setOnlineTime(rs.getLong("onlinetime"));
                    player.setNoble(rs.getInt("nobless") == 1, false);

                    player.setClanJoinExpiryTime(rs.getLong("clan_join_expiry_time"));
                    if (player.getClanJoinExpiryTime() < System.currentTimeMillis()) {
                        player.setClanJoinExpiryTime(0);
                    }

                    player.setClanCreateExpiryTime(rs.getLong("clan_create_expiry_time"));
                    if (player.getClanCreateExpiryTime() < System.currentTimeMillis()) {
                        player.setClanCreateExpiryTime(0);
                    }

                    player.setPowerGrade(rs.getInt("power_grade"));
                    player.setPledgeType(rs.getInt("subpledge"));

                    final int clanId = rs.getInt("clanid");
                    if (clanId > 0) {
                        player.setClan(ClanTable.getInstance().getClan(clanId));
                    }

                    if (player.getClan() != null) {
                        if (player.getClan().getLeaderId() != player.getObjectId()) {
                            if (player.getPowerGrade() == 0) {
                                player.setPowerGrade(5);
                            }

                            player.setClanPrivileges(player.getClan().getPrivilegesByRank(player.getPowerGrade()));
                        } else {
                            player.setClanPrivileges(Clan.CP_ALL);
                            player.setPowerGrade(1);
                        }
                    } else {
                        player.setClanPrivileges(Clan.CP_NOTHING);
                    }

                    player.setDeleteTimer(rs.getLong("deletetime"));
                    player.setTitle(rs.getString("title"));
                    player.setAccessLevel(rs.getInt("accesslevel"));
                    player.setUptime(System.currentTimeMillis());
                    player.setRecomHave(rs.getInt("rec_have"));
                    player.setRecomLeft(rs.getInt("rec_left"));

                    player._classIndex = 0;
                    try {
                        player.setBaseClass(rs.getInt("base_class"));
                    } catch (final Exception e) {
                        player.setBaseClass(activeClassId);
                    }

                    // Restore Subclass Data (cannot be done earlier in function)
                    if (restoreSubClass(player) && activeClassId != player.getBaseClass()) {
                        for (final SubClass subClass : player.getSubClasses().values()) {
                            if (subClass.getClassId() == activeClassId) {
                                player._classIndex = subClass.getClassIndex();
                            }
                        }
                    }

                    // Subclass in use but doesn't exist in DB - a possible subclass cheat has been attempted. Switching to base class.
                    if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass()) {
                        player.setClassId(player.getBaseClass());
                    } else {
                        player._activeClass = activeClassId;
                    }

                    player.setApprentice(rs.getInt("apprentice"));
                    player.setSponsor(rs.getInt("sponsor"));
                    player.setLvlJoinedAcademy(rs.getInt("lvl_joined_academy"));
                    player.setIsIn7sDungeon(rs.getInt("isin7sdungeon") == 1);

                    player.getPunishment().load(rs.getInt("punish_level"), rs.getLong("punish_timer"));

                    CursedWeaponManager.getInstance().checkPlayer(player);

                    player.setAllianceWithVarkaKetra(rs.getInt("varka_ketra_ally"));

                    player.setDeathPenaltyBuffLevel(rs.getInt("death_penalty_level"));

                    // Set the position of the Player.
                    player.getPosition().set(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("heading"));

                    // Set Hero status if it applies
                    if (HeroManager.getInstance().isActiveHero(objectId)) {
                        player.setHero(true);
                    }

                    // Set pledge class rank.
                    player.setPledgeClass(ClanMember.calculatePledgeClass(player));

                    // Retrieve from the database all secondary data of this Player and reward expertise/lucky skills if necessary.
                    // Note that Clan, Noblesse and Hero skills are given separately and not here.
                    player.restoreCharData();
                    player.giveSkills();

                    // buff and status icons
                    if (Config.STORE_SKILL_COOLTIME) {
                        restoreEffects(player);
                    }

                    // Restore current CP, HP and MP values
                    final double currentHp = rs.getDouble("curHp");

                    player.getStatus().setCpHpMp(rs.getDouble("curCp"), currentHp, rs.getDouble("curMp"));

                    if (currentHp < 0.5) {
                        player.setIsDead(true);
                        player.getStatus().stopHpMpRegeneration();
                    }

                    // Restore pet if it exists in the world.
                    final Pet pet = World.getInstance().getPet(player.getObjectId());
                    if (pet != null) {
                        player.setSummon(pet);
                        pet.setOwner(player);
                    }

                    player.refreshWeightPenalty();
                    player.refreshExpertisePenalty();
                    player.refreshHennaList();
                    player.restoreFriendList();

                    // Retrieve the name and ID of the other characters assigned to this account.
                    try (PreparedStatement ps2 = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?")) {
                        ps2.setString(1, player.getAccountName());
                        ps2.setInt(2, objectId);

                        try (ResultSet rs2 = ps2.executeQuery()) {
                            while (rs2.next()) {
                                player.getAccountChars().put(rs2.getInt("obj_Id"), rs2.getString("char_name"));
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't restore player data.", e);
        }

        return player;
    }

    static void create(Player player) {
        // Add the player in the characters table of the database
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_CHARACTER)) {
            ps.setString(1, player.getAccountName());
            ps.setInt(2, player.getObjectId());
            ps.setString(3, player.getName());
            ps.setInt(4, player.getStatus().getLevel());
            ps.setInt(5, player.getStatus().getMaxHp());
            ps.setDouble(6, player.getStatus().getHp());
            ps.setInt(7, player.getStatus().getMaxCp());
            ps.setDouble(8, player.getStatus().getCp());
            ps.setInt(9, player.getStatus().getMaxMp());
            ps.setDouble(10, player.getStatus().getMp());
            ps.setInt(11, player.getAppearance().getFace());
            ps.setInt(12, player.getAppearance().getHairStyle());
            ps.setInt(13, player.getAppearance().getHairColor());
            ps.setInt(14, player.getAppearance().getSex().ordinal());
            ps.setLong(15, player.getStatus().getExp());
            ps.setInt(16, player.getStatus().getSp());
            ps.setInt(17, player.getKarma());
            ps.setInt(18, player.getPvpKills());
            ps.setInt(19, player.getPkKills());
            ps.setInt(20, player.getClanId());
            ps.setInt(21, player.getRace().ordinal());
            ps.setInt(22, player.getClassId().getId());
            ps.setLong(23, player.getDeleteTimer());
            ps.setInt(24, player.hasDwarvenCraft() ? 1 : 0);
            ps.setString(25, player.getTitle());
            ps.setInt(26, player.getAccessLevel().getLevel());
            ps.setInt(27, player.isOnlineInt());
            ps.setInt(28, player.isIn7sDungeon() ? 1 : 0);
            ps.setInt(29, player.getClanPrivileges());
            ps.setInt(30, player.wantsPeace() ? 1 : 0);
            ps.setInt(31, player.getBaseClass());
            ps.setInt(32, player.isNoble() ? 1 : 0);
            ps.setLong(33, 0);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Couldn't create player %s for %s account.", player.getName(), player.getAccountName()), e);
        }
    }

    static boolean restoreSubClass(Player player) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_SUBCLASSES)) {
            ps.setInt(1, player.getObjectId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final SubClass subClass = new SubClass(rs.getInt("class_id"),
                        rs.getInt("class_index"), rs.getLong("exp"),
                        rs.getInt("sp"), rs.getByte("level"));

                    // Enforce the correct indexing of _subClasses against their class indexes.
                    player.getSubClasses().put(subClass.getClassIndex(), subClass);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't restore subclasses for {}.", e, player.getName());
            return false;
        }

        return true;
    }

    static boolean createSubClass(Player player, int classId, int classIndex) {
        AtomicBoolean status = new AtomicBoolean(false);
        player.lock(() -> {
            Map<Integer, SubClass> subClasses = player.getSubClasses();
            if (subClasses.size() == 3 || classIndex == 0 || subClasses.containsKey(classIndex)) {
                return;
            }

            final SubClass subclass = new SubClass(classId, classIndex);

            try (Connection con = ConnectionPool.getConnection();
                 PreparedStatement ps = con.prepareStatement(ADD_CHAR_SUBCLASS)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, subclass.getClassId());
                ps.setLong(3, subclass.getExp());
                ps.setInt(4, subclass.getSp());
                ps.setInt(5, subclass.getLevel());
                ps.setInt(6, subclass.getClassIndex());
                ps.execute();
            } catch (final Exception e) {
                LOGGER.error("Couldn't add subclass for {}.", e, player.getName());
                return;
            }

            subClasses.put(subclass.getClassIndex(), subclass);
            PlayerData.getInstance().getTemplate(classId).getSkills().stream().filter(s -> s.getMinLvl() <= 40)
                .collect(Collectors.groupingBy(IntIntHolder::getId, Collectors.maxBy(Player.COMPARE_SKILLS_BY_LVL)))
                .forEach((i, s) -> saveSkill(player, s.get().getSkill(), classIndex));
            status.compareAndSet(false, true);
        });
        return status.get();
    }

    public static boolean updateSubClass(Player player, int classIndex, int newClassId) {
        player.lock(() -> {
            Map<Integer, SubClass> subClasses = player.getSubClasses();
            try (Connection con = ConnectionPool.getConnection()) {
                // Remove all henna info stored for this sub-class.
                try (PreparedStatement ps = con.prepareStatement(DELETE_CHAR_HENNAS)) {
                    ps.setInt(1, player.getObjectId());
                    ps.setInt(2, classIndex);
                    ps.execute();
                }

                // Remove all shortcuts info stored for this sub-class.
                try (PreparedStatement ps = con.prepareStatement(DELETE_CHAR_SHORTCUTS)) {
                    ps.setInt(1, player.getObjectId());
                    ps.setInt(2, classIndex);
                    ps.execute();
                }

                // Remove all effects info stored for this sub-class.
                try (PreparedStatement ps = con.prepareStatement(DELETE_SKILL_SAVE)) {
                    ps.setInt(1, player.getObjectId());
                    ps.setInt(2, classIndex);
                    ps.execute();
                }

                // Remove all skill info stored for this sub-class.
                try (PreparedStatement ps = con.prepareStatement(DELETE_CHAR_SKILLS)) {
                    ps.setInt(1, player.getObjectId());
                    ps.setInt(2, classIndex);
                    ps.execute();
                }

                // Remove all basic info stored about this sub-class.
                try (PreparedStatement ps = con.prepareStatement(DELETE_CHAR_SUBCLASS)) {
                    ps.setInt(1, player.getObjectId());
                    ps.setInt(2, classIndex);
                    ps.execute();
                }
            } catch (final Exception e) {
                LOGGER.error("Couldn't modify subclass for {} to class index {}.", e, player.getName(), classIndex);
            } finally {
                subClasses.remove(classIndex);
            }
        });

        return createSubClass(player, newClassId, classIndex);
    }

    static void updateSubClass(Player player) {
        if (player.getSubClasses().isEmpty()) {
            return;
        }

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_CHAR_SUBCLASS)) {
            for (SubClass subClass : player.getSubClasses().values()) {
                ps.setLong(1, subClass.getExp());
                ps.setInt(2, subClass.getSp());
                ps.setInt(3, subClass.getLevel());
                ps.setInt(4, subClass.getClassId());
                ps.setInt(5, player.getObjectId());
                ps.setInt(6, subClass.getClassIndex());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            LOGGER.error("Couldn't store subclass data.", e);
        }
    }

    static void saveSkill(Player player, L2Skill skill, int classIndex) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(ADD_OR_UPDATE_SKILL)) {
            ps.setInt(1, player.getObjectId());
            ps.setInt(2, skill.getId());
            ps.setInt(3, skill.getLevel());
            ps.setInt(4, (classIndex > -1) ? classIndex : player.getClassIndex());
            ps.executeUpdate();
        } catch (final Exception e) {
            LOGGER.error("Couldn't store player skill.", e);
        }
    }

    static void restoreSkills(Player player) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR)) {
            ps.setInt(1, player.getObjectId());
            ps.setInt(2, player.getClassIndex());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    player.addSkill(SkillTable.getInstance().getInfo(rs.getInt("skill_id"), rs.getInt("skill_level")), false);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't restore player skills.", e);
        }
    }

    static void restoreEffects(Player player) {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(RESTORE_SKILL_SAVE)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, player.getClassIndex());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final int effectCount = rs.getInt("effect_count");
                        final int effectCurTime = rs.getInt("effect_cur_time");
                        final long reuseDelay = rs.getLong("reuse_delay");
                        final long systime = rs.getLong("systime");
                        final int restoreType = rs.getInt("restore_type");

                        final L2Skill skill = SkillTable.getInstance().getInfo(rs.getInt("skill_id"), rs.getInt("skill_level"));
                        if (skill == null) {
                            continue;
                        }

                        final long remainingTime = systime - System.currentTimeMillis();
                        if (remainingTime > 10) {
                            player.disableSkill(skill, remainingTime);
                            player.addTimeStamp(skill, reuseDelay, systime);
                        }

                        // Restore Type 1 : The remaning skills lost effect upon logout but were still under a high reuse delay.
                        if (restoreType > 0) {
                            continue;
                        }

                        // Restore Type 0 : These skills were still in effect on the character upon logout. Some of which were self casted and might still have a long reuse delay which also is restored.
                        if (skill.hasEffects()) {
                            for (final EffectTemplate template : skill.getEffectTemplates()) {
                                final AbstractEffect effect = template.getEffect(player, player, skill);
                                if (effect != null) {
                                    effect.setCount(effectCount);
                                    effect.setTime(effectCurTime);
                                    effect.scheduleEffect();
                                }
                            }
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(DELETE_SKILL_SAVE)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, player.getClassIndex());
                ps.executeUpdate();
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't restore effects.", e);
        }
    }

    static void restoreRecommends(Player player) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(RESTORE_CHAR_RECOMS)) {
            ps.setInt(1, player.getObjectId());

            try (ResultSet rset = ps.executeQuery()) {
                while (rset.next()) {
                    player.getRecomChars().add(rset.getInt("target_id"));
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't restore recommendations.", e);
        }
    }

    static void saveEffects(Player player, boolean storeEffects) {
        if (!Config.STORE_SKILL_COOLTIME) {
            return;
        }

        try (Connection con = ConnectionPool.getConnection()) {
            // Delete all stored effects.
            try (PreparedStatement ps = con.prepareStatement(DELETE_SKILL_SAVE)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, player.getClassIndex());
                ps.executeUpdate();
            }

            int index = 0;
            final List<Integer> storedSkills = new ArrayList<>();

            try (PreparedStatement ps = con.prepareStatement(ADD_SKILL_SAVE)) {
                // Store all effects with their remaining reuse delays. 'restore_type'= 0.
                if (storeEffects) {
                    for (AbstractEffect effect : player.getAllEffects()) {
                        // Don't bother with HoT effects.
                        if (effect.getEffectType() == EffectType.HEAL_OVER_TIME) {
                            continue;
                        }

                        // Don't bother to reprocess the same skill id/level pair.
                        final L2Skill skill = effect.getSkill();
                        if (storedSkills.contains(skill.getReuseHashCode())) {
                            continue;
                        }

                        // Store the skill, to avoid to process it twice.
                        storedSkills.add(skill.getReuseHashCode());

                        // Don't bother about herbs and toggles.
                        if (skill.isToggle()) {
                            continue;
                        }

                        ps.setInt(1, player.getObjectId());
                        ps.setInt(2, skill.getId());
                        ps.setInt(3, skill.getLevel());
                        ps.setInt(4, effect.getCount());
                        ps.setInt(5, effect.getTime());

                        final Timestamp timestamp = player.getReuseTimeStamp().get(skill.getReuseHashCode());
                        if (timestamp != null && timestamp.hasNotPassed()) {
                            ps.setLong(6, timestamp.getReuse());
                            ps.setDouble(7, timestamp.getStamp());
                        } else {
                            ps.setLong(6, 0);
                            ps.setDouble(7, 0);
                        }

                        ps.setInt(8, 0);
                        ps.setInt(9, player.getClassIndex());
                        ps.setInt(10, ++index);
                        ps.addBatch();
                    }
                }

                // Store the leftover reuse delays. 'restore_type' 1.
                for (final Map.Entry<Integer, Timestamp> entry : player.getReuseTimeStamp().entrySet()) {
                    // Don't bother to reprocess the same skill id/level pair.
                    final int hash = entry.getKey();
                    if (storedSkills.contains(hash)) {
                        continue;
                    }

                    final Timestamp timestamp = entry.getValue();
                    if (timestamp != null && timestamp.hasNotPassed()) {
                        // Store the skill, to avoid to process it twice.
                        storedSkills.add(hash);

                        ps.setInt(1, player.getObjectId());
                        ps.setInt(2, timestamp.getId());
                        ps.setInt(3, timestamp.getValue());
                        ps.setInt(4, -1);
                        ps.setInt(5, -1);
                        ps.setLong(6, timestamp.getReuse());
                        ps.setDouble(7, timestamp.getStamp());
                        ps.setInt(8, 1);
                        ps.setInt(9, player.getClassIndex());
                        ps.setInt(10, ++index);
                        ps.addBatch();
                    }
                }

                ps.executeBatch();
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't store player effects.", e);
        }
    }

    static void deleteSkill(Player player, int skillId) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE_SKILL_FROM_CHAR)) {
            ps.setInt(1, skillId);
            ps.setInt(2, player.getObjectId());
            ps.setInt(3, player.getClassIndex());
            ps.execute();
        } catch (final Exception e) {
            LOGGER.error("Couldn't delete player skill.", e);
        }
    }

    static void updateRecommends(Player player, Player target) {
        try (Connection con = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(ADD_CHAR_RECOM)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, target.getObjectId());
                ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement(UPDATE_TARGET_RECOM_HAVE)) {
                ps.setInt(1, target.getRecomHave());
                ps.setInt(2, target.getObjectId());
                ps.execute();
            }

            try (PreparedStatement ps = con.prepareStatement(UPDATE_CHAR_RECOM_LEFT)) {
                ps.setInt(1, player.getRecomLeft());
                ps.setInt(2, player.getObjectId());
                ps.execute();
            }
        } catch (final Exception e) {
            LOGGER.error("Couldn't update player recommendations.", e);
        }
    }

    static void updateNobles(Player player) {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_NOBLESS)) {
            ps.setBoolean(1, player.isNoble());
            ps.setInt(2, player.getObjectId());
            ps.executeUpdate();
        } catch (final Exception e) {
            LOGGER.error("Couldn't update nobles status for {}.", e, player.getName());
        }
    }
}
