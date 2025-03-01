package net.sf.l2j.gameserver.model.actor;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.commons.util.ArraysUtil;
import net.sf.l2j.gameserver.GlobalEventListener;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.communitybbs.model.Forum;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.CursedWeaponManager;
import net.sf.l2j.gameserver.data.manager.DimensionalRiftManager;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.manager.PartyMatchRoomManager;
import net.sf.l2j.gameserver.data.manager.SevenSignsManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.ItemManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.PlayerLevelData;
import net.sf.l2j.gameserver.enums.AiEventType;
import net.sf.l2j.gameserver.enums.CabalType;
import net.sf.l2j.gameserver.enums.DayCycle;
import net.sf.l2j.gameserver.enums.GaugeColor;
import net.sf.l2j.gameserver.enums.LootRule;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.Paperdoll;
import net.sf.l2j.gameserver.enums.PunishmentType;
import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.enums.TeleportMode;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.ClassRace;
import net.sf.l2j.gameserver.enums.actors.ClassType;
import net.sf.l2j.gameserver.enums.actors.MoveType;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.enums.actors.WeightPenalty;
import net.sf.l2j.gameserver.enums.bbs.ForumAccess;
import net.sf.l2j.gameserver.enums.bbs.ForumType;
import net.sf.l2j.gameserver.enums.items.ActionType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.items.ItemLocation;
import net.sf.l2j.gameserver.enums.items.ItemState;
import net.sf.l2j.gameserver.enums.items.ShotType;
import net.sf.l2j.gameserver.enums.items.WeaponType;
import net.sf.l2j.gameserver.enums.skills.EffectFlag;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.enums.skills.Stats;
import net.sf.l2j.gameserver.events.OnDayCycleChange;
import net.sf.l2j.gameserver.events.OnDie;
import net.sf.l2j.gameserver.events.OnHit;
import net.sf.l2j.gameserver.events.OnQuestAccept;
import net.sf.l2j.gameserver.events.OnRevalidateZone;
import net.sf.l2j.gameserver.events.OnSkillHit;
import net.sf.l2j.gameserver.events.OnValidatePosition;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.AccessLevel;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.PetDataEntry;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.ai.type.PlayerAI;
import net.sf.l2j.gameserver.model.actor.attack.CreatureAttack;
import net.sf.l2j.gameserver.model.actor.attack.PlayerAttack;
import net.sf.l2j.gameserver.model.actor.cast.PlayerCast;
import net.sf.l2j.gameserver.model.actor.container.monster.SpoilState;
import net.sf.l2j.gameserver.model.actor.container.npc.RewardInfo;
import net.sf.l2j.gameserver.model.actor.container.player.Appearance;
import net.sf.l2j.gameserver.model.actor.container.player.BlockList;
import net.sf.l2j.gameserver.model.actor.container.player.CubicList;
import net.sf.l2j.gameserver.model.actor.container.player.FishingStance;
import net.sf.l2j.gameserver.model.actor.container.player.HennaList;
import net.sf.l2j.gameserver.model.actor.container.player.MacroList;
import net.sf.l2j.gameserver.model.actor.container.player.Punishment;
import net.sf.l2j.gameserver.model.actor.container.player.QuestList;
import net.sf.l2j.gameserver.model.actor.container.player.RadarList;
import net.sf.l2j.gameserver.model.actor.container.player.RecipeBook;
import net.sf.l2j.gameserver.model.actor.container.player.Request;
import net.sf.l2j.gameserver.model.actor.container.player.ShortcutList;
import net.sf.l2j.gameserver.model.actor.container.player.SubClass;
import net.sf.l2j.gameserver.model.actor.instance.FestivalMonster;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.instance.StaticObject;
import net.sf.l2j.gameserver.model.actor.instance.TamedBeast;
import net.sf.l2j.gameserver.model.actor.move.PlayerMove;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.actor.template.PetTemplate;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.cards.Cards;
import net.sf.l2j.gameserver.model.craft.ManufactureList;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.graveyard.DieReason;
import net.sf.l2j.gameserver.model.group.CommandChannel;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.group.PartyMatchRoom;
import net.sf.l2j.gameserver.model.holder.Timestamp;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.model.item.instance.ItemDao;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.mastery.Mastery;
import net.sf.l2j.gameserver.model.memo.PlayerMemo;
import net.sf.l2j.gameserver.model.multisell.PreparedListContainer;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.model.pledge.ClanMember;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.SystemMessageColor;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ChairSit;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.ClientSetTime;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;
import net.sf.l2j.gameserver.network.serverpackets.ExSetCompassZoneCode;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.L2FriendStatus;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.ObservationMode;
import net.sf.l2j.gameserver.network.serverpackets.ObservationReturn;
import net.sf.l2j.gameserver.network.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreManageListSell;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopManageList;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopMsg;
import net.sf.l2j.gameserver.network.serverpackets.RecipeShopSellList;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.Revive;
import net.sf.l2j.gameserver.network.serverpackets.Ride;
import net.sf.l2j.gameserver.network.serverpackets.SendTradeDone;
import net.sf.l2j.gameserver.network.serverpackets.ServerClose;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SkillList;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StaticObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.TargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.TargetUnselected;
import net.sf.l2j.gameserver.network.serverpackets.TitleUpdate;
import net.sf.l2j.gameserver.network.serverpackets.TradePressOtherOk;
import net.sf.l2j.gameserver.network.serverpackets.TradePressOwnOk;
import net.sf.l2j.gameserver.network.serverpackets.TradeStart;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.network.serverpackets.ship.GetOnVehicle;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.funcs.FuncHenna;
import net.sf.l2j.gameserver.skills.funcs.FuncMaxCpMul;
import net.sf.l2j.gameserver.skills.funcs.FuncRegenCpMul;
import net.sf.l2j.gameserver.skills.handlers.Default;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.PvpFlagTaskManager;
import net.sf.l2j.gameserver.taskmanager.WaterTaskManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * This class represents a player in the world.<br> There is always a client-thread connected to this (except if a
 * player-store is activated upon logout).
 */
@Slf4j
public final class Player extends Playable {

    public static final int REQUEST_TIMEOUT = 15;

    private static final Comparator<GeneralSkillNode> COMPARE_SKILLS_BY_MIN_LVL = Comparator.comparing(GeneralSkillNode::getMinLvl);
    static final Comparator<GeneralSkillNode> COMPARE_SKILLS_BY_LVL = Comparator.comparing(GeneralSkillNode::getValue);

    private GameClient _client;
    private final Map<Integer, String> _chars = new HashMap<>();

    private final String _accountName;
    private final ReentrantLock locker = new ReentrantLock();
    private long _deleteTimer;

    private boolean _isOnline;
    long onlineTime;
    long onlineBeginTime;
    @Getter
    @Setter
    private long lastAccess;
    private long _uptime;

    private int _baseClass;
    int _activeClass;
    int _classIndex;

    private final Map<Integer, SubClass> _subClasses = new ConcurrentSkipListMap<>();

    private final Appearance _appearance;

    private long _expBeforeDeath;
    private int _karma;
    private int _pvpKills;
    private int _pkKills;
    private byte _pvpFlag;
    private int _siegeState;
    private WeightPenalty _weightPenalty = WeightPenalty.NONE;

    private int _lastCompassZone; // the last compass zone update send to the client

    private boolean _isIn7sDungeon;

    private final Punishment _punishment = new Punishment(this);
    private final RecipeBook _recipeBook = new RecipeBook(this);

    private boolean _isInOlympiadMode;
    private boolean _isInOlympiadStart;
    private int _olympiadGameId = -1;
    private int _olympiadSide = -1;

    private DuelState _duelState = DuelState.NO_DUEL;
    private int _duelId;
    private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;

    private Boat _boat;
    private final SpawnLocation _boatPosition = new SpawnLocation(0, 0, 0, 0);

    private boolean _canFeed;
    private PetTemplate _petTemplate;
    private PetDataEntry _petData;
    private int _controlItemId;
    private int _curFeed;
    private Future<?> _mountFeedTask;
    private ScheduledFuture<?> _dismountTask;

    private int _mountType;
    private int _mountNpcId;
    private int _mountLevel;
    private int _mountObjectId;

    private int _throneId;

    private TeleportMode _teleportMode = TeleportMode.NONE;
    private boolean _isCrystallizing;
    private boolean _isCrafting;

    private boolean _isSitting;
    private boolean _isStanding;
    private boolean _isSittingNow;
    private boolean _isStandingNow;

    private final Location _savedLocation = new Location(0, 0, 0);

    private int _recomHave;
    private int _recomLeft;
    private final List<Integer> _recomChars = new ArrayList<>();

    private final PcInventory _inventory = new PcInventory(this);
    private PcWarehouse _warehouse;
    private PcFreight _freight;
    private final List<PcFreight> _depositedFreight = new ArrayList<>();

    private OperateType _operateType = OperateType.NONE;

    private TradeList _activeTradeList;
    private ItemContainer _activeWarehouse;

    private final TradeList _buyList = new TradeList(this);
    private final TradeList _sellList = new TradeList(this);
    private final ManufactureList _manufactureList = new ManufactureList();

    private PreparedListContainer _currentMultiSell;

    private boolean _isNoble;
    private boolean _isHero;

    private Folk _currentFolk;

    private final PlayerMemo _memos = new PlayerMemo(getObjectId());

    private final FishingStance _fishingStance = new FishingStance(this);
    private final ShortcutList _shortcutList = new ShortcutList(this);
    private final MacroList _macroList = new MacroList(this);
    private final HennaList _hennaList = new HennaList(this);
    private final RadarList _radarList = new RadarList(this);
    private final CubicList _cubicList = new CubicList(this);
    private final BlockList _blockList = new BlockList(this);
    private final QuestList _questList = new QuestList(this);

    private Summon _summon;
    private TamedBeast _tamedBeast;

    private int _partyRoom;

    private int _clanId;
    private Clan _clan;
    private int _apprentice;
    private int _sponsor;
    private long _clanJoinExpiryTime;
    private long _clanCreateExpiryTime;
    private int _powerGrade;
    private int _clanPrivileges;
    private int _pledgeClass;
    private int _pledgeType;
    private int _lvlJoinedAcademy;

    private boolean _wantsPeace;

    private int _deathPenaltyBuffLevel;

    private final AtomicInteger _charges = new AtomicInteger();
    private ScheduledFuture<?> _chargeTask;

    private AccessLevel _accessLevel;

    private Location _enterWorld;
    private final Map<String, ExServerPrimitive> _debug = new HashMap<>();

    private Party _party;
    private LootRule _lootRule;

    private Player _activeRequester;
    private long _requestExpireTime;
    private final Request _request = new Request(this);

    private ScheduledFuture<?> _protectTask;

    private long _recentFakeDeathEndTime;
    private boolean _isFakeDeath;

    private int _armorGradePenalty;
    private boolean _weaponGradePenalty;

    private ItemInstance _activeEnchantItem;

    private boolean _inventoryDisable;

    private final Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);

    private final int[] _loto = new int[5];
    private final int[] _race = new int[2];

    private TeamType _team = TeamType.NONE;

    private int _alliedVarkaKetra; // lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra

    private final List<String> _validBypass = new ArrayList<>();
    private final List<String> _validBypass2 = new ArrayList<>();

    private Forum _forumMemo;

    private final Map<Integer, L2Skill> _skills = new ConcurrentSkipListMap<>();
    private final Map<Integer, Timestamp> _reuseTimeStamps = new ConcurrentHashMap<>();

    private int _cursedWeaponEquippedId;

    private int _mailPosition;

    private static final int FALLING_VALIDATION_DELAY = 10000;
    private volatile long _fallingTimestamp;

    private ScheduledFuture<?> _shortBuffTask;
    private int _shortBuffTaskSkillId;

    private int _coupleId;
    @Getter
    @Setter
    private boolean isUnderMarryRequest;

    private final List<Integer> _friendList = new ArrayList<>(); // Related to CB.
    private final List<Integer> _selectedFriendList = new ArrayList<>(); // Related to CB.
    private final List<Integer> _selectedBlocksList = new ArrayList<>(); // Related to CB.

    @Getter
    private volatile Dialog dialog;

    @Getter
    private final SpoilState spoilState;

    @Getter
    private final Cards cards;

    @Getter
    private final Mastery mastery;

    @Setter
    private WeaponType attackType;

    @Getter
    private final AtomicBoolean twoHandGrip = new AtomicBoolean(false);
    @Getter
    @Setter
    private ItemInstance leftHand;

    /**
     * Constructor of Player (use Creature constructor).
     * <ul>
     * <li>Call the Creature constructor to create an empty _skills slot and copy basic Calculator set to this Player</li>
     * <li>Set the name of the Player</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the Player to 1</B></FONT>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2PcTemplate to apply to the Player
     * @param accountName The name of the account including this Player
     * @param app The PcAppearance of the Player
     */
    public Player(int objectId, PlayerTemplate template, String accountName, Appearance app) {
        super(objectId, template);

        getStatus().initializeValues();

        _accountName = accountName;
        _appearance = app;

        // Create an AI
        _ai = new PlayerAI(this);
        cards = new Cards(this);
        spoilState = new SpoilState();
        mastery = new Mastery(this);

        // Retrieve from the database all items of this Player and add them to _inventory
        getInventory().restore();
        getWarehouse();
        getFreight();

        GlobalEventListener.register(OnDayCycleChange.class).forEach(this::onDayCycleChange);

        eventListener.subscribe().cast(OnValidatePosition.class).forEach(this::onValidatePosition);
        eventListener.subscribe().cast(OnQuestAccept.class).forEach(this::onQuestAccept);
    }

    /**
     * Create a new Player and add it in the characters table of the database.
     * <ul>
     * <li>Create a new Player with an account name</li>
     * <li>Set the name, the Hair Style, the Hair Color and the Face type of the Player</li>
     * <li>Add the player in the characters table of the database</li>
     * </ul>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2PcTemplate to apply to the Player
     * @param accountName The name of the Player
     * @param name The name of the Player
     * @param hairStyle The hair style Identifier of the Player
     * @param hairColor The hair color Identifier of the Player
     * @param face The face type Identifier of the Player
     * @param sex The sex type Identifier of the Player
     * @return The Player added to the database or null
     */
    public static Player create(int objectId, PlayerTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, Sex sex) {
        // Create a new Player with an account name
        final Appearance app = new Appearance(face, hairColor, hairStyle, sex);
        final Player player = new Player(objectId, template, accountName, app);

        // Set the name of the Player
        player.setName(name);

        // Set access level
        player.setAccessLevel(Config.DEFAULT_ACCESS_LEVEL);

        // Cache few informations into CharNameTable.
        PlayerInfoTable.getInstance().addPlayer(objectId, accountName, name, player.getAccessLevel().getLevel());

        // Set the base class ID to that of the actual class ID.
        player.setBaseClass(player.getClassId());

        // started SP for player
        player.getStatus().setSp(Config.HARDCORE_START_SP);

        PlayerDao.create(player);
        return player;
    }

    @Override
    public void addFuncsToNewCharacter() {
        // Add Creature functionalities.
        super.addFuncsToNewCharacter();

        addStatFunc(FuncMaxCpMul.getInstance());
        addStatFunc(FuncRegenCpMul.getInstance());

        addStatFunc(FuncHenna.getSTR());
        addStatFunc(FuncHenna.getCON());
        addStatFunc(FuncHenna.getDEX());
        addStatFunc(FuncHenna.getINT());
        addStatFunc(FuncHenna.getMEN());
        addStatFunc(FuncHenna.getWIT());
    }

    public void lock(Runnable runnable) {
        locker.lock();
        try {
            runnable.run();
        } finally {
            locker.unlock();
        }
    }

    @Override
    public PlayerStatus getStatus() {
        return (PlayerStatus) _status;
    }

    @Override
    public void setStatus() {
        _status = new PlayerStatus(this);
    }

    public Appearance getAppearance() {
        return _appearance;
    }

    /**
     * @return the {@link PlayerTemplate} linked to this {@link Player} base class.
     */
    public PlayerTemplate getBaseTemplate() {
        return PlayerData.getInstance().getTemplate(_baseClass);
    }

    @Override
    public PlayerTemplate getTemplate() {
        return (PlayerTemplate) super.getTemplate();
    }

    @Override
    public void setWalkOrRun(boolean value) {
        super.setWalkOrRun(value);

        broadcastUserInfo();
    }

    public void setTemplate(ClassId newclass) {
        super.setTemplate(PlayerData.getInstance().getTemplate(newclass));
    }

    /**
     * Return the AI of the Player (create it if necessary).
     */
    @Override
    public CreatureAI getAI() {
        CreatureAI ai = _ai;
        if (ai == null) {
            synchronized (this) {
                ai = _ai;
                if (ai == null) {
                    _ai = ai = new PlayerAI(this);
                }
            }
        }
        return ai;
    }

    @Override
    public boolean denyAiAction() {
        return super.denyAiAction() || isInStoreMode() || _operateType == OperateType.OBSERVE;
    }

    public void setDialog(Dialog dialog) {
        lock(() -> this.dialog = dialog);
    }

    @Override
    public PlayerMove getMove() {
        return (PlayerMove) _move;
    }

    @Override
    public void setMove() {
        _move = new PlayerMove(this);
    }

    @Override
    public PlayerAttack getAttack() {
        return (PlayerAttack) _attack;
    }

    @Override
    public void setAttack() {
        _attack = new PlayerAttack(this);
    }

    @Override
    public PlayerCast getCast() {
        return (PlayerCast) _cast;
    }

    @Override
    public void setCast() {
        _cast = new PlayerCast(this);
    }

    @Override
    public boolean canBeHealed() {
        return super.canBeHealed() && !isCursedWeaponEquipped();
    }

    /**
     * A newbie is a {@link Player} between level 6 and 25 which didn't yet acquired first occupation change.<br>
     * <br>
     * <b>Since IL, Newbie statut isn't anymore the first character of an account reaching that state, but any.</b>
     *
     * @param checkLowLevel : If true, check also low level requirement.
     * @return True if this {@link Player} can be considered a Newbie.
     */
    public boolean isNewbie(boolean checkLowLevel) {
        return (checkLowLevel) ? (getClassId().getLevel() <= 1 && getStatus().getLevel() >= 6 && getStatus().getLevel() <= 25) : (getClassId().getLevel() <= 1 && getStatus().getLevel() <= 25);
    }

    public void setBaseClass(int baseClass) {
        _baseClass = baseClass;
    }

    public void setBaseClass(ClassId classId) {
        _baseClass = classId.ordinal();
    }

    /**
     * @return True if the state of {@link OperateType} of this {@link Player} is different than NONE.
     */
    @Override
    public boolean isOperating() {
        return _operateType != OperateType.NONE;
    }

    public boolean isCrafting() {
        return _isCrafting;
    }

    public void setCrafting(boolean state) {
        _isCrafting = state;
    }

    /**
     * @return The {@link PlayerMemo} of the current {@link Player}.
     */
    public PlayerMemo getMemos() {
        return _memos;
    }

    /**
     * @return The {@link ShortcutList} of the current {@link Player}.
     */
    public ShortcutList getShortcutList() {
        return _shortcutList;
    }

    /**
     * @return The {@link MacroList} of the current {@link Player}.
     */
    public MacroList getMacroList() {
        return _macroList;
    }

    /**
     * 0 = not involved, 1 = attacker, 2 = defender
     *
     * @return The siege state of the {@link Player}.
     */
    public int getSiegeState() {
        return _siegeState;
    }

    /**
     * Set the siege state of the {@link Player}.
     *
     * @param siegeState : The new value to set.
     */
    public void setSiegeState(int siegeState) {
        _siegeState = siegeState;
    }

    @Override
    public byte getPvpFlag() {
        return _pvpFlag;
    }

    /**
     * Set the PvP flag of the {@link Player}.
     *
     * @param pvpFlag : 0 or 1.
     */
    public void setPvpFlag(int pvpFlag) {
        _pvpFlag = (byte) pvpFlag;
    }

    public void updatePvPFlag(int value) {
        if (getPvpFlag() == value) {
            return;
        }

        setPvpFlag(value);
        sendPacket(new UserInfo(this));

        if (_summon != null) {
            sendPacket(new RelationChanged(_summon, getRelation(this), false));
        }

        broadcastRelationsChanges();
    }

    public int getRelation(Player target) {
        int result = 0;

        // karma and pvp may not be required
        if (getPvpFlag() != 0) {
            result |= RelationChanged.RELATION_PVP_FLAG;
        }
        if (getKarma() > 0) {
            result |= RelationChanged.RELATION_HAS_KARMA;
        }

        if (isClanLeader()) {
            result |= RelationChanged.RELATION_LEADER;
        }

        if (getSiegeState() != 0) {
            result |= RelationChanged.RELATION_INSIEGE;
            if (getSiegeState() != target.getSiegeState()) {
                result |= RelationChanged.RELATION_ENEMY;
            } else {
                result |= RelationChanged.RELATION_ALLY;
            }
            if (getSiegeState() == 1) {
                result |= RelationChanged.RELATION_ATTACKER;
            }
        }

        if (getClan() != null && target.getClan() != null) {
            if (target.getPledgeType() != Clan.SUBUNIT_ACADEMY && getPledgeType() != Clan.SUBUNIT_ACADEMY && target.getClan().isAtWarWith(getClan().getClanId())) {
                result |= RelationChanged.RELATION_1SIDED_WAR;
                if (getClan().isAtWarWith(target.getClan().getClanId())) {
                    result |= RelationChanged.RELATION_MUTUAL_WAR;
                }
            }
        }
        return result;
    }

    @Override
    public void revalidateZone(boolean force) {
        super.revalidateZone(force);

        try {
            if (isInsideZone(ZoneId.SIEGE)) {
                if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) {
                    return;
                }

                _lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
                sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
            } else if (isInsideZone(ZoneId.PVP)) {
                if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE) {
                    return;
                }

                _lastCompassZone = ExSetCompassZoneCode.PVPZONE;
                sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
            } else if (isIn7sDungeon()) {
                if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE) {
                    return;
                }

                _lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
                sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
            } else if (isInsideZone(ZoneId.PEACE)) {
                if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE) {
                    return;
                }

                _lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
                sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
            } else {
                if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE) {
                    return;
                }

                if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) {
                    updatePvPStatus();
                }

                _lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
                sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
            }
        } finally {
            GlobalEventListener.notify(new OnRevalidateZone(this, _lastCompassZone));
        }
    }

    /**
     * @return the PK counter of the Player.
     */
    public int getPkKills() {
        return _pkKills;
    }

    /**
     * Set the PK counter of the Player.
     *
     * @param pkKills A number.
     */
    public void setPkKills(int pkKills) {
        _pkKills = pkKills;
    }

    /**
     * @return The _deleteTimer of the Player.
     */
    public long getDeleteTimer() {
        return _deleteTimer;
    }

    /**
     * Set the _deleteTimer of the Player.
     *
     * @param deleteTimer Time in ms.
     */
    public void setDeleteTimer(long deleteTimer) {
        _deleteTimer = deleteTimer;
    }

    /**
     * @return The current weight of the Player.
     */
    public int getCurrentWeight() {
        return _inventory.getTotalWeight();
    }

    /**
     * @return The number of recommandation obtained by the Player.
     */
    public int getRecomHave() {
        return _recomHave;
    }

    /**
     * Set the number of recommandations obtained by the Player (Max : 255).
     *
     * @param value Number of recommandations obtained.
     */
    public void setRecomHave(int value) {
        _recomHave = MathUtil.limit(value, 0, 255);
    }

    /**
     * Edit the number of recommandation obtained by the Player (Max : 255).
     *
     * @param value : The value to add or remove.
     */
    public void editRecomHave(int value) {
        _recomHave = MathUtil.limit(_recomHave + value, 0, 255);
    }

    /**
     * @return The number of recommandation that the Player can give.
     */
    public int getRecomLeft() {
        return _recomLeft;
    }

    /**
     * Set the number of givable recommandations by the {@link Player} (Max : 9).
     *
     * @param value : The number of recommendations a player can give.
     */
    public void setRecomLeft(int value) {
        _recomLeft = MathUtil.limit(value, 0, 9);
    }

    /**
     * Increment the number of recommandation that the Player can give.
     */
    private void decRecomLeft() {
        if (_recomLeft > 0) {
            _recomLeft--;
        }
    }

    public List<Integer> getRecomChars() {
        return _recomChars;
    }

    public void giveRecom(Player target) {
        target.editRecomHave(1);
        decRecomLeft();
        _recomChars.add(target.getObjectId());
        PlayerDao.updateRecommends(this, target);
    }

    public boolean canRecom(Player target) {
        return !_recomChars.contains(target.getObjectId());
    }

    /**
     * Return the Karma of the Player.
     */
    @Override
    public int getKarma() {
        return _karma;
    }

    /**
     * Set the Karma of the Player and send StatusUpdate (broadcast).
     *
     * @param karma A value.
     */
    public void setKarma(int karma) {
        if (karma < 0) {
            karma = 0;
        }

        if (_karma > 0 && karma == 0) {
            sendPacket(new UserInfo(this));
            broadcastRelationsChanges();
        }

        // send message with new karma value
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1).addNumber(karma));

        _karma = karma;
        broadcastKarma();
    }

    /**
     * Note: Base weight limit value is 69000.
     */
    @Override
    public int getWeightLimit() {
        double value = getRace().getWeightLimit() * Formulas.CON_BONUS[getStatus().getCON()] * Config.WEIGHT_LIMIT;
        return (int) getStatus().calcStat(Stats.WEIGHT_LIMIT, value, this, null);
    }

    public int getArmorGradePenalty() {
        return _armorGradePenalty;
    }

    public boolean getWeaponGradePenalty() {
        return _weaponGradePenalty;
    }

    public WeightPenalty getWeightPenalty() {
        return _weightPenalty;
    }

    /**
     * Update the overloaded status of the Player.
     */
    public void refreshWeightPenalty() {
        final int weightLimit = getWeightLimit();
        if (weightLimit <= 0) {
            return;
        }

        final double ratio = (getCurrentWeight() - getStatus().calcStat(Stats.WEIGHT_PENALTY, 0, this, null)) / weightLimit;

        final WeightPenalty newWeightPenalty;
        if (ratio < 0.5) {
            newWeightPenalty = WeightPenalty.NONE;
        } else if (ratio < 0.666) {
            newWeightPenalty = WeightPenalty.LEVEL_1;
        } else if (ratio < 0.8) {
            newWeightPenalty = WeightPenalty.LEVEL_2;
        } else if (ratio < 1) {
            newWeightPenalty = WeightPenalty.LEVEL_3;
        } else {
            newWeightPenalty = WeightPenalty.LEVEL_4;
        }

        if (_weightPenalty != newWeightPenalty) {
            _weightPenalty = newWeightPenalty;

            sendPacket(new UserInfo(this));
            sendPacket(new EtcStatusUpdate(this));
            broadcastCharInfo();
        }
    }

    /**
     * Refresh expertise level ; weapon got one rank, when armor got 4 ranks.<br>
     */
    public void refreshExpertisePenalty() {
        final int expertiseLevel = getSkillLevel(L2Skill.SKILL_EXPERTISE);

        int armorPenalty = 0;
        boolean weaponPenalty = false;

        for (final ItemInstance item : getInventory().getPaperdollItems()) {
            if (item.getItemType() != EtcItemType.ARROW && item.getItem().getCrystalType().getId() > expertiseLevel) {
                if (item.isWeapon()) {
                    weaponPenalty = true;
                } else {
                    armorPenalty += (item.getItem().getBodyPart() == Item.SLOT_FULL_ARMOR) ? 2 : 1;
                }
            }
        }

        armorPenalty = Math.min(armorPenalty, 4);

        // Found a different state than previous ; update it.
        if (_weaponGradePenalty != weaponPenalty || _armorGradePenalty != armorPenalty) {
            _weaponGradePenalty = weaponPenalty;
            _armorGradePenalty = armorPenalty;

            // Passive skill "Grade Penalty" is either granted or dropped.
            if (_weaponGradePenalty || _armorGradePenalty > 0) {
                addSkill(SkillTable.getInstance().getInfo(4267, 1), false);
            } else {
                removeSkill(4267, false);
            }

            sendSkillList();
            sendPacket(new EtcStatusUpdate(this));

            // Activate / desactivate weapon effects.
            final ItemInstance item = getActiveWeaponInstance();
            if (item != null) {
                if (_weaponGradePenalty) {
                    ItemPassiveSkillsListener.getInstance().onUnequip(Paperdoll.NULL, item, this);
                } else {
                    ItemPassiveSkillsListener.getInstance().onEquip(Paperdoll.NULL, item, this);
                }
            }
        }
    }

    /**
     * Equip or unequip the item.
     * <UL>
     * <LI>If item is equipped, shots are applied if automation is on.</LI>
     * <LI>If item is unequipped, shots are discharged.</LI>
     * </UL>
     *
     * @param item The item to charge/discharge.
     * @param abortAttack If true, the current attack will be aborted in order to equip the item.
     */
    public void useEquippableItem(ItemInstance item, boolean abortAttack) {
        // trying to equip left hand weapon if twoHandGrip used need fix for dual picking
        if (twoHandGrip.get() && item.getSlot() == Paperdoll.LHAND.getId()) {
            return;
        }

        ItemInstance[] items = null;
        final boolean isEquipped = item.isEquipped();
        final int oldInvLimit = getStatus().getInventoryLimit();

        if (item.getItem() instanceof Weapon) {
            item.unChargeAllShots();
        }

        if (isEquipped) {
            if (item.getEnchantLevel() > 0) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(item));
            } else {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(item));
            }

            items = getInventory().unequipItemInBodySlotAndRecord(item);
        } else {
            items = getInventory().equipItemAndRecord(item);

            if (item.isEquipped()) {
                if (item.getEnchantLevel() > 0) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel()).addItemName(item));
                } else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(item));
                }

                if ((item.getItem().getBodyPart() & Item.SLOT_ALLWEAPON) != 0) {
                    rechargeShots(true, true);
                }
            } else {
                sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
            }
        }
        refreshExpertisePenalty();
        broadcastUserInfo();

        final InventoryUpdate iu = new InventoryUpdate();
        iu.addItems(Arrays.asList(items));
        sendPacket(iu);

        if (abortAttack) {
            getAttack().stop();
        }

        if (getStatus().getInventoryLimit() != oldInvLimit) {
            sendPacket(new ExStorageMaxCount(this));
        }
    }

    /**
     * @return PvP Kills of the Player (number of player killed during a PvP).
     */
    public int getPvpKills() {
        return _pvpKills;
    }

    /**
     * Set PvP Kills of the Player (number of player killed during a PvP).
     *
     * @param pvpKills A value.
     */
    public void setPvpKills(int pvpKills) {
        _pvpKills = pvpKills;
    }

    /**
     * @return The ClassId object of the Player contained in L2PcTemplate.
     */
    public ClassId getClassId() {
        return getTemplate().getClassId();
    }

    /**
     * Set the template of this {@link Player}.
     *
     * @param id : The id of the {@link ClassId} to set.
     */
    public void setClassId(int id) {
        if (!locker.tryLock()) {
            return;
        }

        try {
            if (getLvlJoinedAcademy() != 0 && _clan != null && ClassId.VALUES[id].getLevel() == 2) {
                // Calculate points to add on Clan Reputation score.
                int points;
                if (getLvlJoinedAcademy() <= 16) {
                    points = 400;
                } else if (getLvlJoinedAcademy() >= 39) {
                    points = 170;
                } else {
                    points = 400 - (getLvlJoinedAcademy() - 16) * 10;
                }

                // Add the points.
                _clan.addReputationScore(points);

                // Broadcast to all members, included future ousted member.
                _clan.broadcastToMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_GRADUATED_FROM_ACADEMY).addString(getName()).addNumber(points));

                // Send static messages to the ousted member.
                sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);
                sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_CLAN);

                setLvlJoinedAcademy(0);

                // Refresh existing members, and send last message.
                _clan.broadcastToMembersExcept(this, new PledgeShowMemberListDelete(getName()), SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(getName()));

                // Oust pledge member from the academy.
                _clan.removeClanMember(getObjectId(), 0);

                // Reward the ousted members with graduation gift : academy circlet
                addItem("Gift", 8181, 1, this, true);
            }

            if (isSubClassActive()) {
                _subClasses.get(_classIndex).setClassId(id);
            }

            broadcastPacket(new MagicSkillUse(this, this, 5103, 1, 1000, 0));
            setClassTemplate(id);

            if (getClassId().getLevel() == 3) {
                sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER);
            } else {
                sendPacket(SystemMessageId.CLASS_TRANSFER);
            }

            // Update class icon in party and clan
            if (_party != null) {
                _party.broadcastPacket(new PartySmallWindowUpdate(this));
            }

            if (_clan != null) {
                _clan.broadcastToMembers(new PledgeShowMemberListUpdate(this));
            }

            if (Config.AUTO_LEARN_SKILLS) {
                rewardSkills();
            }
        } finally {
            locker.unlock();
        }
    }

    public void setActiveEnchantItem(ItemInstance scroll) {
        _activeEnchantItem = scroll;
    }

    public ItemInstance getActiveEnchantItem() {
        return _activeEnchantItem;
    }

    /**
     * @return The Race object of the Player.
     */
    public ClassRace getRace() {
        return (isSubClassActive()) ? getBaseTemplate().getRace() : getTemplate().getRace();
    }

    public RadarList getRadarList() {
        return _radarList;
    }

    public CubicList getCubicList() {
        return _cubicList;
    }

    /**
     * @param castleId The castle to check.
     * @return True if this Player is a clan leader in ownership of the passed castle.
     */
    public boolean isCastleLord(int castleId) {
        final Clan clan = getClan();
        if (clan != null && clan.getLeader().getPlayerInstance() == this) {
            final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
            return castle != null && castle.getCastleId() == castleId;
        }
        return false;
    }

    /**
     * @return The Clan Identifier of the Player.
     */
    public int getClanId() {
        return _clanId;
    }

    /**
     * @return The Clan Crest Identifier of the Player or 0.
     */
    public int getClanCrestId() {
        return (_clan != null) ? _clan.getCrestId() : 0;
    }

    /**
     * @return The Clan CrestLarge Identifier or 0
     */
    public int getClanCrestLargeId() {
        return (_clan != null) ? _clan.getCrestLargeId() : 0;
    }

    public long getClanJoinExpiryTime() {
        return _clanJoinExpiryTime;
    }

    public void setClanJoinExpiryTime(long time) {
        _clanJoinExpiryTime = time;
    }

    public long getClanCreateExpiryTime() {
        return _clanCreateExpiryTime;
    }

    public void setClanCreateExpiryTime(long time) {
        _clanCreateExpiryTime = time;
    }

    public void setOnlineTime(long time) {
        onlineTime = time;
        onlineBeginTime = System.currentTimeMillis();
    }

    /**
     * Return the PcInventory Inventory of the Player contained in _inventory.
     */
    @Override
    public PcInventory getInventory() {
        return _inventory;
    }

    @Override
    public boolean isSitting() {
        return _isSitting;
    }

    @Override
    public boolean isStanding() {
        return _isStanding;
    }

    @Override
    public boolean isSittingNow() {
        return _isSittingNow;
    }

    @Override
    public boolean isStandingNow() {
        return _isStandingNow;
    }

    /**
     * Sit down the {@link Player}. The player retrieves control after a 2.5s delay.
     * <ul>
     * <li>Set the AI Intention to IDLE</li>
     * <li>Broadcast {@link ChangeWaitType} packet</li>
     * </ul>
     *
     * @return true if the Player could successfully sit down, false otherwise.
     */
    public boolean sitDown() {
        _isSittingNow = true;
        _isStanding = false;

        ThreadPool.schedule(() ->
        {
            _isSittingNow = false;
            _isSitting = true;

            getAI().notifyEvent(AiEventType.SAT_DOWN, null, null);

        }, 2500);

        // Broadcast the packet.
        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));

        // Tutorial
        final QuestState qs = _questList.getQuestState("Tutorial");
        if (qs != null) {
            qs.getQuest().notifyEvent("CE8388608", null, this);
        }

        return true;
    }

    /**
     * Stand the {@link Player} up. The player retrieves control after a 2.5s delay.
     * <ul>
     * <li>Schedules the STOOD_UP event</li>
     * <li>Broadcast {@link ChangeWaitType} packet</li>
     * </ul>
     */
    public void standUp() {
        _isStandingNow = true;
        _isSitting = false;

        // Schedule a stand up task to wait for the animation to finish
        ThreadPool.schedule(() ->
        {
            _isStandingNow = false;
            _isStanding = true;

            getAI().notifyEvent(AiEventType.STOOD_UP, null, null);

        }, 2500);

        // Broadcast the packet.
        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
    }

    /**
     * @return The PcWarehouse object of the Player.
     */
    public PcWarehouse getWarehouse() {
        if (_warehouse == null) {
            _warehouse = new PcWarehouse(this);
            _warehouse.restore();
        }
        return _warehouse;
    }

    /**
     * Free memory used by Warehouse
     */
    public void clearWarehouse() {
        if (_warehouse != null) {
            _warehouse.deleteMe();
        }

        _warehouse = null;
    }

    /**
     * @return The PcFreight object of the Player.
     */
    public PcFreight getFreight() {
        if (_freight == null) {
            _freight = new PcFreight(this);
            _freight.restore();
        }
        return _freight;
    }

    /**
     * Free memory used by Freight
     */
    public void clearFreight() {
        if (_freight != null) {
            _freight.deleteMe();
        }

        _freight = null;
    }

    /**
     * @param objectId The id of the owner.
     * @return deposited PcFreight object for the objectId or create new if not existing.
     */
    public PcFreight getDepositedFreight(int objectId) {
        for (final PcFreight freight : _depositedFreight) {
            if (freight != null && freight.getOwnerId() == objectId) {
                return freight;
            }
        }

        final PcFreight freight = new PcFreight(null);
        freight.doQuickRestore(objectId);
        _depositedFreight.add(freight);
        return freight;
    }

    /**
     * Clear memory used by deposited freight
     */
    public void clearDepositedFreight() {
        for (final PcFreight freight : _depositedFreight) {
            if (freight != null) {
                freight.deleteMe();
            }
        }
        _depositedFreight.clear();
    }

    /**
     * @return The Adena amount of the Player.
     */
    public int getAdena() {
        return _inventory.getAdena();
    }

    /**
     * @return The Ancient Adena amount of the Player.
     */
    public int getAncientAdena() {
        return _inventory.getAncientAdena();
    }

    /**
     * Add adena to Inventory of the Player and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param count int Quantity of adena to be added
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addAdena(String process, int count, WorldObject reference, boolean sendMessage) {
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
        }

        if (count > 0) {
            _inventory.addAdena(process, count, this, reference);

            final InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(_inventory.getAdenaInstance());
            sendPacket(iu);
        }
    }

    /**
     * Reduce adena in Inventory of the Player and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param count int Quantity of adena to be reduced
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAdena(String process, int count, WorldObject reference, boolean sendMessage) {
        if (count > getAdena()) {
            if (sendMessage) {
                sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            }

            return false;
        }

        if (count > 0) {
            final ItemInstance adenaItem = _inventory.getAdenaInstance();
            if (!_inventory.reduceAdena(process, count, this, reference)) {
                return false;
            }

            // Send update packet
            final InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(adenaItem);
            sendPacket(iu);

            if (sendMessage) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA).addNumber(count));
            }
        }
        return true;
    }

    /**
     * Add ancient adena to Inventory of the Player and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param count int Quantity of ancient adena to be added
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addAncientAdena(String process, int count, WorldObject reference, boolean sendMessage) {
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(PcInventory.ANCIENT_ADENA_ID).addNumber(count));
        }

        if (count > 0) {
            _inventory.addAncientAdena(process, count, this, reference);

            final InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(_inventory.getAncientAdenaInstance());
            sendPacket(iu);
        }
    }

    /**
     * Reduce ancient adena in Inventory of the Player and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param count int Quantity of ancient adena to be reduced
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAncientAdena(String process, int count, WorldObject reference, boolean sendMessage) {
        if (count > getAncientAdena()) {
            if (sendMessage) {
                sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
            }

            return false;
        }

        if (count > 0) {
            final ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
            if (!_inventory.reduceAncientAdena(process, count, this, reference)) {
                return false;
            }

            final InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(ancientAdenaItem);
            sendPacket(iu);

            if (sendMessage) {
                if (count > 1) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID).addItemNumber(count));
                } else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID));
                }
            }
        }
        return true;
    }

    /**
     * Adds item to inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param item ItemInstance to be added
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage) {
        if (item.getCount() > 0) {
            // Sends message to client if requested
            if (sendMessage) {
                if (item.getCount() > 1) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(item).addNumber(item.getCount()));
                } else if (item.getEnchantLevel() > 0) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(item.getEnchantLevel()).addItemName(item));
                } else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item));
                }
            }

            // Add the item to inventory
            final ItemInstance newitem = _inventory.addItem(process, item, this, reference);

            // Send inventory update packet
            final InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(newitem);
            sendPacket(playerIU);

            // Update current load as well
            final StatusUpdate su = new StatusUpdate(this);
            su.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
            sendPacket(su);

            // Cursed Weapon
            if (CursedWeaponManager.getInstance().isCursed(newitem.getItemId())) {
                CursedWeaponManager.getInstance().activate(this, newitem);
            }
            // If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
            else if (item.getItem().getItemType() == EtcItemType.ARROW && getAttackType() == WeaponType.BOW && !getInventory().hasItemIn(Paperdoll.LHAND)) {
                checkAndEquipArrows();
            }
        }
    }

    /**
     * Adds item to Inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param itemId int Item Identifier of the item to be added
     * @param count int Quantity of items to be added
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return The created ItemInstance.
     */
    public ItemInstance addItem(String process, int itemId, int count, WorldObject reference, boolean sendMessage) {
        if (count > 0) {
            // Retrieve the template of the item.
            final Item item = ItemManager.getInstance().getTemplate(itemId);
            if (item == null) {
                return null;
            }

            // Sends message to client if requested. Since Herbs are directly assimilated, they don't send any "You have earned X" message, only "The effects of X flow through you".
            if (count > 1) {
                if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest")) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(count));
                } else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(itemId).addItemNumber(count));
                }
            } else {
                if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest")) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
                } else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId));
                }
            }

            // Sends message to client if requested. Since Herbs are directly assimilated, they don't send any "You have earned X" message, only "The effects of X flow through you".
            if (sendMessage && item.getItemType() != EtcItemType.HERB) {
                if (count > 1) {
                    if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest")) {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(count));
                    } else {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(itemId).addItemNumber(count));
                    }
                } else {
                    if (process.equalsIgnoreCase("Sweep") || process.equalsIgnoreCase("Quest")) {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
                    } else {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId));
                    }
                }
            }

            // If the item is herb type, dont add it to inventory.
            if (item.getItemType() == EtcItemType.HERB) {
                final ItemInstance herb = new ItemInstance(0, itemId);

                final IItemHandler handler = ItemHandler.getInstance().getHandler(herb.getEtcItem());
                if (handler != null) {
                    handler.useItem(this, herb, false);
                }
            } else {
                // Add the item to inventory
                final ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);

                // Cursed Weapon
                if (CursedWeaponManager.getInstance().isCursed(createdItem.getItemId())) {
                    CursedWeaponManager.getInstance().activate(this, createdItem);
                }
                // If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
                else if (item.getItemType() == EtcItemType.ARROW && getAttackType() == WeaponType.BOW && !getInventory().hasItemIn(Paperdoll.LHAND)) {
                    checkAndEquipArrows();
                }

                return createdItem;
            }
        }
        return null;
    }

    /**
     * Destroy item from inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param item ItemInstance to be destroyed
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage) {
        return destroyItem(process, item, item.getCount(), reference, sendMessage);
    }

    /**
     * Destroy item from inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param item ItemInstance to be destroyed
     * @param count int Quantity of ancient adena to be reduced
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(String process, ItemInstance item, int count, WorldObject reference, boolean sendMessage) {
        item = _inventory.destroyItem(process, item, count, this, reference);
        if (item == null) {
            if (sendMessage) {
                sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send inventory update packet
        final InventoryUpdate iu = new InventoryUpdate();
        if (item.getCount() == 0) {
            iu.addRemovedItem(item);
        } else {
            iu.addModifiedItem(item);
        }
        sendPacket(iu);

        // Update current load as well
        final StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            if (count > 1) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item).addItemNumber(count));
            } else {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item));
            }
        }
        return true;
    }

    /**
     * Destroys item from inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param objectId int Item Instance identifier of the item to be destroyed
     * @param count int Quantity of items to be destroyed
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(String process, int objectId, int count, WorldObject reference, boolean sendMessage) {
        final ItemInstance item = _inventory.getItemByObjectId(objectId);
        if (item == null) {
            if (sendMessage) {
                sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        return destroyItem(process, item, count, reference, sendMessage);
    }

    /**
     * Destroys shots from inventory without logging and only occasional saving to database. Sends InventoryUpdate
     * packet to the Player.
     *
     * @param objectId int Item Instance identifier of the item to be destroyed
     * @param count int Quantity of items to be destroyed
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItemWithoutTrace(int objectId, int count) {
        final ItemInstance item = _inventory.getItemByObjectId(objectId);
        if (item == null || item.getCount() < count) {
            return false;
        }

        return destroyItem(null, item, count, null, false);
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param itemId int Item identifier of the item to be destroyed
     * @param count int Quantity of items to be destroyed
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(String process, int itemId, int count, WorldObject reference, boolean sendMessage) {
        if (itemId == 57) {
            return reduceAdena(process, count, reference, sendMessage);
        }

        final ItemInstance item = _inventory.getItemByItemId(itemId);

        if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null) {
            if (sendMessage) {
                sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        // Send inventory update packet
        final InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(item);
        sendPacket(playerIU);

        // Update current load as well
        final StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            if (count > 1) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(itemId).addItemNumber(count));
            } else {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));
            }
        }
        return true;
    }

    /**
     * Transfers item to another ItemContainer and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param objectId int Item Identifier of the item to be transfered
     * @param count int Quantity of items to be transfered
     * @param target Inventory the Inventory target.
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @return ItemInstance corresponding to the new item or the updated item in inventory
     */
    public ItemInstance transferItem(String process, int objectId, int count, Inventory target, WorldObject reference) {
        final ItemInstance oldItem = checkItemManipulation(objectId, count);
        if (oldItem == null) {
            return null;
        }

        final ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
        if (newItem == null) {
            return null;
        }

        // Send inventory update packet
        final InventoryUpdate playerIU = new InventoryUpdate();

        if (oldItem.getCount() > 0 && oldItem != newItem) {
            playerIU.addModifiedItem(oldItem);
        } else {
            playerIU.addRemovedItem(oldItem);
        }

        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate playerSU = new StatusUpdate(this);
        playerSU.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
        sendPacket(playerSU);

        // Send target update packet
        if (target instanceof PcInventory) {
            final Player targetPlayer = ((PcInventory) target).getOwner();

            final InventoryUpdate playerIU2 = new InventoryUpdate();
            if (newItem.getCount() > count) {
                playerIU2.addModifiedItem(newItem);
            } else {
                playerIU2.addNewItem(newItem);
            }
            targetPlayer.sendPacket(playerIU2);

            // Update current load as well
            playerSU = new StatusUpdate(targetPlayer);
            playerSU.addAttribute(StatusType.CUR_LOAD, targetPlayer.getCurrentWeight());
            targetPlayer.sendPacket(playerSU);
        } else if (target instanceof PetInventory) {
            final PetInventoryUpdate petIU = new PetInventoryUpdate();
            if (newItem.getCount() > count) {
                petIU.addModifiedItem(newItem);
            } else {
                petIU.addNewItem(newItem);
            }
            ((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
        }
        return newItem;
    }

    /**
     * Drop item from inventory and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param item ItemInstance to be dropped
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean dropItem(String process, ItemInstance item, WorldObject reference, boolean sendMessage) {
        item = _inventory.dropItem(process, item, this, reference);

        if (item == null) {
            if (sendMessage) {
                sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }

            return false;
        }

        item.dropMe(this, 25);

        // Send inventory update packet
        final InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(item);
        sendPacket(playerIU);

        // Update current load as well
        final StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));
        }

        return true;
    }

    /**
     * Drop item from inventory by using its <B>objectID</B> and send InventoryUpdate packet to the Player.
     *
     * @param process String Identifier of process triggering this action
     * @param objectId int Item Instance identifier of the item to be dropped
     * @param count int Quantity of items to be dropped
     * @param x int coordinate for drop X
     * @param y int coordinate for drop Y
     * @param z int coordinate for drop Z
     * @param reference WorldObject Object referencing current action like NPC selling item or previous item in
     * transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return ItemInstance corresponding to the new item or the updated item in inventory
     */
    public ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, WorldObject reference, boolean sendMessage) {
        final ItemInstance invItem = _inventory.getItemByObjectId(objectId);
        final ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

        if (item == null) {
            if (sendMessage) {
                sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
            }

            return null;
        }

        item.dropMe(this, x, y, z);

        // Send inventory update packet
        final InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(invItem);
        sendPacket(playerIU);

        // Update current load as well
        final StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusType.CUR_LOAD, getCurrentWeight());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));
        }

        return item;
    }

    public ItemInstance checkItemManipulation(int objectId, int count) {
        if (World.getInstance().getObject(objectId) == null) {
            return null;
        }

        final ItemInstance item = getInventory().getItemByObjectId(objectId);

        if (item == null || item.getOwnerId() != getObjectId()) {
            return null;
        }

        if (count < 1 || (count > 1 && !item.isStackable())) {
            return null;
        }

        if (count > item.getCount()) {
            return null;
        }

        // Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
        if (_summon != null && _summon.getControlItemObjectId() == objectId || _mountObjectId == objectId) {
            return null;
        }

        if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId) {
            return null;
        }

        // We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
        if (item.isAugmented() && getCast().isCastingNow()) {
            return null;
        }

        return item;
    }

    /**
     * Launch a task corresponding to Config time.
     *
     * @param protect boolean Drop timer or activate it.
     */
    public void setSpawnProtection(boolean protect) {
        if (protect) {
            if (_protectTask == null) {
                _protectTask = ThreadPool.schedule(() ->
                {
                    setSpawnProtection(false);
                    sendMessage("The spawn protection has ended.");
                }, Config.PLAYER_SPAWN_PROTECTION * 1000L);
            }
        } else {
            _protectTask.cancel(true);
            _protectTask = null;
        }
        broadcastUserInfo();
    }

    public boolean isSpawnProtected() {
        return _protectTask != null;
    }

    /**
     * Set protection from agro mobs when getting up from fake death, according settings.
     */
    public void setRecentFakeDeath() {
        _recentFakeDeathEndTime = System.currentTimeMillis() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * 1000L;
    }

    public void clearRecentFakeDeath() {
        _recentFakeDeathEndTime = 0;
    }

    public boolean isRecentFakeDeath() {
        return _recentFakeDeathEndTime > System.currentTimeMillis();
    }

    @Override
    public boolean isFakeDeath() {
        return _isFakeDeath;
    }

    @Override
    public boolean isAlikeDead() {
        if (super.isAlikeDead()) {
            return true;
        }

        return isFakeDeath();
    }

    /**
     * @return The client owner of this char.
     */
    public GameClient getClient() {
        return _client;
    }

    public void setClient(GameClient client) {
        _client = client;
    }

    public String getAccountName() {
        return _accountName;
    }

    public Map<Integer, String> getAccountChars() {
        return _chars;
    }

    /**
     * Close the active connection with the {@link GameClient} linked to this {@link Player}.
     *
     * @param closeClient : If true, the client is entirely closed. Otherwise, the client is sent back to login.
     */
    public void logout(boolean closeClient) {
        final GameClient client = _client;
        if (client == null) {
            return;
        }

        if (client.isDetached()) {
            client.cleanMe(true);
        } else if (!client.getConnection().isClosed()) {
            client.close((closeClient) ? LeaveWorld.STATIC_PACKET : ServerClose.STATIC_PACKET);
        }
    }

    @Override
    public void enableSkill(L2Skill skill) {
        super.enableSkill(skill);
        _reuseTimeStamps.remove(skill.getReuseHashCode());
    }

    @Override
    public void onAction(Player player, boolean isCtrlPressed, boolean isShiftPressed) {
        if (player.getTarget() != this) {
            player.setTarget(this);
        } else {
            if (player.isGM() && isShiftPressed) {
                player.sendMessage("Distance to: " + MathUtil.calculateDistance(player, this, true));
                player.sendPacket(ActionFailed.STATIC_PACKET);
            } else if (isAttackableWithoutForceBy(player) || (isCtrlPressed && isAttackableBy(player))) {
                player.getAI().tryToAttack(this, isCtrlPressed, isShiftPressed);
            } else if (isOperating()) {
                player.getAI().tryToInteract(this, isCtrlPressed, isShiftPressed);
            } else {
                player.getAI().tryToFollow(this, isShiftPressed);
            }
        }
    }

    @Override
    public void broadcastPacket(L2GameServerPacket packet, boolean selfToo) {
        if (selfToo) {
            sendPacket(packet);
        }

        super.broadcastPacket(packet, selfToo);
    }

    @Override
    public void broadcastPacketInRadius(L2GameServerPacket packet, int radius) {
        sendPacket(packet);

        super.broadcastPacketInRadius(packet, radius);
    }

    /**
     * Broadcast informations from a user to himself and his knownlist.<BR> If player is morphed, it sends informations
     * from the template the player is using.
     * <ul>
     * <li>Send a UserInfo packet (public and private data) to this Player.</li>
     * <li>Send a CharInfo packet (public data only) to Player's knownlist.</li>
     * </ul>
     */
    public void broadcastUserInfo() {
        sendPacket(new UserInfo(this));

        if (getPolymorphTemplate() != null) {
            broadcastPacket(new AbstractNpcInfo.PcMorphInfo(this, getPolymorphTemplate()), false);
        } else {
            broadcastCharInfo();
        }
    }

    public void broadcastCharInfo() {
        for (final Player player : getKnownType(Player.class)) {
            player.sendPacket(new CharInfo(this));

            final int relation = getRelation(player);
            // TODO check this!!!
            final boolean isAutoAttackable = isAttackableWithoutForceBy(player);

            player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
            if (_summon != null) {
                player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
            }
        }
    }

    /**
     * Broadcast player title information.
     */
    public void broadcastTitleInfo() {
        sendPacket(new UserInfo(this));
        broadcastPacket(new TitleUpdate(this));
    }

    /**
     * @return the Alliance Identifier of the Player.
     */
    public int getAllyId() {
        if (_clan == null) {
            return 0;
        }

        return _clan.getAllyId();
    }

    public int getAllyCrestId() {
        if (getClanId() == 0) {
            return 0;
        }

        if (getClan().getAllyId() == 0) {
            return 0;
        }

        return getClan().getAllyCrestId();
    }

    @Override
    public void sendPacket(L2GameServerPacket packet) {
        if (_client != null) {
            _client.sendPacket(packet);
        }
    }

    @Override
    public void sendPacket(SystemMessageId id) {
        sendPacket(SystemMessage.getSystemMessage(id));
    }

    /**
     * Check if this {@link Player} can open any type of private shop (buy, sell, manufacture). Show him the error
     * message, if any.
     *
     * @param cancelActiveTrade : if true, active trade will also be canceled.
     * @return true if all conditions are met, false otherwise.
     */
    public boolean canOpenPrivateStore(boolean cancelActiveTrade) {
        // If under shop mode, don't check any conditions except death.
        if (isInStoreMode()) {
            return !isAlikeDead();
        }

        // Cancel active trade, if parameter is on.
        if (cancelActiveTrade && getActiveTradeList() != null) {
            cancelActiveTrade();
        }

        // Under fight conditions.
        if (isInDuel() || AttackStanceTaskManager.getInstance().isInAttackStance(this) || getPvpFlag() > 0 || getAttack().isAttackingNow()) {
            setOperateType(OperateType.NONE);
            sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
            return false;
        }

        // Under cast.
        if (getCast().isCastingNow()) {
            setOperateType(OperateType.NONE);
            sendPacket(SystemMessageId.PRIVATE_STORE_NOT_WHILE_CASTING);
            return false;
        }

        // No store zones.
        if (isInsideZone(ZoneId.NO_STORE) || isInOlympiadMode()) {
            setOperateType(OperateType.NONE);
            sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
            return false;
        }

        // Misc conditions.
        if (isAlikeDead() || isMounted() || isProcessingRequest() || isOutOfControl()) {
            setOperateType(OperateType.NONE);
            return false;
        }

        // Can't open a private store if already sat (isInStoreMode() is checked in order toggle off works).
        if ((isSitting() && !isInStoreMode()) || isStandingNow()) {
            setOperateType(OperateType.NONE);
            return false;
        }

        return true;
    }

    public void tryOpenPrivateBuyStore() {
        // Check multiple conditions. Message is sent directly from the method.
        if (!canOpenPrivateStore(true)) {
            return;
        }

        if (getOperateType() == OperateType.NONE || getOperateType() == OperateType.BUY) {
            if (isSittingNow()) {
                return;
            }

            // Stand up only if operate type is BUY.
            if (getOperateType() == OperateType.BUY) {
                standUp();
            }

            setOperateType(OperateType.BUY_MANAGE);
            sendPacket(new PrivateStoreManageListBuy(this));
        }
    }

    public void tryOpenPrivateSellStore(boolean isPackageSale) {
        // Check multiple conditions. Message is sent directly from the method.
        if (!canOpenPrivateStore(true)) {
            return;
        }

        if (getOperateType() == OperateType.NONE || getOperateType() == OperateType.SELL || getOperateType() == OperateType.PACKAGE_SELL) {
            if (isSittingNow()) {
                return;
            }

            // Stand up only if operate type is SELL or PACKAGE_SELL.
            if (getOperateType() == OperateType.SELL || getOperateType() == OperateType.PACKAGE_SELL) {
                standUp();
            }

            setOperateType(OperateType.SELL_MANAGE);
            sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
        }
    }

    public void tryOpenWorkshop(boolean isDwarven) {
        // Check multiple conditions. Message is sent directly from the method.
        if (!canOpenPrivateStore(true)) {
            return;
        }

        if (getOperateType() == OperateType.NONE || getOperateType() == OperateType.MANUFACTURE) {
            if (isSittingNow()) {
                return;
            }

            // Stand up only if operate type is MANUFACTURE.
            if (getOperateType() == OperateType.MANUFACTURE) {
                standUp();
            }

            setOperateType(OperateType.MANUFACTURE_MANAGE);
            sendPacket(new RecipeShopManageList(this, isDwarven));
        }
    }

    public PreparedListContainer getMultiSell() {
        return _currentMultiSell;
    }

    public void setMultiSell(PreparedListContainer list) {
        _currentMultiSell = list;
    }

    @Override
    public void setTarget(WorldObject newTarget) {
        // Unset new target following specific conditions.
        if (newTarget != null) {
            // Check if the new target is visible.
            if (!newTarget.isVisible() && !(newTarget instanceof Player && isInParty() && _party.containsPlayer(newTarget))) {
                newTarget = null;
            }

            // Can't target and attack festival monsters if not participant.
            if (newTarget instanceof FestivalMonster && !isFestivalParticipant()) {
                newTarget = null;
            }

            // Can't target and attack rift invaders if not in the same room.
            if (isInParty() && getParty().isInDimensionalRift() && !getParty().getDimensionalRift().isInCurrentRoomZone(newTarget)) {
                newTarget = null;
            }
        }

        // Get the current target
        final WorldObject oldTarget = getTarget();

        if (oldTarget != null) {
            // No target change, return.
            if (oldTarget == newTarget) {
                return;
            }

            // Remove the Player from the _statusListener of the old target if it was a Creature.
            if (oldTarget instanceof Creature) {
                ((Creature) oldTarget).getStatus().removeStatusListener(this);
            }
        }

        // Verify if it's a static object.
        if (newTarget instanceof StaticObject) {
            sendPacket(new MyTargetSelected(newTarget.getObjectId(), 0));
            sendPacket(new StaticObjectInfo((StaticObject) newTarget));
        }
        // Add the Player to the _statusListener of the new target if it's a Creature
        else if (newTarget instanceof Creature target) {

            // Validate location of the new target.
            if (newTarget.getObjectId() != getObjectId()) {
                sendPacket(new ValidateLocation(target));
            }

            // Show the client his new target.
            sendPacket(new MyTargetSelected(target.getObjectId(), (target.isAttackableBy(this) || target instanceof Summon) ? getStatus().getLevel() - target.getStatus().getLevel() : 0));

            target.getStatus().addStatusListener(this);

            // Send max/current hp.
            final StatusUpdate su = new StatusUpdate(target);
            su.addAttribute(StatusType.MAX_HP, target.getStatus().getMaxHp());
            su.addAttribute(StatusType.CUR_HP, (int) target.getStatus().getHp());
            sendPacket(su);

            broadcastPacket(new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()), false);
        }

        if (newTarget instanceof Folk) {
            setCurrentFolk((Folk) newTarget);
        } else if (newTarget == null) {
            sendPacket(ActionFailed.STATIC_PACKET);

            if (getTarget() != null) {
                broadcastPacket(new TargetUnselected(this));
                setCurrentFolk(null);
            }
        }

        // Target the new WorldObject
        super.setTarget(newTarget);
    }

    @Override
    public ItemInstance getActiveWeaponInstance() {
        return getInventory().getItemFrom(Paperdoll.RHAND);
    }

    @Override
    public Weapon getActiveWeaponItem() {
        final ItemInstance item = getActiveWeaponInstance();
        return (item == null) ? getTemplate().getFists() : (Weapon) item.getItem();
    }

    @Override
    public ItemInstance getSecondaryWeaponInstance() {
        return getInventory().getItemFrom(Paperdoll.LHAND);
    }

    @Override
    public Item getSecondaryWeaponItem() {
        final ItemInstance item = getSecondaryWeaponInstance();
        return (item == null) ? null : item.getItem();
    }

    public int getCoupleId() {
        return _coupleId;
    }

    public void setCoupleId(int coupleId) {
        _coupleId = coupleId;
    }

    /**
     * Kill the Creature, Apply Death Penalty, Manage gain/loss Karma and Item Drop.
     * <ul>
     * <li>Reduce the Experience of the Player in function of the calculated Death Penalty</li>
     * <li>If necessary, unsummon the Pet of the killed Player</li>
     * <li>Manage Karma gain for attacker and Karam loss for the killed Player</li>
     * <li>If the killed Player has Karma, manage Drop Item</li>
     * <li>Kill the Player</li>
     * </ul>
     *
     * @param killer The Creature who attacks
     */
    @Override
    public boolean doDie(Creature killer) {
        // Kill the Player
        if (!super.doDie(killer)) {
            return false;
        }

        if (isMounted()) {
            stopFeed();
        }

        // Clean player charges on death.
        clearCharges();

        synchronized (this) {
            if (isFakeDeath()) {
                stopFakeDeath(true);
            }
        }

        if (killer != null) {
            final Player pk = killer.getActingPlayer();

            if (isCursedWeaponEquipped()) {
                CursedWeaponManager.getInstance().drop(_cursedWeaponEquippedId, killer);
            } else {
                if (pk == null || !pk.isCursedWeaponEquipped()) {
                    onDieDropItem(killer); // Check if any item should be dropped

                    // if the area isn't an arena
                    if (!isInArena()) {
                        // if both victim and attacker got clans & aren't academicians
                        if (pk != null && pk.getClan() != null && getClan() != null && !isAcademyMember() && !pk.isAcademyMember()) {
                            // if clans got mutual war, then use the reputation calcul
                            if (_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId())) {
                                // when your reputation score is 0 or below, the other clan cannot acquire any reputation points
                                if (getClan().getReputationScore() > 0) {
                                    pk.getClan().addReputationScore(1);
                                }
                                // when the opposing sides reputation score is 0 or below, your clans reputation score doesn't decrease
                                if (pk.getClan().getReputationScore() > 0) {
                                    _clan.takeReputationScore(1);
                                }
                            }
                        }
                    }
                }

                // Reduce player's xp and karma.
                if (!hasSkill(L2Skill.SKILL_LUCKY) || getStatus().getLevel() > 9) {
                    applyDeathPenalty(pk != null && getClan() != null && pk.getClan() != null && (getClan().isAtWarWith(pk.getClanId()) || pk.getClan().isAtWarWith(getClanId())), pk != null);
                }
            }
        }

        // Unsummon Cubics
        _cubicList.stopCubics(false);

        if (getFusionSkill() != null) {
            getCast().stop();
        }

        for (final Creature creature : getKnownType(Creature.class)) {
            if (creature.getFusionSkill() != null && creature.getFusionSkill().getTarget() == this) {
                creature.getCast().stop();
            }
        }

        // calculate death penalty buff
        calculateDeathPenaltyBuffLevel(killer);

        if (isPhoenixBlessed()) {
            reviveRequest(this, null, false);
        }

        // Icons update in order to get retained buffs list
        updateEffectIcons();

        return true;
    }

    private void onDieDropItem(Creature killer) {
        if (killer == null) {
            return;
        }

        final Player pk = killer.getActingPlayer();
        if (getKarma() <= 0 && pk != null && pk.getClan() != null && getClan() != null && pk.getClan().isAtWarWith(getClanId())) {
            return;
        }

        if ((!isInsideZone(ZoneId.PVP) || pk == null) && (!isGM() || Config.KARMA_DROP_GM)) {
            final boolean isKillerNpc = (killer instanceof Npc);
            final int pkLimit = Config.KARMA_PK_LIMIT;

            int dropEquip = 0;
            int dropEquipWeapon = 0;
            int dropItem = 0;
            int dropLimit = 0;
            int dropPercent = 0;

            if (getKarma() > 0 && getPkKills() >= pkLimit) {
                dropPercent = Config.KARMA_RATE_DROP;
                dropEquip = Config.KARMA_RATE_DROP_EQUIP;
                dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
                dropItem = Config.KARMA_RATE_DROP_ITEM;
                dropLimit = Config.KARMA_DROP_LIMIT;
            } else if (isKillerNpc && getStatus().getLevel() > 4 && !isFestivalParticipant()) {
                dropPercent = Config.PLAYER_RATE_DROP;
                dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
                dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
                dropItem = Config.PLAYER_RATE_DROP_ITEM;
                dropLimit = Config.PLAYER_DROP_LIMIT;
            }

            if (dropPercent > 0 && Rnd.get(100) < dropPercent) {
                int dropCount = 0;
                int itemDropPercent = 0;

                for (final ItemInstance itemDrop : getInventory().getItems()) {
                    // Don't drop those following things
                    if (!itemDrop.isDropable() || itemDrop.getItemId() == 57 || itemDrop.getItem().getType2() == Item.TYPE2_QUEST || (_summon != null && _summon.getControlItemObjectId() == itemDrop.getItemId()) || ArraysUtil.contains(Config.KARMA_NONDROPPABLE_ITEMS, itemDrop.getItemId()) || ArraysUtil.contains(Config.KARMA_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId())) {
                        continue;
                    }

                    if (itemDrop.isEquipped()) {
                        // Set proper chance according to Item type of equipped Item
                        itemDropPercent = itemDrop.getItem().getType2() == Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
                        getInventory().unequipItemInSlot(itemDrop.getSlot());
                    } else {
                        itemDropPercent = dropItem; // Item in inventory
                    }

                    // NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
                    if (Rnd.get(100) < itemDropPercent) {
                        dropItem("DieDrop", itemDrop, killer, true);

                        if (++dropCount >= dropLimit) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateKarmaLoss(long exp) {
        if (!isCursedWeaponEquipped() && getKarma() > 0) {
            final int karmaLost = Formulas.calculateKarmaLost(getStatus().getLevel(), exp);
            if (karmaLost > 0) {
                setKarma(getKarma() - karmaLost);
            }
        }
    }

    /**
     * This method is used to update PvP counter, or PK counter / add Karma if necessary.<br> It also updates clan
     * kills/deaths counters on siege.
     *
     * @param target The L2Playable victim.
     */
    public void onKillUpdatePvPKarma(Playable target) {
        if (target == null) {
            return;
        }

        final Player targetPlayer = target.getActingPlayer();
        if (targetPlayer == null || targetPlayer == this) {
            return;
        }

        // Don't rank up the CW if it was a summon.
        if (isCursedWeaponEquipped() && target instanceof Player) {
            CursedWeaponManager.getInstance().increaseKills(_cursedWeaponEquippedId);
            return;
        }

        // If in duel and you kill (only can kill l2summon), do nothing
        if (isInDuel() && targetPlayer.isInDuel()) {
            return;
        }

        // If in pvp zone, do nothing.
        if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)) {
            // Until the zone was a siege zone. Check also if victim was a player. Randomers aren't counted.
            if (target instanceof Player && getSiegeState() > 0 && targetPlayer.getSiegeState() > 0 && getSiegeState() != targetPlayer.getSiegeState()) {
                // Now check clan relations.
                final Clan killerClan = getClan();
                if (killerClan != null) {
                    killerClan.setSiegeKills(killerClan.getSiegeKills() + 1);
                }

                final Clan targetClan = targetPlayer.getClan();
                if (targetClan != null) {
                    targetClan.setSiegeDeaths(targetClan.getSiegeDeaths() + 1);
                }
            }
            return;
        }

        // Check if it's pvp (cases : regular, wars, victim is PKer)
        if (checkIfPvP(target) || (targetPlayer.getClan() != null && getClan() != null && getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(getClanId()) && targetPlayer.getPledgeType() != Clan.SUBUNIT_ACADEMY && getPledgeType() != Clan.SUBUNIT_ACADEMY) || (targetPlayer.getKarma() > 0 && Config.KARMA_AWARD_PK_KILL)) {
            if (target instanceof Player) {
                // Add PvP point to attacker.
                setPvpKills(getPvpKills() + 1);

                // Send UserInfo packet to attacker with its Karma and PK Counter
                sendPacket(new UserInfo(this));
            }
        }
        // Otherwise, killer is considered as a PKer.
        else if (targetPlayer.getKarma() == 0 && targetPlayer.getPvpFlag() == 0) {
            // PK Points are increased only if you kill a player.
            if (target instanceof Player) {
                setPkKills(getPkKills() + 1);
            }

            // Calculate new karma.
            setKarma(getKarma() + Formulas.calculateKarmaGain(getPkKills(), target instanceof Summon));

            // Unequip adventurer items.
            checkItemRestriction();

            // Stop Pvp task if running.
            PvpFlagTaskManager.getInstance().remove(this, true);
        }
    }

    public void updatePvPStatus() {
        if (isInsideZone(ZoneId.PVP)) {
            return;
        }

        PvpFlagTaskManager.getInstance().add(this, Config.PVP_NORMAL_TIME);

        if (getPvpFlag() == 0) {
            updatePvPFlag(1);
        }
    }

    public void updatePvPStatus(Creature target) {
        final Player player = target.getActingPlayer();
        if (player == null) {
            return;
        }

        if (isInDuel() && player.getDuelId() == getDuelId()) {
            return;
        }

        if ((!isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP)) && player.getKarma() == 0) {
            PvpFlagTaskManager.getInstance().add(this, checkIfPvP(player) ? Config.PVP_PVP_TIME : Config.PVP_NORMAL_TIME);

            if (getPvpFlag() == 0) {
                updatePvPFlag(1);
            }
        }
    }

    /**
     * Calculate the xp loss and the karma decrease.
     *
     * @param atWar : If true, use clan war penalty system instead of regular system.
     * @param killedByPlayable : If true, we use specific rules if killed by playable only.
     */
    public void applyDeathPenalty(boolean atWar, boolean killedByPlayable) {
        if (isInsideZone(ZoneId.PVP)) {
            // No xp loss inside siege zone if a Charm of Courage is active.
            if (isInsideZone(ZoneId.SIEGE)) {
                if (isAffected(EffectFlag.CHARM_OF_COURAGE)) {
                    stopEffects(EffectType.CHARM_OF_COURAGE);
                    return;
                }
            }
            // No xp loss for arenas participants killed by Playable.
            else if (killedByPlayable) {
                return;
            }
        }

        // Get the level of the Player.
        final int lvl = getStatus().getLevel();

        // The death steal you some xp.
        double percentLost = PlayerLevelData.getInstance().getPlayerLevel(lvl).getExpLossAtDeath();

        if (getKarma() > 0) {
            percentLost *= Config.RATE_KARMA_EXP_LOST;
        }

        if (isFestivalParticipant() || atWar || isInsideZone(ZoneId.SIEGE)) {
            percentLost /= 4.0;
        }

        // Calculate the xp loss.
        int maxLevel = PlayerLevelData.getInstance().getMaxLevel();
        long lostExp;
        if (lvl < maxLevel) {
            lostExp = Math.round((getStatus().getExpForLevel(lvl + 1) - getStatus().getExpForLevel(lvl)) * percentLost / 100);
        } else {
            lostExp = Math.round((getStatus().getExpForLevel(maxLevel) - getStatus().getExpForLevel(maxLevel - 1)) * percentLost / 100);
        }

        // Set new karma.
        updateKarmaLoss(lostExp);
    }

    public int getPartyRoom() {
        return _partyRoom;
    }

    public boolean isInPartyMatchRoom() {
        return _partyRoom > 0;
    }

    public void setPartyRoom(int id) {
        _partyRoom = id;
    }

    /**
     * Remove the {@link Player} from both waiting list and any potential {@link PartyMatchRoom}.
     */
    public void removeMeFromPartyMatch() {
        // Remove waiting Player.
        PartyMatchRoomManager.getInstance().removeWaitingPlayer(this);

        // Remove from existing PartyMatchRoom, if any.
        if (_partyRoom > 0) {
            final PartyMatchRoom room = PartyMatchRoomManager.getInstance().getRoom(_partyRoom);
            if (room != null) {
                room.removeMember(this);
            }
        }
    }

    @Override
    public Summon getSummon() {
        return _summon;
    }

    /**
     * Set the {@link Summon} of this {@link Player}.
     *
     * @param summon : The Summon to set.
     */
    public void setSummon(Summon summon) {
        _summon = summon;
    }

    /**
     * @return true if this {@link Player} has a {@link Pet}, false otherwise.
     */
    public boolean hasPet() {
        return _summon instanceof Pet;
    }

    /**
     * @return true if this {@link Player} has a {@link Servitor}, false otherwise.
     */
    public boolean hasServitor() {
        return _summon instanceof Servitor;
    }

    /**
     * @return the {@link TamedBeast} of this {@link Player}, null otherwise.
     */
    public TamedBeast getTamedBeast() {
        return _tamedBeast;
    }

    /**
     * Set the {@link TamedBeast} of this {@link Player}.
     *
     * @param tamedBeast : The TamedBeast to set.
     */
    public void setTamedBeast(TamedBeast tamedBeast) {
        _tamedBeast = tamedBeast;
    }

    /**
     * @return the current {@link Request}.
     */
    public Request getRequest() {
        return _request;
    }

    /**
     * Set the Player requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
     *
     * @param requester
     */
    public void setActiveRequester(Player requester) {
        _activeRequester = requester;
    }

    /**
     * @return the Player requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
     */
    public Player getActiveRequester() {
        if (_activeRequester != null && _activeRequester.isRequestExpired() && _activeTradeList == null) {
            _activeRequester = null;
        }

        return _activeRequester;
    }

    /**
     * @return True if a request is in progress, or false otherwise.
     */
    public boolean isProcessingRequest() {
        return getActiveRequester() != null || _requestExpireTime > System.currentTimeMillis();
    }

    /**
     * @return True if a transaction <B>(trade OR request)</B> is in progress.
     */
    public boolean isProcessingTransaction() {
        return getActiveRequester() != null || _activeTradeList != null || _requestExpireTime > System.currentTimeMillis();
    }

    /**
     * Set the request expire time of that {@link Player}, and set his {@link Player} partner as the active requester.
     *
     * @param partner : The {@link Player} partner to test.
     */
    public void onTransactionRequest(Player partner) {
        _requestExpireTime = System.currentTimeMillis() + REQUEST_TIMEOUT * 1000;
        partner.setActiveRequester(this);
    }

    /**
     * @return True if last request is expired, or false otherwise.
     */
    public boolean isRequestExpired() {
        return _requestExpireTime <= System.currentTimeMillis();
    }

    public void onTransactionResponse() {
        _requestExpireTime = 0;
    }

    public ItemContainer getActiveWarehouse() {
        return _activeWarehouse;
    }

    public void setActiveWarehouse(ItemContainer warehouse) {
        _activeWarehouse = warehouse;
    }

    public TradeList getActiveTradeList() {
        return _activeTradeList;
    }

    public void setActiveTradeList(TradeList tradeList) {
        _activeTradeList = tradeList;
    }

    public void onTradeStart(Player partner) {
        _activeTradeList = new TradeList(this);
        _activeTradeList.setPartner(partner);

        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1).addString(partner.getName()));
        sendPacket(new TradeStart(this));
    }

    public void onTradeConfirm(Player partner) {
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CONFIRMED_TRADE).addString(partner.getName()));

        partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
        sendPacket(TradePressOtherOk.STATIC_PACKET);
    }

    public void onTradeCancel(Player partner) {
        if (_activeTradeList == null) {
            return;
        }

        _activeTradeList.lock();
        _activeTradeList = null;

        sendPacket(SendTradeDone.FAIL_STATIC_PACKET);
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANCELED_TRADE).addString(partner.getName()));
    }

    public void onTradeFinish(boolean isSuccessful) {
        _activeTradeList = null;

        if (isSuccessful) {
            sendPacket(SendTradeDone.SUCCESS_STATIC_PACKET);
            sendPacket(SystemMessageId.TRADE_SUCCESSFUL);
        } else {
            sendPacket(SendTradeDone.FAIL_STATIC_PACKET);
            sendPacket(SystemMessageId.EXCHANGE_HAS_ENDED);
        }
    }

    public void startTrade(Player partner) {
        onTradeStart(partner);
        partner.onTradeStart(this);
    }

    public void cancelActiveTrade() {
        if (_activeTradeList == null) {
            return;
        }

        final Player partner = _activeTradeList.getPartner();
        if (partner != null) {
            partner.onTradeCancel(this);
        }

        onTradeCancel(this);
    }

    /**
     * @return The Buy {@link TradeList} of this {@link Player}.
     */
    public TradeList getBuyList() {
        return _buyList;
    }

    /**
     * @return The Sell {@link TradeList} of this {@link Player}.
     */
    public TradeList getSellList() {
        return _sellList;
    }

    /**
     * @return The {@link ManufactureList} of this {@link Player}.
     */
    public ManufactureList getManufactureList() {
        return _manufactureList;
    }

    /**
     * @return The {@link OperateType} of this {@link Player}.
     */
    public OperateType getOperateType() {
        return _operateType;
    }

    /**
     * Set the {@link OperateType} of this {@link Player}.
     *
     * @param type : The new {@link OperateType} state to set.
     */
    public void setOperateType(OperateType type) {
        _operateType = type;
    }

    /**
     * @return True if this {@link Player} is set on any store mode, or false otherwise.
     */
    public boolean isInStoreMode() {
        return _operateType == OperateType.BUY || _operateType == OperateType.SELL || _operateType == OperateType.PACKAGE_SELL || _operateType == OperateType.MANUFACTURE;
    }

    /**
     * @return True if this {@link Player} is set on any store manage mode, or false otherwise.
     */
    public boolean isInManageStoreMode() {
        return _operateType == OperateType.BUY_MANAGE || _operateType == OperateType.SELL_MANAGE || _operateType == OperateType.MANUFACTURE_MANAGE;
    }

    /**
     * @return True if this {@link Player} can use dwarven recipes.
     */
    public boolean hasDwarvenCraft() {
        return hasSkill(L2Skill.SKILL_CREATE_DWARVEN);
    }

    /**
     * @return True if this {@link Player} can use common recipes.
     */
    public boolean hasCommonCraft() {
        return hasSkill(L2Skill.SKILL_CREATE_COMMON);
    }

    /**
     * Method used by regular leveling system.<br> Reward the {@link Player} with autoGet skills only, or if
     * Config.AUTO_LEARN_SKILLS is activated, with all available skills.
     */
    public void giveSkills() {
        if (Config.AUTO_LEARN_SKILLS) {
            rewardSkills();
        } else {
            // We reward all autoGet skills to this player, but don't store any on database.
            for (final GeneralSkillNode skill : getAvailableAutoGetSkills()) {
                addSkill(skill.getSkill(), false);
            }

            // Remove the Lucky skill if level superior to 10.
            if (getStatus().getLevel() >= 10 && hasSkill(L2Skill.SKILL_LUCKY)) {
                removeSkill(L2Skill.SKILL_LUCKY, false);
            }

            // Remove invalid skills.
            removeInvalidSkills();

            sendSkillList();
        }
    }

    /**
     * Method used by admin commands, Config.AUTO_LEARN_SKILLS or class master.<br> Reward the {@link Player} with all
     * available skills, being autoGet or general skills.
     */
    public void rewardSkills() {
        // We reward all skills to the players, but don't store autoGet skills on the database.
        for (final GeneralSkillNode skill : getAllAvailableSkills()) {
            addSkill(skill.getSkill(), skill.getCost() != 0);
        }

        // Remove the Lucky skill if level superior to 10.
        if (getStatus().getLevel() >= 10 && hasSkill(L2Skill.SKILL_LUCKY)) {
            removeSkill(L2Skill.SKILL_LUCKY, false);
        }

        // Remove invalid skills.
        removeInvalidSkills();

        sendSkillList();
    }

    /**
     * Delete all invalid {@link L2Skill}s for this {@link Player}.<br>
     * <br>
     * A skill is considered invalid when the level of obtention of the skill is superior to 9 compared to player level
     * (expertise skill obtention level is compared to player level without any penalty).<br>
     * <br>
     * It is then either deleted, or level is refreshed.
     */
    private void removeInvalidSkills() {
        if (getSkills().isEmpty()) {
            return;
        }

        // Retrieve the player template skills, based on actual level (+9 for regular skills, unchanged for expertise).
        final Map<Integer, Optional<GeneralSkillNode>> availableSkills = getTemplate().getSkills().stream().filter(s -> s.getMinLvl() <= getStatus().getLevel() + ((s.getId() == L2Skill.SKILL_EXPERTISE) ? 0 : 9)).collect(Collectors.groupingBy(s -> s.getId(), Collectors.maxBy(COMPARE_SKILLS_BY_LVL)));

        for (final L2Skill skill : getSkills().values()) {
            // Bother only with skills existing on template (spare temporary skills, items skills, etc).
            if (getTemplate().getSkills().stream().filter(s -> s.getId() == skill.getId()).count() == 0) {
                continue;
            }

            // The known skill doesn't exist on available skills ; we drop existing skill.
            final Optional<GeneralSkillNode> tempSkill = availableSkills.get(skill.getId());
            if (tempSkill == null) {
                removeSkill(skill.getId(), true);
                continue;
            }

            // Retrieve the skill and max level for enchant scenario.
            final GeneralSkillNode availableSkill = tempSkill.get();
            final int maxLevel = SkillTable.getInstance().getMaxLevel(skill.getId());

            // Case of enchanted skills.
            if (skill.getLevel() > maxLevel) {
                // Player level is inferior to 76, or available skill is a good candidate.
                if ((getStatus().getLevel() < 76 || availableSkill.getValue() < maxLevel) && skill.getLevel() > availableSkill.getValue()) {
                    addSkill(availableSkill.getSkill(), true);
                }
            }
            // We check if current known skill level is bigger than available skill level. If it's true, we override current skill with available skill.
            else if (skill.getLevel() > availableSkill.getValue()) {
                addSkill(availableSkill.getSkill(), true);
            }
        }
    }

    /**
     * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills.<br>
     * <b>Do not call this on enterworld or char load.</b>.
     */
    private void regiveTemporarySkills() {
        // Add noble skills if noble.
        if (isNoble()) {
            setNoble(true, false);
        }

        // Add Hero skills if hero.
        if (isHero()) {
            setHero(true);
        }

        // Add Clan skills.
        if (getClan() != null) {
            getClan().checkAndAddClanSkills(this);

            if (getClan().getLevel() >= Config.MINIMUM_CLAN_LEVEL && isClanLeader()) {
                addSiegeSkills();
            }
        }

        // Reload passive skills from armors / jewels / weapons.
        getInventory().reloadEquippedItems();

        // Add Death Penalty Buff Level.
        if (_deathPenaltyBuffLevel > 0) {
            addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);
        }
    }

    public void addSiegeSkills() {
        for (final L2Skill sk : SkillTable.getInstance().getSiegeSkills(isNoble())) {
            addSkill(sk, false);
        }
    }

    public void removeSiegeSkills() {
        for (final L2Skill sk : SkillTable.getInstance().getSiegeSkills(isNoble())) {
            removeSkill(sk.getId(), false);
        }
    }

    /**
     * @return a {@link List} of all available autoGet {@link GeneralSkillNode}s <b>of maximal level</b> for this
     * {@link Player}.
     */
    public List<GeneralSkillNode> getAvailableAutoGetSkills() {
        final List<GeneralSkillNode> result = new ArrayList<>();

        getTemplate().getSkills().stream().filter(s -> s.getMinLvl() <= getStatus().getLevel() && s.getCost() == 0).collect(Collectors.groupingBy(s -> s.getId(), Collectors.maxBy(COMPARE_SKILLS_BY_LVL))).forEach((i, s) ->
        {
            if (getSkillLevel(i) < s.get().getValue()) {
                result.add(s.get());
            }
        });
        return result;
    }

    /**
     * @return a {@link List} of available {@link GeneralSkillNode}s (only general) for this {@link Player}.
     */
    public List<GeneralSkillNode> getAvailableSkills() {
        final List<GeneralSkillNode> result = new ArrayList<>();

        getTemplate().getSkills().stream().filter(s -> s.getMinLvl() <= getStatus().getLevel() && s.getCost() != 0).forEach(s ->
        {
            if (getSkillLevel(s.getId()) == s.getValue() - 1) {
                result.add(s);
            }
        });
        return result;
    }

    /**
     * @return a {@link List} of all available {@link GeneralSkillNode}s (being general or autoGet) <b>of maximal
     * level</b> for this {@link Player}.
     */
    public List<GeneralSkillNode> getAllAvailableSkills() {
        final List<GeneralSkillNode> result = new ArrayList<>();

        getTemplate().getSkills().stream().filter(s -> s.getMinLvl() <= getStatus().getLevel()).collect(Collectors.groupingBy(s -> s.getId(), Collectors.maxBy(COMPARE_SKILLS_BY_LVL))).forEach((i, s) ->
        {
            if (getSkillLevel(i) < s.get().getValue()) {
                result.add(s.get());
            }
        });
        return result;
    }

    /**
     * Retrieve next lowest level skill to learn, based on current player level and skill sp cost.
     *
     * @return the required level for next {@link GeneralSkillNode} to learn for this {@link Player}.
     */
    public int getRequiredLevelForNextSkill() {
        return getTemplate().getSkills().stream().filter(s -> s.getMinLvl() > getStatus().getLevel() && s.getCost() != 0).min(COMPARE_SKILLS_BY_MIN_LVL).map(s -> s.getMinLvl()).orElse(0);
    }

    /**
     * Set the _clan object, _clanId, _clanLeader Flag and title of the Player.
     *
     * @param clan The Clan object which is used to feed Player values.
     */
    public void setClan(Clan clan) {
        _clan = clan;
        setTitle("");

        if (clan == null) {
            _clanId = 0;
            _clanPrivileges = 0;
            _pledgeType = 0;
            _powerGrade = 0;
            _lvlJoinedAcademy = 0;
            _apprentice = 0;
            _sponsor = 0;
            return;
        }

        if (!clan.isMember(getObjectId())) {
            // char has been kicked from clan
            setClan(null);
            return;
        }

        _clanId = clan.getClanId();
    }

    /**
     * @return The _clan object of the Player.
     */
    public Clan getClan() {
        return _clan;
    }

    /**
     * @return True if the Player is the leader of its clan.
     */
    public boolean isClanLeader() {
        return _clan != null && getObjectId() == _clan.getLeaderId();
    }

    /**
     * Reduce the number of arrows owned by the Player and send InventoryUpdate or ItemList (to unequip if the last
     * arrow was consummed).
     */
    @Override
    public void reduceArrowCount() // TODO: replace with a simple player.destroyItem...
    {
        final ItemInstance arrows = getSecondaryWeaponInstance();
        if (arrows == null) {
            return;
        }

        final InventoryUpdate iu = new InventoryUpdate();

        if (arrows.getCount() > 1) {
            synchronized (arrows) {
                arrows.changeCount(null, -1, this, null);
                arrows.setLastChange(ItemState.MODIFIED);

                iu.addModifiedItem(arrows);

                // could do also without saving, but let's save approx 1 of 10
                if (Rnd.get(10) < 1) {
                    ItemDao.updateCount(arrows);
                }

                _inventory.refreshWeight();
            }
        } else {
            iu.addRemovedItem(arrows);

            // Destroy entire item and save to database
            _inventory.destroyItem("Consume", arrows, this, null);
        }
        sendPacket(iu);
    }

    /**
     * Check if the arrow item exists on inventory and is already slotted ; if not, equip it.
     */
    @Override
    public boolean checkAndEquipArrows() {
        // Retrieve arrows instance on player inventory.
        final ItemInstance arrows = getInventory().findArrowForBow(getActiveWeaponItem());
        if (arrows == null) {
            return false;
        }

        // Arrows are already equiped, don't bother.
        if (arrows.getLocation() == ItemLocation.PAPERDOLL) {
            return true;
        }

        // Equip arrows in left hand.
        getInventory().setPaperdollItem(Paperdoll.LHAND, arrows);

        // Send ItemList to this player to update left hand equipement
        sendPacket(new ItemList(this, false));

        return true;
    }

    /**
     * Disarm the {@link Player}'s weapon.
     *
     * @param leftHandIncluded : If set to True, the left hand item is also disarmed.
     * @return True if successful, false otherwise.
     */
    public boolean disarmWeapon(boolean leftHandIncluded) {
        // Don't allow disarming a cursed weapon.
        if (isCursedWeaponEquipped()) {
            return false;
        }

        // Cancel current attack, no matter what.
        getAttack().stop();

        InventoryUpdate iu = null;

        // Unequip the weapon.
        ItemInstance[] unequipped = getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_R_HAND);
        if (!ArraysUtil.isEmpty(unequipped)) {
            iu = new InventoryUpdate();
            for (final ItemInstance itm : unequipped) {
                iu.addModifiedItem(itm);
            }

            SystemMessage sm;
            if (unequipped[0].getEnchantLevel() > 0) {
                sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
            } else {
                sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]);
            }

            sendPacket(sm);
        }

        // Unequip the shield.
        if (leftHandIncluded) {
            unequipped = getInventory().unequipItemInBodySlotAndRecord(Item.SLOT_L_HAND);
            if (!ArraysUtil.isEmpty(unequipped)) {
                if (iu == null) {
                    iu = new InventoryUpdate();
                }

                for (final ItemInstance itm : unequipped) {
                    iu.addModifiedItem(itm);
                }

                SystemMessage sm;
                if (unequipped[0].getEnchantLevel() > 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
                } else {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]);
                }

                sendPacket(sm);
            }
        }

        // If InventoryUpdate packet is filled, it means either weapon or sidearm has been disarmed. An update is needed.
        if (iu != null) {
            sendPacket(iu);
            broadcastUserInfo();
        }

        return true;
    }

    public boolean mount(Summon pet) {
        if (!disarmWeapon(true)) {
            return false;
        }

        forceRunStance();
        stopAllToggles();

        final Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().getNpcId());
        setMount(pet.getNpcId(), pet.getStatus().getLevel(), mount.getMountType());

        _petTemplate = (PetTemplate) pet.getTemplate();
        _petData = _petTemplate.getPetDataEntry(pet.getStatus().getLevel());
        _mountObjectId = pet.getControlItemObjectId();

        startFeed(pet.getNpcId());
        broadcastPacket(mount);

        // Notify self and others about speed change
        broadcastUserInfo();

        pet.unSummon(this);
        return true;
    }

    public boolean mount(int npcId, int controlItemId) {
        if (!disarmWeapon(true)) {
            return false;
        }

        forceRunStance();
        stopAllToggles();

        final Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, npcId);

        setMount(npcId, getStatus().getLevel(), mount.getMountType());

        _petTemplate = (PetTemplate) NpcData.getInstance().getTemplate(npcId);
        _petData = _petTemplate.getPetDataEntry(getStatus().getLevel());
        _mountObjectId = controlItemId;

        broadcastPacket(mount);

        // Notify self and others about speed change
        broadcastUserInfo();

        startFeed(npcId);
        return true;
    }

    /**
     * Test if this {@link Player} can mount the selected {@link Summon} or dismount if already mounted, and act
     * accordingly.<br>
     * <br>
     * This method is used by both "Actions" panel "Mount/Dismount" button, and /mount /dismount usercommands.
     *
     * @param summon : The Summon to check.
     */
    public void mountPlayer(Summon summon) {
        if (summon instanceof Pet && summon.isMountable() && !isMounted() && !isBetrayed()) {
            // A strider cannot be ridden when dead.
            if (isDead()) {
                sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
                return;
            }

            // A dead strider cannot be ridden.
            if (summon.isDead()) {
                sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
                return;
            }

            // A strider in battle cannot be ridden.
            if (summon.isInCombat() || summon.isRooted()) {
                sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
                return;
            }

            // A strider cannot be ridden while in battle.
            if (isInCombat() || isCursedWeaponEquipped()) {
                sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
                return;
            }

            // A strider can be ridden only when standing.
            if (isSitting()) {
                sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
                return;
            }

            // You can't mount while fishing.
            if (isFishing()) {
                sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
                return;
            }

            // Distance check.
            if (!MathUtil.checkIfInRange(200, this, summon, true)) {
                sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_STRIDER_TO_MOUNT);
                return;
            }

            // Pet is hungry.
            if (((Pet) summon).checkHungryState()) {
                sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
                return;
            }

            if (!summon.isDead() && !isMounted()) {
                mount(summon);
            }
        } else if (isMounted()) {
            // Wyvern mounted.
            if (getMountType() == 2) {
                // Invalid zone.
                if (isInsideZone(ZoneId.NO_LANDING)) {
                    sendPacket(SystemMessageId.NO_DISMOUNT_HERE);
                    return;
                }

                // Unsafe fall height.
                if (Math.abs(getZ() - GeoEngine.getInstance().getHeight(getPosition())) > getTemplate().getSafeFallHeight(getAppearance().getSex())) {
                    sendPacket(SystemMessageId.CANNOT_DISMOUNT_FROM_ELEVATION);
                    return;
                }
            }

            // Hungry state.
            if (checkFoodState(_petTemplate.getHungryLimit())) {
                sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
                return;
            }

            dismount();
        }
    }

    public void dismount() {
        sendPacket(new SetupGauge(GaugeColor.GREEN, 0));

        final int petId = _mountNpcId;

        setMount(0, 0, 0);
        stopFeed();

        broadcastPacket(new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0));

        _petTemplate = null;
        _petData = null;
        _mountObjectId = 0;

        storePetFood(petId);

        // Notify self and others about speed change
        broadcastUserInfo();
    }

    public void storePetFood(int petId) {
        if (_controlItemId != 0 && petId != 0) {
            try (Connection con = ConnectionPool.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?")) {
                ps.setInt(1, getCurrentFeed());
                ps.setInt(2, _controlItemId);
                ps.executeUpdate();

                _controlItemId = 0;
            } catch (final Exception e) {
                log.error("Couldn't store pet food data for {}.", e, _controlItemId);
            }
        }
    }

    protected class FeedTask implements Runnable {
        @Override
        public void run() {
            if (!isMounted()) {
                stopFeed();
                return;
            }

            // Eat or return to pet control item.
            if (getCurrentFeed() > getFeedConsume()) {
                setCurrentFeed(getCurrentFeed() - getFeedConsume());
            } else {
                // Keep temporary track of current fly state for future usage.
                final boolean wasFlying = isFlying();

                setCurrentFeed(0);
                stopFeed();
                dismount();
                sendPacket(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED);

                // If Player was wyvern mounted, then he is teleported to nearest town to avoid the falling damage.
                if (wasFlying) {
                    teleportTo(TeleportType.TOWN);
                }

                return;
            }

            ItemInstance food = getInventory().getItemByItemId(_petTemplate.getFood1());
            if (food == null) {
                food = getInventory().getItemByItemId(_petTemplate.getFood2());
            }

            if (food != null && checkFoodState(_petTemplate.getAutoFeedLimit())) {
                final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
                if (handler != null) {
                    handler.useItem(Player.this, food, false);
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(food));
                }
            }
        }
    }

    private synchronized void startFeed(int npcId) {
        _canFeed = npcId > 0;
        if (!isMounted()) {
            return;
        }

        if (_summon != null) {
            setCurrentFeed(((Pet) _summon).getCurrentFed());
            _controlItemId = _summon.getControlItemObjectId();
            sendPacket(new SetupGauge(GaugeColor.GREEN, getCurrentFeed() * 10000 / getFeedConsume(), _petData.getMaxMeal() * 10000 / getFeedConsume()));
            if (!isDead()) {
                _mountFeedTask = ThreadPool.scheduleAtFixedRate(new FeedTask(), 10000, 10000);
            }
        } else if (_canFeed) {
            setCurrentFeed(_petData.getMaxMeal());
            sendPacket(new SetupGauge(GaugeColor.GREEN, getCurrentFeed() * 10000 / getFeedConsume(), _petData.getMaxMeal() * 10000 / getFeedConsume()));
            if (!isDead()) {
                _mountFeedTask = ThreadPool.scheduleAtFixedRate(new FeedTask(), 10000, 10000);
            }
        }
    }

    private synchronized void stopFeed() {
        if (_mountFeedTask != null) {
            _mountFeedTask.cancel(false);
            _mountFeedTask = null;
        }
    }

    public PetTemplate getPetTemplate() {
        return _petTemplate;
    }

    public PetDataEntry getPetDataEntry() {
        return _petData;
    }

    public int getCurrentFeed() {
        return _curFeed;
    }

    private int getFeedConsume() {
        return (isInCombat()) ? _petData.getMountMealInBattle() : _petData.getMountMealInNormal();
    }

    public void setCurrentFeed(int num) {
        _curFeed = Math.min(num, _petData.getMaxMeal());

        sendPacket(new SetupGauge(GaugeColor.GREEN, getCurrentFeed() * 10000 / getFeedConsume(), _petData.getMaxMeal() * 10000 / getFeedConsume()));
    }

    /**
     * @param state : The state to check (can be autofeed, hungry or unsummon).
     * @return true if the limit is reached, false otherwise or if there is no need to feed.
     */
    public boolean checkFoodState(double state) {
        return _canFeed && getCurrentFeed() < _petData.getMaxMeal() * state;
    }

    public void setUptime(long time) {
        _uptime = time;
    }

    public long getUptime() {
        return System.currentTimeMillis() - _uptime;
    }

    /**
     * Return True if the Player is invulnerable.
     */
    @Override
    public boolean isInvul() {
        return super.isInvul() || isSpawnProtected();
    }

    /**
     * Return True if the Player has a Party in progress.
     */
    @Override
    public boolean isInParty() {
        return _party != null;
    }

    /**
     * Set the _party object of the Player (without joining it).
     *
     * @param party The object.
     */
    public void setParty(Party party) {
        _party = party;
    }

    /**
     * Return the _party object of the Player.
     */
    @Override
    public Party getParty() {
        return _party;
    }

    public LootRule getLootRule() {
        return _lootRule;
    }

    public void setLootRule(LootRule lootRule) {
        _lootRule = lootRule;
    }

    /**
     * Return True if the Player is a GM.
     */
    @Override
    public boolean isGM() {
        return getAccessLevel().isGm();
    }

    /**
     * Set the {@link AccessLevel} of this {@link Player}.
     * <ul>
     * <li>If invalid, set the default user access level 0.</li>
     * <li>If superior to 0, it means it's a special access.</li>
     * </ul>
     *
     * @param level : The level to set.
     */
    public void setAccessLevel(int level) {
        // Retrieve the AccessLevel. Even if not existing, it returns user level.
        AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
        if (accessLevel == null) {
            log.warn("An invalid access level {} has been granted for {}, therefore it has been reset.", level, this);
            accessLevel = AdminData.getInstance().getAccessLevel(0);
        }

        _accessLevel = accessLevel;

        if (level > 0) {
            // For level lower or equal to user, we don't apply AccessLevel name as title.
            setTitle(accessLevel.getName());

            // We log master access.
            if (level == AdminData.getInstance().getMasterAccessLevel()) {
                log.info("{} has logged in with Master access level.", getName());
            }
        }

        // We refresh GMList if the access level is GM.
        if (accessLevel.isGm()) {
            // A little hack to avoid Enterworld config to be replaced.
            if (!AdminData.getInstance().isRegisteredAsGM(this)) {
                AdminData.getInstance().addGm(this, false);
            }
        } else {
            AdminData.getInstance().deleteGm(this);
        }

        getAppearance().setNameColor(accessLevel.getNameColor());
        getAppearance().setTitleColor(accessLevel.getTitleColor());
        broadcastUserInfo();

        PlayerInfoTable.getInstance().updatePlayerData(this, true);
    }

    public void setAccountAccesslevel(int level) {
        LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
    }

    /**
     * @return the _accessLevel of the Player.
     */
    public AccessLevel getAccessLevel() {
        return _accessLevel;
    }

    public void setEnterWorldLoc(int x, int y, int z) {
        _enterWorld = new Location(x, y, z);
    }

    public ExServerPrimitive getDebugPacket(String name) {
        return _debug.computeIfAbsent(name, p -> new ExServerPrimitive(name, _enterWorld));
    }

    public void clearDebugPackets() {
        _debug.values().stream().peek(ExServerPrimitive::reset).forEach(esp -> esp.sendTo(this));
    }

    /**
     * Update Stats of the Player client side by sending UserInfo/StatusUpdate to this Player and CharInfo/StatusUpdate
     * to all Player in its _KnownPlayers (broadcast).
     *
     * @param broadcastType
     */
    public void updateAndBroadcastStatus(int broadcastType) {
        refreshWeightPenalty();
        refreshExpertisePenalty();

        if (broadcastType == 1) {
            sendPacket(new UserInfo(this));
        } else if (broadcastType == 2) {
            broadcastUserInfo();
        }
    }

    /**
     * Send StatusUpdate packet with Karma to the Player and all Player to inform (broadcast).
     */
    public void broadcastKarma() {
        final StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusType.KARMA, getKarma());
        sendPacket(su);

        if (_summon != null) {
            sendPacket(new RelationChanged(_summon, getRelation(this), false));
        }

        broadcastRelationsChanges();
    }

    /**
     * Set the online Flag to True or False and update the characters table of the database with online status and
     * lastAccess (called when login and logout).
     *
     * @param isOnline
     * @param updateInDb
     */
    public void setOnlineStatus(boolean isOnline, boolean updateInDb) {
        if (_isOnline != isOnline) {
            _isOnline = isOnline;
        }

        // Update the characters table of the database with online status and lastAccess (called when login and logout)
        if (updateInDb) {
            updateOnlineStatus();
        }
    }

    public void setIsIn7sDungeon(boolean isIn7sDungeon) {
        _isIn7sDungeon = isIn7sDungeon;
    }

    /**
     * Update the characters table of the database with online status and lastAccess of this Player (called when login
     * and logout).
     */
    public void updateOnlineStatus() {
        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?")) {
            ps.setInt(1, isOnlineInt());
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, getObjectId());
            ps.execute();
        } catch (final Exception e) {
            log.error("Couldn't set player online status.", e);
        }
    }

    public Forum getMemo() {
        if (_forumMemo == null) {
            _forumMemo = CommunityBoard.getInstance().getOrCreateForum(ForumType.MEMO, ForumAccess.ALL, getObjectId());
        }

        return _forumMemo;
    }

    /**
     * Restores secondary data for the Player, based on the current class index.
     */
    void restoreCharData() {
        // Retrieve from the database all skills of this Player and add them to _skills.
        PlayerDao.restoreSkills(this);

        // Retrieve from the database all macroses of this Player and add them to _macroses.
        _macroList.restore();

        // Retrieve from the database the recipe book of this Player.
        if (!isSubClassActive()) {
            _recipeBook.restore();
        }

        // Retrieve from the database all shortCuts of this Player and add them to _shortCuts.
        _shortcutList.restore();

        // Retrieve from the database all henna of this Player and add them to _henna.
        _hennaList.restore();

        // Retrieve from the database all recom data of this Player and add them to _recomChars.
        PlayerDao.restoreRecommends(this);

        // Retrieve from the database all quest states and variables for this Player and add them to _quests.
        _questList.restore();
    }

    /**
     * Update Player stats in the characters table of the database.
     *
     * @param storeActiveEffects
     */
    public synchronized void store(boolean storeActiveEffects) {
        PlayerDao.update(this);
        PlayerDao.updateSubClass(this);
        PlayerDao.saveEffects(this, storeActiveEffects);
    }

    public void store() {
        store(true);
    }

    /**
     * @return True if the Player is online.
     */
    public boolean isOnline() {
        return _isOnline;
    }

    /**
     * @return an int interpretation of online status.
     */
    public int isOnlineInt() {
        if (_isOnline && getClient() != null) {
            return getClient().isDetached() ? 2 : 1;
        }

        return 0;
    }

    public boolean isIn7sDungeon() {
        return _isIn7sDungeon;
    }

    /**
     * Add a {@link L2Skill} and its Func objects to the calculator set of the {@link Player}. Don't refresh shortcuts.
     *
     * @param newSkill : The skill to add.
     * @param store : If true, we save the skill on database.
     * @return true if the skill has been successfully added.
     * @see Player#addSkill(L2Skill, boolean, boolean)
     */
    public boolean addSkill(L2Skill newSkill, boolean store) {
        return addSkill(newSkill, store, false);
    }

    /**
     * Add a {@link L2Skill} and its Func objects to the calculator set of the {@link Player}.<BR>
     * <ul>
     * <li>Replace or add oldSkill by newSkill (only if oldSkill is different than newSkill)</li>
     * <li>If an old skill has been replaced, remove all its Func objects of Creature calculator set</li>
     * <li>Add Func objects of newSkill to the calculator set of the Creature</li>
     * </ul>
     *
     * @param newSkill : The skill to add.
     * @param store : If true, we save the skill on database.
     * @param updateShortcuts : If true, we refresh all shortcuts associated to that skill (should be only called when
     * skill upgrades, either manually or by trainer).
     * @return true if the skill has been successfully added.
     */
    public boolean addSkill(L2Skill newSkill, boolean store, boolean updateShortcuts) {
        // New skill is null, abort.
        if (newSkill == null) {
            return false;
        }

        // Search the old skill. We test if it's different than the new one. If yes, we abort the operation.
        final L2Skill oldSkill = getSkills().get(newSkill.getId());
        if (oldSkill != null && oldSkill.equals(newSkill)) {
            return false;
        }

        // The 2 skills were different (or old wasn't existing). We can refresh the map.
        getSkills().put(newSkill.getId(), newSkill);

        // If an old skill has been replaced, remove all its Func objects
        if (oldSkill != null) {
            // if skill came with another one, we should delete the other one too.
            if (oldSkill.triggerAnotherSkill()) {
                removeSkill(oldSkill.getTriggeredId(), false);
            }

            removeStatsByOwner(oldSkill);
        }

        // Add Func objects of newSkill to the calculator set of the Creature
        addStatFuncs(newSkill.getStatFuncs(this));

        // Test and delete chance skill if found.
        if (oldSkill != null && getChanceSkills() != null) {
            removeChanceSkill(oldSkill.getId());
        }

        // If new skill got a chance, trigger it.
        if (newSkill.isChance()) {
            addChanceTrigger(newSkill);
        }

        // Add or update the skill in the database.
        if (store) {
            PlayerDao.saveSkill(this, newSkill, -1);
        }

        // Update shortcuts.
        if (updateShortcuts) {
            getShortcutList().refreshShortcuts(newSkill.getId(), newSkill.getLevel(), ShortcutType.SKILL);
        }

        return true;
    }

    /**
     * Remove a {@link L2Skill} from this {@link Player}. If parameter store is true, we also remove it from database
     * and update shortcuts.
     *
     * @param skillId : The skill identifier to remove.
     * @param store : If true, we delete the skill from database.
     * @return the L2Skill removed or null if it couldn't be removed.
     */
    public L2Skill removeSkill(int skillId, boolean store) {
        return removeSkill(skillId, store, true);
    }

    /**
     * Remove a {@link L2Skill} from this {@link Player}. If parameter store is true, we also remove it from database
     * and update shortcuts.
     *
     * @param skillId : The skill identifier to remove.
     * @param store : If true, we delete the skill from database.
     * @param removeEffect : If true, we remove the associated effect if existing.
     * @return the L2Skill removed or null if it couldn't be removed.
     */
    public L2Skill removeSkill(int skillId, boolean store, boolean removeEffect) {
        // Remove the skill from the Creature _skills
        final L2Skill oldSkill = getSkills().remove(skillId);
        if (oldSkill == null) {
            return null;
        }

        // this is just a fail-safe againts buggers and gm dummies...
        if (oldSkill.triggerAnotherSkill() && oldSkill.getTriggeredId() > 0) {
            removeSkill(oldSkill.getTriggeredId(), false);
        }

        // Stop casting if this skill is used right now
        if (getCast().getCurrentSkill() != null && skillId == getCast().getCurrentSkill().getId()) {
            getCast().stop();
        }

        // Remove all its Func objects from the Creature calculator set
        if (removeEffect) {
            removeStatsByOwner(oldSkill);
            stopSkillEffects(skillId);
        }

        if (oldSkill.isChance() && getChanceSkills() != null) {
            removeChanceSkill(skillId);
        }

        if (store) {
            PlayerDao.deleteSkill(this, skillId);

            // Don't busy testing shortcuts if skill was a passive skill.
            if (!oldSkill.isPassive()) {
                getShortcutList().deleteShortcuts(skillId, ShortcutType.SKILL);
            }
        }
        return oldSkill;
    }

    /**
     * @return the {@link HennaList} of this {@link Player}.
     */
    public HennaList getHennaList() {
        return _hennaList;
    }

    /**
     * Refresh the {@link HennaList} of this {@link Player}.
     */
    public void refreshHennaList() {
        _hennaList.recalculateStats();

        sendPacket(new HennaInfo(this));
    }

    /**
     * @param objectId : The looter object to make checks on.
     * @return true if the active player is the looter or in the same party or command channel than looter objectId.
     */
    public boolean isLooterOrInLooterParty(int objectId) {
        if (objectId == getObjectId()) {
            return true;
        }

        final Player looter = World.getInstance().getPlayer(objectId);
        if (looter == null) {
            return false;
        }

        if (_party == null) {
            return false;
        }

        final CommandChannel channel = _party.getCommandChannel();
        return (channel != null) ? channel.containsPlayer(looter) : _party.containsPlayer(looter);
    }

    public boolean canCastBeneficialSkillOnPlayable(Playable target, L2Skill skill, boolean isCtrlPressed) {
        // You can do beneficial skills on yourself anytime
        if (this == target) {
            return true;
        }

        final Player targetPlayer = target.getActingPlayer();
        // No checks for players in Arena
        if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)) {
            return true;
        }

        // No checks for players in Olympiad
        if (isInOlympiadMode() && targetPlayer.isInOlympiadMode() && getOlympiadGameId() == targetPlayer.getOlympiadGameId()) {
            return true;
        }

        // No checks for players in Duel
        if (isInDuel() && targetPlayer.isInDuel() && getDuelId() == targetPlayer.getDuelId()) {
            return true;
        }

        final boolean sameParty = (isInParty() && targetPlayer.isInParty() && getParty().getLeader() == targetPlayer.getParty().getLeader());
        final boolean sameCommandChannel = (isInParty() && targetPlayer.isInParty() && getParty().getCommandChannel() != null && getParty().getCommandChannel().containsPlayer(targetPlayer));
        final boolean sameClan = (getClanId() > 0 && getClanId() == targetPlayer.getClanId());
        final boolean sameAlliance = (getAllyId() > 0 && getAllyId() == targetPlayer.getAllyId());
        if (sameParty || sameCommandChannel || sameClan || sameAlliance) {
            return true;
        }

        // If the target not from the same CC/party/alliance/clan is flagged / PK, you can buff with isCtrlPressed.
        if (targetPlayer.getPvpFlag() > 0 || targetPlayer.getKarma() > 0) {
            return isCtrlPressed;
        }

        // If the target not from the same CC/party/alliance/clan is white, it may be freely buffed
        return true;
    }

    /**
     * @return True if the Player is a Mage (based on class templates).
     */
    public boolean isMageClass() {
        return getClassId().getType() != ClassType.FIGHTER;
    }

    public boolean isMounted() {
        return _mountType > 0;
    }

    /**
     * This method allows to :
     * <ul>
     * <li>change isRiding/isFlying flags</li>
     * <li>gift player with Wyvern Breath skill if mount is a wyvern</li>
     * <li>send the skillList (faded icons update)</li>
     * </ul>
     *
     * @param npcId the npcId of the mount
     * @param npcLevel The level of the mount
     * @param mountType 0, 1 or 2 (dismount, strider or wyvern).
     */
    public void setMount(int npcId, int npcLevel, int mountType) {
        switch (mountType) {
            case 0: // Dismounted
                if (isFlying()) {
                    removeSkill(FrequentSkill.WYVERN_BREATH.getSkill().getId(), false);
                }
            case 1:
                getMove().removeMoveType(MoveType.FLY);
                break;

            case 2: // Flying Wyvern
                addSkill(FrequentSkill.WYVERN_BREATH.getSkill(), false);
                getMove().addMoveType(MoveType.FLY);
                break;
        }

        _mountNpcId = npcId;
        _mountType = mountType;
        _mountLevel = npcLevel;

        // Update faded icons && eventual added skills.
        sendSkillList();
    }

    @Override
    public boolean isSeated() {
        return _throneId > 0;
    }

    public int getThroneId() {
        return _throneId;
    }

    public void setThroneId(int id) {
        _throneId = id;
    }

    @Override
    public boolean isRiding() {
        return _mountType == 1;
    }

    @Override
    public boolean isFlying() {
        return _mountType == 2;
    }

    /**
     * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern).
     */
    public int getMountType() {
        return _mountType;
    }

    @Override
    public void stopAllEffects() {
        super.stopAllEffects();
        updateAndBroadcastStatus(2);
    }

    @Override
    public void stopAllEffectsExceptThoseThatLastThroughDeath() {
        super.stopAllEffectsExceptThoseThatLastThroughDeath();
        updateAndBroadcastStatus(2);
    }

    /**
     * Stop all toggle-type effects
     */
    public void stopAllToggles() {
        _effects.stopAllToggles();
    }

    /**
     * Send UserInfo to this Player and CharInfo to all Player in its _KnownPlayers.<BR>
     * <ul>
     * <li>Send UserInfo to this Player (Public and Private Data)</li>
     * <li>Send CharInfo to all Player in _KnownPlayers of the Player (Public data only)</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
     * <BR>
     */
    @Override
    public void updateAbnormalEffect() {
        broadcastUserInfo();
    }

    /**
     * Disable the Inventory and create a new task to enable it after 1.5s.
     */
    public void tempInventoryDisable() {
        _inventoryDisable = true;

        ThreadPool.schedule(() -> _inventoryDisable = false, 1500);
    }

    /**
     * @return True if the Inventory is disabled.
     */
    public boolean isInventoryDisabled() {
        return _inventoryDisable;
    }

    @Override
    public String toString() {
        return getName() + " (" + getObjectId() + ")";
    }

    /**
     * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
     */
    public int getEnchantEffect() {
        final ItemInstance wpn = getActiveWeaponInstance();
        return (wpn == null) ? 0 : Math.min(127, wpn.getEnchantLevel());
    }

    /**
     * Remember the current {@link Folk} of the {@link Player}, used notably for integrity check.
     *
     * @param folk : The Folk to remember.
     */
    public void setCurrentFolk(Folk folk) {
        _currentFolk = folk;
    }

    /**
     * @return the current {@link Folk} of the {@link Player}.
     */
    public Folk getCurrentFolk() {
        return _currentFolk;
    }

    /**
     * @return True if Player is a participant in the Festival of Darkness.
     */
    public boolean isFestivalParticipant() {
        return FestivalOfDarknessManager.getInstance().isParticipant(this);
    }

    public void addAutoSoulShot(int itemId) {
        _activeSoulShots.add(itemId);
    }

    public boolean removeAutoSoulShot(int itemId) {
        return _activeSoulShots.remove(itemId);
    }

    public Set<Integer> getAutoSoulShot() {
        return _activeSoulShots;
    }

    @Override
    public boolean isChargedShot(ShotType type) {
        final ItemInstance weapon = getActiveWeaponInstance();
        return weapon != null && weapon.isChargedShot(type);
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        final ItemInstance weapon = getActiveWeaponInstance();
        if (weapon != null) {
            weapon.setChargedShot(type, charged);
        }
    }

    @Override
    public void rechargeShots(boolean physical, boolean magic) {
        if (_activeSoulShots.isEmpty()) {
            return;
        }

        for (final int itemId : _activeSoulShots) {
            final ItemInstance item = getInventory().getItemByItemId(itemId);
            if (item != null) {
                if (magic && item.getItem().getDefaultAction() == ActionType.spiritshot) {
                    final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                    if (handler != null) {
                        handler.useItem(this, item, false);
                    }
                }

                if (physical && item.getItem().getDefaultAction() == ActionType.soulshot) {
                    final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
                    if (handler != null) {
                        handler.useItem(this, item, false);
                    }
                }
            } else {
                removeAutoSoulShot(itemId);
            }
        }
    }

    /**
     * Cancel autoshot use for shot itemId
     *
     * @param itemId int id to disable
     * @return true if canceled.
     */
    public boolean disableAutoShot(int itemId) {
        if (_activeSoulShots.contains(itemId)) {
            removeAutoSoulShot(itemId);
            sendPacket(new ExAutoSoulShot(itemId, 0));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
            return true;
        }

        return false;
    }

    /**
     * Cancel all autoshots for player
     */
    public void disableAutoShotsAll() {
        for (final int itemId : _activeSoulShots) {
            sendPacket(new ExAutoSoulShot(itemId, 0));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
        }
        _activeSoulShots.clear();
    }

    public int getClanPrivileges() {
        return _clanPrivileges;
    }

    public void setClanPrivileges(int privs) {
        _clanPrivileges = privs;
    }

    public boolean hasClanPrivileges(int priv) {
        return (_clanPrivileges & priv) == priv;
    }

    public int getPledgeClass() {
        return _pledgeClass;
    }

    public void setPledgeClass(int classId) {
        _pledgeClass = classId;
    }

    public int getPledgeType() {
        return _pledgeType;
    }

    public void setPledgeType(int typeId) {
        _pledgeType = typeId;
    }

    public int getApprentice() {
        return _apprentice;
    }

    public void setApprentice(int id) {
        _apprentice = id;
    }

    public int getSponsor() {
        return _sponsor;
    }

    public void setSponsor(int id) {
        _sponsor = id;
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(message, SystemMessageColor.DEFAULT);
    }

    @Override
    public void sendMessage(String message, SystemMessageColor color) {
        sendPacket(SystemMessage.sendString(message, color));
    }

    @Override
    public void teleportTo(int x, int y, int z, int randomOffset) {
        // Set the Boat as null upon teleport.
        setBoat(null);

        super.teleportTo(x, y, z, randomOffset);
    }

    /**
     * Unsummon all types of summons : pets, cubics, normal summons and trained beasts.
     */
    public void dropAllSummons() {
        // Delete summons and pets
        if (_summon != null) {
            _summon.unSummon(this);
        }

        // Delete trained beasts
        if (_tamedBeast != null) {
            _tamedBeast.deleteMe();
        }

        // Delete any form of cubics
        _cubicList.stopCubics(true);
    }

    public void enterObserverMode(int x, int y, int z) {
        dropAllSummons();

        if (getParty() != null) {
            getParty().removePartyMember(this, MessageType.EXPELLED);
        }

        standUp();

        _savedLocation.set(getPosition());

        setInvul(true);
        getAppearance().setVisible(false);
        setIsParalyzed(true);

        // Abort attack, cast and move.
        abortAll(true);

        teleportTo(x, y, z, 0);
        sendPacket(new ObservationMode(x, y, z));
    }

    public void enterOlympiadObserverMode(int id) {
        final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(id);
        if (task == null) {
            return;
        }

        dropAllSummons();

        if (getParty() != null) {
            getParty().removePartyMember(this, MessageType.EXPELLED);
        }

        _olympiadGameId = id;

        standUp();

        // Don't override saved location if we jump from stadium to stadium.
        if (!isInObserverMode()) {
            _savedLocation.set(getPosition());
        }

        setTarget(null);
        setInvul(true);
        getAppearance().setVisible(false);

        teleportTo(task.getZone().getSpawns(SpawnType.NORMAL).get(2), 0);
        sendPacket(new ExOlympiadMode(3));
    }

    public void leaveObserverMode() {
        getAI().tryToIdle();

        setTarget(null);
        getAppearance().setVisible(true);
        setInvul(false);
        setIsParalyzed(false);

        sendPacket(new ObservationReturn(_savedLocation));
        teleportTo(_savedLocation, 0);

        // Clear the location.
        _savedLocation.clean();
    }

    public void leaveOlympiadObserverMode() {
        if (_olympiadGameId == -1) {
            return;
        }

        _olympiadGameId = -1;

        getAI().tryToIdle();

        setTarget(null);
        getAppearance().setVisible(true);
        setInvul(false);

        sendPacket(new ExOlympiadMode(0));
        teleportTo(_savedLocation, 0);

        // Clear the location.
        _savedLocation.clean();
    }

    public int getOlympiadSide() {
        return _olympiadSide;
    }

    public void setOlympiadSide(int i) {
        _olympiadSide = i;
    }

    public int getOlympiadGameId() {
        return _olympiadGameId;
    }

    public void setOlympiadGameId(int id) {
        _olympiadGameId = id;
    }

    public Location getSavedLocation() {
        return _savedLocation;
    }

    public boolean isInObserverMode() {
        return !_isInOlympiadMode && !_savedLocation.equals(Location.DUMMY_LOC);
    }

    public TeleportMode getTeleportMode() {
        return _teleportMode;
    }

    public void setTeleportMode(TeleportMode mode) {
        _teleportMode = mode;
    }

    public int getLoto(int i) {
        return _loto[i];
    }

    public void setLoto(int i, int val) {
        _loto[i] = val;
    }

    public int getRace(int i) {
        return _race[i];
    }

    public void setRace(int i, int val) {
        _race[i] = val;
    }

    public BlockList getBlockList() {
        return _blockList;
    }

    public QuestList getQuestList() {
        return _questList;
    }

    public boolean isHero() {
        return _isHero;
    }

    public void setHero(boolean hero) {
        if (hero && _baseClass == _activeClass) {
            for (final L2Skill skill : SkillTable.getHeroSkills()) {
                addSkill(skill, false);
            }
        } else {
            for (final L2Skill skill : SkillTable.getHeroSkills()) {
                removeSkill(skill.getId(), false);
            }
        }
        _isHero = hero;

        sendSkillList();
    }

    public boolean isOlympiadStart() {
        return _isInOlympiadStart;
    }

    public void setOlympiadStart(boolean b) {
        _isInOlympiadStart = b;
    }

    public boolean isInOlympiadMode() {
        return _isInOlympiadMode;
    }

    public void setOlympiadMode(boolean b) {
        _isInOlympiadMode = b;
    }

    public boolean isInDuel() {
        return _duelId > 0;
    }

    public int getDuelId() {
        return _duelId;
    }

    public void setDuelState(DuelState state) {
        _duelState = state;
    }

    public DuelState getDuelState() {
        return _duelState;
    }

    /**
     * Sets up the duel state using a non 0 duelId.
     *
     * @param duelId 0=not in a duel
     */
    public void setInDuel(int duelId) {
        if (duelId > 0) {
            _duelState = DuelState.ON_COUNTDOWN;
            _duelId = duelId;
        } else {
            if (_duelState == DuelState.DEAD) {
                enableAllSkills();
                getStatus().startHpMpRegeneration();
            }
            _duelState = DuelState.NO_DUEL;
            _duelId = 0;
        }
    }

    /**
     * This returns a SystemMessage stating why the player is not available for duelling.
     *
     * @return S1_CANNOT_DUEL... message
     */
    public SystemMessage getNoDuelReason() {
        // Prepare the message with the good reason.
        final SystemMessage sm = SystemMessage.getSystemMessage(_noDuelReason).addCharName(this);

        // Reinitialize the reason.
        _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;

        // Send stored reason.
        return sm;
    }

    /**
     * Checks if this player might join / start a duel. To get the reason use getNoDuelReason() after calling this
     * function.
     *
     * @return true if the player might join/start a duel.
     */
    public boolean canDuel() {
        if (isInCombat() || _punishment.getType() == PunishmentType.JAIL) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
        } else if (isDead() || isAlikeDead() || getStatus().getHpRatio() < 0.5 || getStatus().getMpRatio() < 0.5) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_HP_OR_MP_IS_BELOW_50_PERCENT;
        } else if (isInDuel()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
        } else if (isInOlympiadMode()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
        } else if (isCursedWeaponEquipped() || getKarma() != 0) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
        } else if (isOperating()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
        } else if (isMounted() || isInBoat()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
        } else if (isFishing()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
        } else if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE)) {
            _noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
        } else {
            return true;
        }

        return false;
    }

    public boolean isNoble() {
        return _isNoble;
    }

    /**
     * Set Noblesse status, and reward with nobles' {@link L2Skill}s.
     *
     * @param isNoble : If true, add {@link L2Skill}s ; otherwise remove them.
     * @param storeInDb : If true, store directly the data in the db.
     */
    public void setNoble(boolean isNoble, boolean storeInDb) {
        if (isNoble) {
            for (final L2Skill skill : SkillTable.getNobleSkills()) {
                addSkill(skill, false);
            }
        } else {
            for (final L2Skill skill : SkillTable.getNobleSkills()) {
                removeSkill(skill.getId(), false);
            }
        }

        _isNoble = isNoble;

        sendSkillList();

        if (storeInDb) {
            PlayerDao.updateNobles(this);
        }
    }

    public void setLvlJoinedAcademy(int lvl) {
        _lvlJoinedAcademy = lvl;
    }

    public int getLvlJoinedAcademy() {
        return _lvlJoinedAcademy;
    }

    public boolean isAcademyMember() {
        return _lvlJoinedAcademy > 0;
    }

    public void setTeam(TeamType team) {
        _team = team;
    }

    public TeamType getTeam() {
        return _team;
    }

    public void setWantsPeace(boolean wantsPeace) {
        _wantsPeace = wantsPeace;
    }

    public boolean wantsPeace() {
        return _wantsPeace;
    }

    public boolean isFishing() {
        return _fishingStance.isUnderFishCombat() || _fishingStance.isLookingForFish();
    }

    public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance) {
        _alliedVarkaKetra = sideAndLvlOfAlliance;
    }

    /**
     * [-5,-1] varka, 0 neutral, [1,5] ketra
     *
     * @return the side faction.
     */
    public int getAllianceWithVarkaKetra() {
        return _alliedVarkaKetra;
    }

    public boolean isAlliedWithVarka() {
        return _alliedVarkaKetra < 0;
    }

    public boolean isAlliedWithKetra() {
        return _alliedVarkaKetra > 0;
    }

    public void sendSkillList() {
        final boolean isWearingFormalWear = isWearingFormalWear();
        final boolean isClanDisabled = getClan() != null && getClan().getReputationScore() < 0;

        final SkillList sl = new SkillList();
        for (final L2Skill skill : getSkills().values()) {
            sl.addSkill(skill.getId(), skill.getLevel(), skill.isPassive(), isWearingFormalWear || (skill.isClanSkill() && isClanDisabled));
        }

        sendPacket(sl);
    }


    public boolean isSubClassActive() {
        return _classIndex > 0;
    }

    public Map<Integer, SubClass> getSubClasses() {
        return _subClasses;
    }

    public int getBaseClass() {
        return _baseClass;
    }

    public int getActiveClass() {
        return _activeClass;
    }

    public int getClassIndex() {
        return _classIndex;
    }

    private void setClassTemplate(int classId) {
        _activeClass = classId;

        // Set the template of the Player
        setTemplate(PlayerData.getInstance().getTemplate(classId));
    }

    /**
     * Changes the character's class based on the given class index. <BR>
     * <BR>
     * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's
     * sub-classes respectively.
     *
     * @param classIndex
     */
    public void setActiveClass(int classIndex) {
        if (!locker.tryLock()) {
            return;
        }

        SubClass subclass = null;
        if (classIndex != 0) {
            subclass = _subClasses.get(classIndex);
            if (subclass == null) {
                return;
            }
        }

        try {
            // Remove active item skills before saving char to database because next time when choosing this class, worn items can be different
            for (final ItemInstance item : getInventory().getAugmentedItems()) {
                if (item != null && item.isEquipped()) {
                    item.getAugmentation().removeBonus(this);
                }
            }

            // abort any kind of cast.
            getCast().stop();

            // Stop casting for any player that may be casting a force buff on this l2pcinstance.
            for (final Creature creature : getKnownType(Creature.class)) {
                if (creature.getFusionSkill() != null && creature.getFusionSkill().getTarget() == this) {
                    creature.getCast().stop();
                }
            }

            store();
            _reuseTimeStamps.clear();

            // clear charges
            _charges.set(0);
            stopChargeTask();

            setClassTemplate((subclass == null) ? getBaseClass() : subclass.getClassId());

            _classIndex = classIndex;

            if (_party != null) {
                _party.recalculateLevel();
            }

            if (_summon instanceof Servitor) {
                _summon.unSummon(this);
            }

            for (final L2Skill skill : getSkills().values()) {
                removeSkill(skill.getId(), false);
            }

            stopAllEffectsExceptThoseThatLastThroughDeath();
            _cubicList.stopCubics(true);

            if (isSubClassActive()) {
                _recipeBook.clear();
            } else {
                _recipeBook.restore();
            }

            _hennaList.restore();

            PlayerDao.restoreSkills(this);
            giveSkills();
            regiveTemporarySkills();

            // Prevents some issues when changing between subclases that shares skills
            getDisabledSkills().clear();

            PlayerDao.restoreEffects(this);
            updateEffectIcons();
            sendPacket(new EtcStatusUpdate(this));

            // If player has quest "Repent Your Sins", remove it
            final QuestState st = _questList.getQuestState("Q422_RepentYourSins");
            if (st != null) {
                st.exitQuest(true);
            }

            int max = getStatus().getMaxHp();
            if (getStatus().getHp() > max) {
                getStatus().setHp(max);
            }

            max = getStatus().getMaxMp();
            if (getStatus().getMp() > max) {
                getStatus().setMp(max);
            }

            max = getStatus().getMaxCp();
            if (getStatus().getCp() > max) {
                getStatus().setCp(max);
            }

            refreshWeightPenalty();
            refreshExpertisePenalty();
            refreshHennaList();
            broadcastUserInfo();

            // Remove shot automation
            disableAutoShotsAll();

            // Discharge any active shots
            final ItemInstance item = getActiveWeaponInstance();
            if (item != null) {
                item.unChargeAllShots();
            }

            _shortcutList.restore();
            sendPacket(new ShortCutInit(this));

            broadcastPacket(new SocialAction(this, 15));
            sendPacket(new SkillCoolTime(this));
        } finally {
            locker.unlock();
        }
    }

    public boolean isLocked() {
        return locker.isLocked();
    }

    public void onPlayerEnter() {
        if (isCursedWeaponEquipped()) {
            CursedWeaponManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).cursedOnLogin();
        }

        // Teleport player if the Seven Signs period isn't the good one, or if the player isn't in a cabal.
        if (isIn7sDungeon() && !isGM()) {
            if (SevenSignsManager.getInstance().isSealValidationPeriod() || SevenSignsManager.getInstance().isCompResultsPeriod()) {
                if (SevenSignsManager.getInstance().getPlayerCabal(getObjectId()) != SevenSignsManager.getInstance().getWinningCabal()) {
                    teleportTo(TeleportType.TOWN);
                    setIsIn7sDungeon(false);
                }
            } else if (SevenSignsManager.getInstance().getPlayerCabal(getObjectId()) == CabalType.NORMAL) {
                teleportTo(TeleportType.TOWN);
                setIsIn7sDungeon(false);
            }
        }

        // Jail task
        _punishment.handle();

        if (isGM()) {
            if (isInvul()) {
                sendMessage("Entering world in Invulnerable mode.");
            }

            if (!getAppearance().isVisible()) {
                sendMessage("Entering world in Invisible mode.");
            }

            if (getBlockList().isBlockingAll()) {
                sendMessage("Entering world in Refusal mode.");
            }
        }

        revalidateZone(true);
        notifyFriends(true);
    }

    @Override
    public void doRevive() {
        super.doRevive();

        stopEffects(EffectType.CHARM_OF_COURAGE);
        sendPacket(new EtcStatusUpdate(this));

        if (isMounted()) {
            startFeed(_mountNpcId);
        }
    }

    public void reviveRequest(Player reviver, L2Skill skill, boolean isPet) {
        if (isReviveRequest()) {
            // Resurrection has already been proposed.
            reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED);
            if (isPet) {
                // A pet cannot be resurrected while it's owner is in the process of resurrecting.
                reviver.sendPacket(SystemMessageId.CANNOT_RES_PET2);
            } else {
                // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
                reviver.sendPacket(SystemMessageId.MASTER_CANNOT_RES);
            }
            return;
        }

        if ((isPet && _summon != null && _summon.isDead()) || (!isPet && isDead())) {
            double revivePower = (isPhoenixBlessed()) ? 100 : Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), reviver);
            ConfirmDlg confirmDlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_S1).addCharName(reviver).addTime(60000);
            dialog = new Dialog(this, confirmDlg, Map.of(
                "revivePower", revivePower,
                "isRevivingPet", isPet,
                "reviver", reviver,
                "isFullRestore", skill.getId() == 1016 && skill.getLevel() == 10
            )).send();
        }
    }

    public void onActionRequest() {
        if (isSpawnProtected()) {
            sendMessage("As you acted, you are no longer under spawn protection.");
            setSpawnProtection(false);
        }
    }

    @Override
    public void onTeleported() {
        super.onTeleported();

        if (Config.PLAYER_SPAWN_PROTECTION > 0) {
            setSpawnProtection(true);
        }

        // Modify the position of the tamed beast if necessary
        if (_tamedBeast != null) {
            _tamedBeast.teleportTo(getPosition(), 0);
        }

        // Modify the position of the pet if necessary
        if (_summon != null) {
            _summon.teleportTo(getPosition(), 0);
        }

        // If under shop mode, cancel it. Leave the Player sat down.
        if (isInStoreMode()) {
            setOperateType(OperateType.NONE);
        }
    }

    @Override
    public void addExpAndSp(long addToExp, int addToSp) {
        getStatus().addExpAndSp(addToExp, addToSp);
    }

    public void addExpAndSp(long addToExp, int addToSp, Map<Creature, RewardInfo> rewards) {
        getStatus().addExpAndSp(addToExp, addToSp, rewards);
    }

    public void removeExpAndSp(long removeExp, int removeSp) {
        getStatus().removeExpAndSp(removeExp, removeSp);
    }

    public void addSp(int addToSp) {
        getStatus().addSp(addToSp);
    }

    public void addExp(long addToExp, Map<Creature, RewardInfo> rewards) {
        getStatus().addExp(addToExp, rewards);
    }

    @Override
    public void reduceCurrentHp(double value, Creature attacker, boolean awake, boolean isDOT, L2Skill skill) {
        if (skill != null) {
            getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
        } else {
            getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
        }
    }

    public synchronized void addBypass(String bypass) {
        if (bypass == null) {
            return;
        }

        _validBypass.add(bypass);
    }

    public synchronized void addBypass2(String bypass) {
        if (bypass == null) {
            return;
        }

        _validBypass2.add(bypass);
    }

    public synchronized boolean validateBypass(String cmd) {
        for (final String bp : _validBypass) {
            if (bp == null) {
                continue;
            }

            if (bp.equals(cmd)) {
                return true;
            }
        }

        for (final String bp : _validBypass2) {
            if (bp == null) {
                continue;
            }

            if (cmd.startsWith(bp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test cases (player drop, trade item) where the item shouldn't be able to manipulate.
     *
     * @param objectId : The item objectId.
     * @return true if it the item can be manipulated, false ovtherwise.
     */
    public ItemInstance validateItemManipulation(int objectId) {
        final ItemInstance item = getInventory().getItemByObjectId(objectId);

        // You don't own the item, or item is null.
        if (item == null || item.getOwnerId() != getObjectId()) {
            return null;
        }

        // Pet whom item you try to manipulate is summoned/mounted.
        if (_summon != null && _summon.getControlItemObjectId() == objectId || _mountObjectId == objectId) {
            return null;
        }

        // Item is under enchant process.
        if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId) {
            return null;
        }

        // Can't trade a cursed weapon.
        if (CursedWeaponManager.getInstance().isCursed(item.getItemId())) {
            return null;
        }

        return item;
    }

    public synchronized void clearBypass() {
        _validBypass.clear();
        _validBypass2.clear();
    }

    /**
     * @return true if the current {@link Player} is linked to a {@link Boat}.
     */
    public boolean isInBoat() {
        return _boat != null;
    }

    /**
     * @return the {@link Boat} linked to the current {@link Player}.
     */
    public Boat getBoat() {
        return _boat;
    }

    /**
     * Set the {@link Boat} for the current {@link Player}.<br>
     * <br>
     * If the parameter is null but Player is registered into a Boat, we delete the passenger from the Boat.
     *
     * @param boat : The Boat to set, or null to clean it.
     */
    public void setBoat(Boat boat) {
        if (boat == null && _boat != null) {
            // Remove passenger out from the Boat.
            _boat.getPassengers().remove(this);

            // Clear the boat position.
            _boatPosition.clean();
        }
        _boat = boat;
    }

    /**
     * @return the {@link SpawnLocation} related to Boat.
     */
    public SpawnLocation getBoatPosition() {
        return _boatPosition;
    }

    public void setCrystallizing(boolean mode) {
        _isCrystallizing = mode;
    }

    public boolean isCrystallizing() {
        return _isCrystallizing;
    }

    /**
     * Manage the delete task of a Player (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from
     * the world...).
     * <ul>
     * <li>If the Player is in observer mode, set its position to its position before entering in observer mode</li>
     * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
     * <li>Stop the HP/MP/CP Regeneration task</li>
     * <li>Cancel Crafting, Attak or Cast</li>
     * <li>Remove the Player from the world</li>
     * <li>Stop Party and Unsummon Pet</li>
     * <li>Update database with items in its inventory and remove them from the world</li>
     * <li>Remove the object from region</li>
     * <li>Close the connection with the client</li>
     * </ul>
     */
    @Override
    public void deleteMe() {
        super.deleteMe();

        cleanup();
        store();
    }

    private synchronized void cleanup() {
        try {
            // Put the online status to false
            setOnlineStatus(false, true);

            // Abort attack, cast and move.
            abortAll(true);

            removeMeFromPartyMatch();

            if (isFlying()) {
                removeSkill(FrequentSkill.WYVERN_BREATH.getSkill().getId(), false);
            }

            // Dismount the player.
            if (isMounted()) {
                dismount();
            }
            // If the Player has a summon, unsummon it.
            else if (_summon != null) {
                _summon.unSummon(this);
            }

            // Stop all scheduled tasks.
            stopChargeTask();

            _punishment.stopTask(true);

            // Stop all timers associated to that Player.
            WaterTaskManager.getInstance().remove(this);
            AttackStanceTaskManager.getInstance().remove(this);
            PvpFlagTaskManager.getInstance().remove(this, false);

            // Cancel the cast of eventual fusion skill users on this target.
            for (final Creature creature : getKnownType(Creature.class)) {
                if (creature.getFusionSkill() != null && creature.getFusionSkill().getTarget() == this) {
                    creature.getCast().stop();
                }
            }

            // Stop signets & toggles effects.
            for (final AbstractEffect effect : getAllEffects()) {
                if (effect.getSkill().isToggle()) {
                    effect.exit();
                    continue;
                }

                switch (effect.getEffectType()) {
                    case SIGNET_GROUND:
                    case SIGNET_EFFECT:
                        effect.exit();
                        break;
                }
            }

            // Remove the Player from the world
            decayMe();

            // If a party is in progress, leave it
            if (_party != null) {
                _party.removePartyMember(this, MessageType.DISCONNECTED);
            }

            // Handle removal from olympiad game
            if (OlympiadManager.getInstance().isRegistered(this) || getOlympiadGameId() != -1) {
                OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
            }

            // set the status for pledge member list to OFFLINE
            if (getClan() != null) {
                final ClanMember clanMember = getClan().getClanMember(getObjectId());
                if (clanMember != null) {
                    clanMember.setPlayerInstance(null);
                }
            }

            // deals with sudden exit in the middle of transaction
            if (getActiveRequester() != null) {
                setActiveRequester(null);
                cancelActiveTrade();
            }

            // If the Player is a GM, remove it from the GM List
            if (isGM()) {
                AdminData.getInstance().deleteGm(this);
            }

            // Check if the Player is in observer mode to set its position to its position before entering in observer mode
            if (isInObserverMode()) {
                setXYZInvisible(_savedLocation);
            }

            // Oust player from boat
            if (_boat != null) {
                _boat.oustPlayer(this, MapRegionData.getInstance().getLocationToTeleport(this, TeleportType.TOWN));
            }

            // Update inventory and remove them from the world
            getInventory().deleteMe();

            // Update warehouse and remove them from the world
            clearWarehouse();

            // Update freight and remove them from the world
            clearFreight();
            clearDepositedFreight();

            if (isCursedWeaponEquipped()) {
                CursedWeaponManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
            }

            if (_clan != null) {
                _clan.broadcastToMembersExcept(this, new PledgeShowMemberListUpdate(this));
            }

            if (isSeated()) {
                final WorldObject object = World.getInstance().getObject(_throneId);
                if (object instanceof StaticObject) {
                    ((StaticObject) object).setBusy(false);
                }
            }

            World.getInstance().removePlayer(this); // force remove in case of crash during teleport

            // friends & blocklist update
            notifyFriends(false);
            getBlockList().playerLogout();
        } catch (final Exception e) {
            log.error("Couldn't disconnect correctly the player.", e);
        }
    }

    public FishingStance getFishingStance() {
        return _fishingStance;
    }

    public int getMountNpcId() {
        return _mountNpcId;
    }

    public int getMountLevel() {
        return _mountLevel;
    }

    public void setMountObjectId(int id) {
        _mountObjectId = id;
    }

    public int getMountObjectId() {
        return _mountObjectId;
    }

    @Override
    public Map<Integer, L2Skill> getSkills() {
        return _skills;
    }

    public Punishment getPunishment() {
        return _punishment;
    }

    public RecipeBook getRecipeBook() {
        return _recipeBook;
    }

    /**
     * @return true if the {@link Player} is jailed, false otherwise.
     */
    public boolean isInJail() {
        return _punishment.getType() == PunishmentType.JAIL;
    }

    /**
     * @return true if the {@link Player} is chat banned, false otherwise.
     */
    public boolean isChatBanned() {
        return _punishment.getType() == PunishmentType.CHAT;
    }

    public int getPowerGrade() {
        return _powerGrade;
    }

    public void setPowerGrade(int power) {
        _powerGrade = power;
    }

    public boolean isCursedWeaponEquipped() {
        return _cursedWeaponEquippedId != 0;
    }

    public void setCursedWeaponEquippedId(int value) {
        _cursedWeaponEquippedId = value;
    }

    public int getCursedWeaponEquippedId() {
        return _cursedWeaponEquippedId;
    }

    public void shortBuffStatusUpdate(int magicId, int level, int time) {
        if (_shortBuffTask != null) {
            _shortBuffTask.cancel(false);
            _shortBuffTask = null;
        }

        _shortBuffTask = ThreadPool.schedule(() ->
        {
            sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
            setShortBuffTaskSkillId(0);
        }, time * 1000L);
        setShortBuffTaskSkillId(magicId);

        sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
    }

    public int getShortBuffTaskSkillId() {
        return _shortBuffTaskSkillId;
    }

    public void setShortBuffTaskSkillId(int id) {
        _shortBuffTaskSkillId = id;
    }

    public int getDeathPenaltyBuffLevel() {
        return _deathPenaltyBuffLevel;
    }

    public void setDeathPenaltyBuffLevel(int level) {
        _deathPenaltyBuffLevel = level;
    }

    /**
     * Check and calculate if a new Death Penalty buff level needs to be added. If Death Penalty already applies, raise
     * its level by 1.
     *
     * @param killer : The {@link Creature} who killed this {@link Player}.
     */
    public void calculateDeathPenaltyBuffLevel(Creature killer) {
        if (_deathPenaltyBuffLevel >= 15) // maximum level reached
        {
            return;
        }

        if ((getKarma() > 0 || Rnd.get(1, 100) <= Config.DEATH_PENALTY_CHANCE) && !(killer instanceof Player) && !(getCharmOfLuck() && (killer == null || killer.isRaidRelated())) && !isPhoenixBlessed() && !(isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE))) {
            if (_deathPenaltyBuffLevel != 0) {
                removeSkill(5076, false);
            }

            _deathPenaltyBuffLevel++;

            addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);

            sendPacket(new EtcStatusUpdate(this));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
        }
    }

    /**
     * Reduce the Death Penalty buff effect from this {@link Player} of 1. If it reaches 0, remove it entirely.
     */
    public void reduceDeathPenaltyBuffLevel() {
        if (_deathPenaltyBuffLevel <= 0) {
            return;
        }

        removeSkill(5076, false);

        _deathPenaltyBuffLevel--;

        if (_deathPenaltyBuffLevel > 0) {
            addSkill(SkillTable.getInstance().getInfo(5076, _deathPenaltyBuffLevel), false);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
        } else {
            sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);
        }

        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Remove the Death Penalty buff effect from this {@link Player}.
     */
    public void removeDeathPenaltyBuffLevel() {
        if (_deathPenaltyBuffLevel <= 0) {
            return;
        }

        removeSkill(5076, false);

        _deathPenaltyBuffLevel = 0;

        sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);
        sendPacket(new EtcStatusUpdate(this));
    }

    public Collection<Timestamp> getReuseTimeStamps() {
        return _reuseTimeStamps.values();
    }

    public Map<Integer, Timestamp> getReuseTimeStamp() {
        return _reuseTimeStamps;
    }

    /**
     * Index according to skill id the current timestamp of use.
     *
     * @param skill
     * @param reuse delay
     */
    @Override
    public void addTimeStamp(L2Skill skill, long reuse) {
        _reuseTimeStamps.put(skill.getReuseHashCode(), new Timestamp(skill, reuse));
    }

    /**
     * Index according to skill this TimeStamp instance for restoration purposes only.
     *
     * @param skill
     * @param reuse
     * @param systime
     */
    public void addTimeStamp(L2Skill skill, long reuse, long systime) {
        _reuseTimeStamps.put(skill.getReuseHashCode(), new Timestamp(skill, reuse, systime));
    }

    @Override
    public Player getActingPlayer() {
        return this;
    }

    @Override
    public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss, boolean parried) {
        // Check if hit is missed
        if (miss) {
            sendPacket(SystemMessageId.MISSED_TARGET);
            return;
        } else if (parried) {
            sendPacket(SystemMessageId.HIT_WAS_PARRIED);
        }

        // Check if hit is critical
        if (pcrit) {
            sendPacket(SystemMessageId.CRITICAL_HIT);
        }
        if (mcrit) {
            sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC);
        }

        if (target.isInvul()) {
            if (target.isParalyzed()) {
                sendPacket(SystemMessageId.OPPONENT_PETRIFIED);
            } else {
                sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED);
            }
        } else {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage));
        }

        if (isInOlympiadMode() && target instanceof Player && ((Player) target).isInOlympiadMode() && ((Player) target).getOlympiadGameId() == getOlympiadGameId()) {
            OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
        }
    }

    public void checkItemRestriction() {
        for (final ItemInstance item : getInventory().getPaperdollItems()) {
            if (item.getItem().checkCondition(this, this, false)) {
                continue;
            }

            useEquippableItem(item, item.isWeapon());
        }
    }

    /**
     * A method used to test player entrance on no landing zone.<br>
     * <br>
     * If a player is mounted on a Wyvern, it launches a dismount task after 5 seconds, and a warning message.
     */
    public void enterOnNoLandingZone() {
        if (getMountType() == 2) {
            if (_dismountTask == null) {
                _dismountTask = ThreadPool.schedule(this::dismount, 5000);
            }

            sendPacket(SystemMessageId.AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_WYVERN);
        }
    }

    /**
     * A method used to test player leave on no landing zone.<br>
     * <br>
     * If a player is mounted on a Wyvern, it cancels the dismount task, if existing.
     */
    public void exitOnNoLandingZone() {
        if (getMountType() == 2 && _dismountTask != null) {
            _dismountTask.cancel(true);
            _dismountTask = null;
        }
    }

    /**
     * Remove player from BossZones (used on char logout/exit)
     */
    public void removeFromBossZone() {
        for (final BossZone zone : ZoneManager.getInstance().getAllZones(BossZone.class)) {
            zone.removePlayer(this);
        }
    }

    /**
     * @return the number of charges this Player got.
     */
    public int getCharges() {
        return _charges.get();
    }

    public void increaseCharges(int count, int max) {
        if (_charges.get() >= max) {
            sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
            return;
        }

        restartChargeTask();

        if (_charges.addAndGet(count) >= max) {
            _charges.set(max);
            sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
        } else {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(_charges.get()));
        }

        sendPacket(new EtcStatusUpdate(this));
    }

    public boolean decreaseCharges(int count) {
        if (_charges.get() < count) {
            return false;
        }

        if (_charges.addAndGet(-count) == 0) {
            stopChargeTask();
        } else {
            restartChargeTask();
        }

        sendPacket(new EtcStatusUpdate(this));
        return true;
    }

    public void clearCharges() {
        if (_charges.get() > 0) {
            _charges.set(0);
            sendPacket(new EtcStatusUpdate(this));
        }
    }

    /**
     * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
     */
    private void restartChargeTask() {
        if (_chargeTask != null) {
            _chargeTask.cancel(false);
            _chargeTask = null;
        }

        _chargeTask = ThreadPool.schedule(this::clearCharges, 600000);
    }

    /**
     * Stops the Charges Clearing Task.
     */
    public void stopChargeTask() {
        if (_chargeTask != null) {
            _chargeTask.cancel(false);
            _chargeTask = null;
        }
    }

    public int getMailPosition() {
        return _mailPosition;
    }

    public void setMailPosition(int mailPosition) {
        _mailPosition = mailPosition;
    }

    /**
     * @param z
     * @return true if character falling now On the start of fall return false for correct coord sync !
     */
    public boolean isFalling(int z) {
        if (isDead() || getMove().getMoveType() != MoveType.GROUND) {
            return false;
        }

        if (System.currentTimeMillis() < _fallingTimestamp) {
            return true;
        }

        final int deltaZ = getZ() - z;
        if (deltaZ <= getBaseTemplate().getSafeFallHeight(getAppearance().getSex())) {
            return false;
        }

        final int damage = (int) Formulas.calcFallDam(this, deltaZ);
        if (damage > 0) {
            if (getStatus().getHp() - damage < 0.5) {
                setDieReason(DieReason.FALL);
            }
            reduceCurrentHp(damage, null, false, true, null);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
        }

        setFalling();

        return false;
    }

    /**
     * Set falling timestamp
     */
    public void setFalling() {
        _fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
    }

    public boolean isAllowedToEnchantSkills() {
        if (isLocked()) {
            return false;
        }

        if (AttackStanceTaskManager.getInstance().isInAttackStance(this)) {
            return false;
        }

        if (getCast().isCastingNow()) {
            return false;
        }

        return !isInBoat();
    }

    public List<Integer> getFriendList() {
        return _friendList;
    }

    public void selectFriend(Integer friendId) {
        if (!_selectedFriendList.contains(friendId)) {
            _selectedFriendList.add(friendId);
        }
    }

    public void deselectFriend(Integer friendId) {
        _selectedFriendList.remove(friendId);
    }

    public List<Integer> getSelectedFriendList() {
        return _selectedFriendList;
    }

    void restoreFriendList() {
        _friendList.clear();

        try (Connection con = ConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 0")) {
            ps.setInt(1, getObjectId());

            try (ResultSet rset = ps.executeQuery()) {
                while (rset.next()) {
                    final int friendId = rset.getInt("friend_id");
                    if (friendId == getObjectId()) {
                        continue;
                    }

                    _friendList.add(friendId);
                }
            }
        } catch (final Exception e) {
            log.error("Couldn't restore {}'s friendlist.", getName(), e);
        }
    }

    private void notifyFriends(boolean isOnline) {
        for (final int id : _friendList) {
            final Player friend = World.getInstance().getPlayer(id);
            if (friend != null) {
                friend.sendPacket(new L2FriendStatus(this, isOnline));

                if (isOnline) {
                    friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addCharName(this));
                }
            }
        }
    }

    public void selectBlock(Integer friendId) {
        if (!_selectedBlocksList.contains(friendId)) {
            _selectedBlocksList.add(friendId);
        }
    }

    public void deselectBlock(Integer friendId) {
        _selectedBlocksList.remove(friendId);
    }

    public List<Integer> getSelectedBlocksList() {
        return _selectedBlocksList;
    }

    @Override
    public void broadcastRelationsChanges() {
        for (final Player player : getKnownType(Player.class)) {
            final int relation = getRelation(player);
            final boolean isAutoAttackable = isAttackableWithoutForceBy(player);

            player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
            if (_summon != null) {
                player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
            }
        }
    }

    @Override
    public void sendInfo(Player player) {
        if (_boat != null) {
            getPosition().set(_boat.getPosition());
        }

        if (getPolymorphTemplate() != null) {
            player.sendPacket(new AbstractNpcInfo.PcMorphInfo(this, getPolymorphTemplate()));
        } else {
            player.sendPacket(new CharInfo(this));

            if (isSeated()) {
                final WorldObject object = World.getInstance().getObject(_throneId);
                if (object instanceof StaticObject) {
                    player.sendPacket(new ChairSit(getObjectId(), ((StaticObject) object).getStaticObjectId()));
                }
            }
        }

        int relation = getRelation(player);
        boolean isAutoAttackable = isAttackableWithoutForceBy(player);

        player.sendPacket(new RelationChanged(this, relation, isAutoAttackable));
        if (_summon != null) {
            player.sendPacket(new RelationChanged(_summon, relation, isAutoAttackable));
        }

        relation = player.getRelation(this);
        isAutoAttackable = player.isAttackableWithoutForceBy(this);

        sendPacket(new RelationChanged(player, relation, isAutoAttackable));
        if (player.getSummon() != null) {
            sendPacket(new RelationChanged(player.getSummon(), relation, isAutoAttackable));
        }

        if (_boat != null) {
            player.sendPacket(new GetOnVehicle(getObjectId(), _boat.getObjectId(), getBoatPosition()));
        }

        switch (getOperateType()) {
            case SELL:
            case PACKAGE_SELL:
                player.sendPacket(new PrivateStoreMsgSell(this));
                break;

            case BUY:
                player.sendPacket(new PrivateStoreMsgBuy(this));
                break;

            case MANUFACTURE:
                player.sendPacket(new RecipeShopMsg(this));
                break;
        }
    }

    @Override
    public double getCollisionRadius() {
        return (isMounted()) ? NpcData.getInstance().getTemplate(_mountNpcId).getCollisionRadius() : getBaseTemplate().getCollisionRadiusBySex(getAppearance().getSex());
    }

    @Override
    public double getCollisionHeight() {
        return (isMounted()) ? NpcData.getInstance().getTemplate(_mountNpcId).getCollisionHeight() : getBaseTemplate().getCollisionHeightBySex(getAppearance().getSex());
    }

    @Override
    public boolean polymorph(int npcId) {
        if (super.polymorph(npcId)) {
            sendPacket(new UserInfo(this));
            return true;
        }
        return false;
    }

    @Override
    public void unpolymorph() {
        super.unpolymorph();
        sendPacket(new UserInfo(this));
    }

    @Override
    public void addKnownObject(WorldObject object) {
        sendInfoFrom(object);
    }

    @Override
    public void removeKnownObject(WorldObject object) {
        super.removeKnownObject(object);

        // send Server-Client Packet DeleteObject to the Player
        sendPacket(new DeleteObject(object, (object instanceof Player) && ((Player) object).isSeated()));
    }

    public void refreshInfos() {
        for (final WorldObject object : getKnownType(WorldObject.class)) {
            if (object instanceof Player && ((Player) object).isInObserverMode()) {
                continue;
            }

            sendInfoFrom(object);
        }
    }

    /**
     * teleToLocation method without Dimensional Rift check.
     *
     * @param loc : The Location to teleport.
     */
    public void teleToLocation(Location loc) {
        super.teleportTo(loc, 0);
    }

    @Override
    public void teleportTo(Location loc, int randomOffset) {
        if (DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) {
            sendMessage("You have been sent to the waiting room.");

            if (isInParty() && getParty().isInDimensionalRift()) {
                getParty().getDimensionalRift().usedTeleport(this);
            }

            loc = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportLoc();
        }
        super.teleportTo(loc, randomOffset);
    }

    private void sendInfoFrom(WorldObject object) {
        // Send object info to player.
        object.sendInfo(this);

        if (object instanceof Creature obj) {
            // Send the state of the Creature to the Player.
            if (obj.hasAI()) {
                obj.getAI().describeStateToPlayer(this);
            }
        }
    }

    /**
     * @return true if this {@link Player} is currently wearing a Formal Wear.
     */
    public boolean isWearingFormalWear() {
        final ItemInstance formal = getInventory().getItemFrom(Paperdoll.CHEST);
        return formal != null && formal.getItem().getBodyPart() == Item.SLOT_ALLDRESS;
    }

    public void startFakeDeath() {
        _isFakeDeath = true;
        _isSittingNow = true;
        _isStanding = false;

        ThreadPool.schedule(() ->
        {
            _isSittingNow = false;
            _isSitting = true;

            getAI().notifyEvent(AiEventType.SAT_DOWN, null, null);

        }, (int) (3000 / getStatus().getMovementSpeedMultiplier()));

        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
    }

    public void stopFakeDeath(boolean removeEffects) {
        if (removeEffects) {
            stopEffects(EffectType.FAKE_DEATH);
        }

        // Start the grace period for this character (grace from mobs only)!
        setRecentFakeDeath();

        _isStandingNow = true;
        _isSitting = false;

        ThreadPool.schedule(() ->
        {
            _isStandingNow = false;
            _isStanding = true;
            _isFakeDeath = false;

            getAI().notifyEvent(AiEventType.STOOD_UP, null, null);
        }, (int) (2500 / getStatus().getMovementSpeedMultiplier()));

        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
        broadcastPacket(new Revive(this));
    }

    @Override
    public void onInteract(Player player) {
        switch (getOperateType()) {
            case SELL:
            case PACKAGE_SELL:
                player.sendPacket(new PrivateStoreListSell(player, this));
                break;

            case BUY:
                player.sendPacket(new PrivateStoreListBuy(player, this));
                break;

            case MANUFACTURE:
                player.sendPacket(new RecipeShopSellList(player, this));
                break;
        }
    }

    /**
     * Added to other GMs, test also this {@link Player} instance. If GM, set it.
     */
    @Override
    public List<Player> getSurroundingGMs() {
        final List<Player> gms = super.getSurroundingGMs();
        if (isGM()) {
            gms.add(this);
        }

        return gms;
    }

    public boolean hasDialog(SystemMessageId messageId) {
        return dialog != null && dialog.isMessage(messageId);
    }

    public boolean hasDialog(int id) {
        return dialog != null && dialog.isMessage(id);
    }

    public boolean isReviveRequest() {
        return hasDialog(SystemMessageId.RESSURECTION_REQUEST_BY_S1)
            || hasDialog(SystemMessageId.DO_YOU_WANT_TO_BE_RESTORED);
    }

    @Override
    protected void onHit(OnHit event) {
        super.onHit(event);

        Creature attacker = event.getAttacker();
        Creature target = event.getTarget();
        CreatureAttack.HitHolder hit = event.getHit();

        if (target == this) {
            ItemInstance armor = getInventory().getRandomEquippedItem(0);
            if (hit.block.isSuccess()) {
                armor = getSecondaryWeaponInstance();
            }

            if (armor != null) {
                Optional.ofNullable(armor.getDurabilityModule()).ifPresent(e -> e.fractureArmor(this, null, Default.Context.builder()
                    .block(hit.block)
                    .isCritical(hit.isCritical)
                    .value(hit.damage)
                    .isMissed(hit.isMissed)
                    .build()));
            }
        } else if (attacker == this) {
            ItemInstance wpn = getActiveWeaponInstance();
            if (wpn != null) {
                Optional.ofNullable(wpn.getDurabilityModule()).ifPresent(e -> e.fractureWeapon(this, target, null, Default.Context.builder()
                    .block(hit.block)
                    .isCritical(hit.isCritical)
                    .value(hit.damage)
                    .isMissed(hit.isMissed)
                    .build()));
            }
        }
    }

    @Override
    protected void onSkillHit(OnSkillHit event) {
        super.onSkillHit(event);

        L2Skill skill = event.getSkill();
        Creature caster = event.getCaster();
        Creature target = event.getTarget();
        Default.Context context = event.getContext();

        if (target == this) {
            ItemInstance armor = getInventory().getRandomEquippedItem(skill.isMagic() ? 1 : 0);
            if (context.getBlock().isSuccess()) {
                armor = getSecondaryWeaponInstance();
            }

            if (armor != null) {
                Optional.ofNullable(armor.getDurabilityModule()).ifPresent(e -> e.fractureArmor(this, skill, context));
            }
        } else if (caster == this) {
            ItemInstance wpn = getActiveWeaponInstance();
            if (wpn != null) {
                Optional.ofNullable(wpn.getDurabilityModule()).ifPresent(e -> e.fractureWeapon(this, target, skill, context));
            }

            if (!target.isDead()) {
                skill.getSkillType().rewardSp(this, target, skill, context.getValue());
            }
        }
    }

    private void onValidatePosition(OnValidatePosition event) {
    }

    private void onQuestAccept(OnQuestAccept event) {
    }

    @Override
    protected void onDie(OnDie event) {
        _inventory.hardcoreDropItems(event.getReason() == DieReason.MORTAL_COMBAT);
    }

    private void onDayCycleChange(OnDayCycleChange event) {
        DayCycle current = event.getCurrent();
        DayCycle previous = event.getPrevious();
        L2Skill skill = SkillTable.getInstance().getInfo(L2Skill.SKILL_SHADOW_SENSE, 1);
        boolean hasSkill = skill != null && hasSkill(L2Skill.SKILL_SHADOW_SENSE);
        if (current == DayCycle.NIGHT) {
            // Shadow Sense skill is set and player has Shadow Sense skill, activate/deactivate its effect.
            if (hasSkill) {
                // Remove and add Shadow Sense to activate/deactivate effect.
                removeSkill(L2Skill.SKILL_SHADOW_SENSE, false);
                addSkill(skill, false);
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NIGHT_S1_EFFECT_APPLIES).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
            }
        } else if (previous == DayCycle.NIGHT) {
            if (hasSkill) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
            }
        }

        sendPacket(SystemMessageId.PLAYING_FOR_LONG_TIME);

        // synchronize for all players current game time
        sendPacket(new ClientSetTime());
    }

    @Override
    public WeaponType getAttackType() {
        if (attackType != null) {
            return attackType;
        }

        if (getActiveWeaponInstance() != null) {
            return (WeaponType) getActiveWeaponInstance().getItemType();
        }

        return null;
    }

    public void acceptTwoHandGrip() {
        ItemInstance itemFrom = getInventory().getItemFrom(Paperdoll.RHAND);
        if (itemFrom == null) {
            sendMessage("Вы должны экипировать одноручное оружие меч/топор/молот, чтобы взять его двуручным хватом.", SystemMessageColor.RED_LIGHT);
            return;
        }

        ItemInstance itemByObjectId = getInventory().getItemByObjectId(itemFrom.getObjectId());
        if (itemByObjectId == null) {
            sendMessage("Вы должны экипировать одноручное оружие меч/топор/молот, чтобы взять его двуручным хватом.", SystemMessageColor.RED_LIGHT);
            return;
        }

        ItemInstance leftHand = getInventory().getItemFrom(Paperdoll.LHAND);
        if (leftHand != null) {
            this.leftHand = getInventory().unequipItemInSlot(leftHand.getSlot());
            InventoryUpdate update = new InventoryUpdate();
            update.addModifiedItem(leftHand);
            sendPacket(update);
        }

        getInventory().unequipItemInSlot(itemByObjectId.getSlot());
        ThreadPool.schedule(() -> {
            twoHandGrip.set(true);
            getInventory().equipItem(itemByObjectId);
            setAttackType(WeaponType.valueOf("BIG" + ((WeaponType) itemByObjectId.getItemType()).name()));
            sendMessage("Вы плотно держите оружие двумя руками.", SystemMessageColor.GREEN_LIGHT);
        }, 33);
    }

    public void cancelTwoHandGrip() {
        ItemInstance itemInstance = getInventory().unequipItemInBodySlot(Item.SLOT_R_HAND);
        if (itemInstance != null) {
            ItemInstance itemByObjectId = getInventory().getItemByObjectId(itemInstance.getObjectId());
            ThreadPool.schedule(() -> {
                twoHandGrip.set(false);
                getInventory().equipItem(itemByObjectId);
                if (leftHand != null) {
                    getInventory().equipItem(leftHand);
                    InventoryUpdate update = new InventoryUpdate();
                    update.addModifiedItem(leftHand);
                    sendPacket(update);
                    leftHand = null;
                }
                setAttackType(null);
                sendMessage("Вы снова держите оружие одной рукой.", SystemMessageColor.GREEN_LIGHT);
            }, 33);
        }
    }
}