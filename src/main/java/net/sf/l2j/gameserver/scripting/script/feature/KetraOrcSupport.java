package net.sf.l2j.gameserver.scripting.script.feature;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.xml.TeleportData;
import net.sf.l2j.gameserver.enums.TeleportType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.WarehouseWithdrawList;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * This script supports :
 * <ul>
 * <li>Ketra Orc Village functions</li>
 * <li>Quests failures && alliance downgrade if you kill an allied mob.</li>
 * <li>Petrification effect in case an allied player helps a neutral or enemy.</li>
 * </ul>
 */
public class KetraOrcSupport extends Quest {
    private static final int KADUN = 31370; // Hierarch
    private static final int WAHKAN = 31371; // Messenger
    private static final int ASEFA = 31372; // Soul Guide
    private static final int ATAN = 31373; // Grocer
    private static final int JAFF = 31374; // Warehouse Keeper
    private static final int JUMARA = 31375; // Trader
    private static final int KURFA = 31376; // Gate Keeper

    private static final int HORN = 7186;

    private static final int[] KETRAS =
        {
            21324,
            21325,
            21327,
            21328,
            21329,
            21331,
            21332,
            21334,
            21335,
            21336,
            21338,
            21339,
            21340,
            21342,
            21343,
            21344,
            21345,
            21346,
            21347,
            21348,
            21349
        };

    private static final IntIntHolder[] BUFFS =
        {
            new IntIntHolder(4359, 2), // Focus: Requires 2 Buffalo Horns
            new IntIntHolder(4360, 2), // Death Whisper: Requires 2 Buffalo Horns
            new IntIntHolder(4345, 3), // Might: Requires 3 Buffalo Horns
            new IntIntHolder(4355, 3), // Acumen: Requires 3 Buffalo Horns
            new IntIntHolder(4352, 3), // Berserker: Requires 3 Buffalo Horns
            new IntIntHolder(4354, 3), // Vampiric Rage: Requires 3 Buffalo Horns
            new IntIntHolder(4356, 6), // Empower: Requires 6 Buffalo Horns
            new IntIntHolder(4357, 6) // Haste: Requires 6 Buffalo Horns
        };

    /**
     * Names of missions which will be automatically dropped if the alliance is broken.
     */
    private static final String[] KETRA_QUESTS =
        {
            "Q605_AllianceWithKetraOrcs",
            "Q606_WarWithVarkaSilenos",
            "Q607_ProveYourCourage",
            "Q608_SlayTheEnemyCommander",
            "Q609_MagicalPowerOfWater_Part1",
            "Q610_MagicalPowerOfWater_Part2"
        };

    public KetraOrcSupport() {
        super(-1, "feature");

        addFirstTalkId(KADUN, WAHKAN, ASEFA, ATAN, JAFF, JUMARA, KURFA);
        addTalkId(ASEFA, JAFF, KURFA);

        // Verify if the killer didn't kill an allied mob. Test his party aswell.
        addKillId(KETRAS);

        // Verify if an allied is healing/buff an enemy. Petrify him if it's the case.
        addSkillSeeId(KETRAS);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        String htmltext = getNoQuestMsg();

        if (StringUtil.isDigit(event)) {
            final IntIntHolder buff = BUFFS[Integer.parseInt(event)];
            if (player.getInventory().getItemCount(HORN) >= buff.getValue()) {
                htmltext = "31372-4.htm";
                takeItems(player, HORN, buff.getValue());
                npc.getAI().tryToCast(player, buff.getId(), 1);
                npc.getStatus().setMaxHpMp();
            }
        } else if (event.equals("Withdraw")) {
            if (player.getWarehouse().getSize() == 0) {
                htmltext = "31374-0.htm";
            } else {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                player.setActiveWarehouse(player.getWarehouse());
                player.sendPacket(new WarehouseWithdrawList(player, 1));
            }
        } else if (event.equals("Teleport")) {
            switch (player.getAllianceWithVarkaKetra()) {
                case 4:
                    TeleportData.getInstance().showTeleportList(player, npc, TeleportType.STANDARD);
                    return null;

                case 5:
                    TeleportData.getInstance().showTeleportList(player, npc, TeleportType.ALLY);
                    return null;
            }
        }

        return htmltext;
    }

    @Override
    public String onFirstTalk(Npc npc, Player player) {
        String htmltext = getNoQuestMsg();

        final int allianceLevel = player.getAllianceWithVarkaKetra();

        switch (npc.getNpcId()) {
            case KADUN:
                if (allianceLevel > 0) {
                    htmltext = "31370-friend.htm";
                } else {
                    htmltext = "31370-no.htm";
                }
                break;

            case WAHKAN:
                if (allianceLevel > 0) {
                    htmltext = "31371-friend.htm";
                } else {
                    htmltext = "31371-no.htm";
                }
                break;

            case ASEFA:
                if (allianceLevel < 1) {
                    htmltext = "31372-3.htm";
                } else if (allianceLevel < 3 && allianceLevel > 0) {
                    htmltext = "31372-1.htm";
                } else if (allianceLevel > 2) {
                    if (player.getInventory().hasItems(HORN)) {
                        htmltext = "31372-4.htm";
                    } else {
                        htmltext = "31372-2.htm";
                    }
                }
                break;

            case ATAN:
                if (player.getKarma() >= 1) {
                    htmltext = "31373-pk.htm";
                } else if (allianceLevel <= 0) {
                    htmltext = "31373-no.htm";
                } else if (allianceLevel == 1 || allianceLevel == 2) {
                    htmltext = "31373-1.htm";
                } else {
                    htmltext = "31373-2.htm";
                }
                break;

            case JAFF:
                switch (allianceLevel) {
                    case 1:
                        htmltext = "31374-1.htm";
                        break;
                    case 2:
                    case 3:
                        htmltext = "31374-2.htm";
                        break;
                    default:
                        if (allianceLevel <= 0) {
                            htmltext = "31374-no.htm";
                        } else if (player.getWarehouse().getSize() == 0) {
                            htmltext = "31374-3.htm";
                        } else {
                            htmltext = "31374-4.htm";
                        }
                        break;
                }
                break;

            case JUMARA:
                switch (allianceLevel) {
                    case 2:
                        htmltext = "31375-1.htm";
                        break;
                    case 3:
                    case 4:
                        htmltext = "31375-2.htm";
                        break;
                    case 5:
                        htmltext = "31375-3.htm";
                        break;
                    default:
                        htmltext = "31375-no.htm";
                        break;
                }
                break;

            case KURFA:
                if (allianceLevel <= 0) {
                    htmltext = "31376-no.htm";
                } else if (allianceLevel > 0 && allianceLevel < 4) {
                    htmltext = "31376-1.htm";
                } else if (allianceLevel == 4) {
                    htmltext = "31376-2.htm";
                } else {
                    htmltext = "31376-3.htm";
                }
                break;
        }

        return htmltext;
    }

    @Override
    public String onKill(Npc npc, Creature killer) {
        final Player player = killer.getActingPlayer();
        if (player != null) {
            final Party party = player.getParty();
            if (party != null) {
                for (Player partyMember : party.getMembers()) {
                    testKetraDemote(partyMember);
                }
            } else {
                testKetraDemote(player);
            }
        }
        return null;
    }

    @Override
    public String onSkillSee(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet) {
        // Caster is an allied.
        if (caster.isAlliedWithKetra()) {
            // Caster's skill is a positive effect ? Go further.
            switch (skill.getSkillType()) {
                case BUFF:
                case HEAL:
                case HEAL_PERCENT:
                case HEAL_STATIC:
                case BALANCE_LIFE:
                case HOT:
                    for (Creature target : targets) {
                        // Target isn't a summon nor a player, is dead or is current caster, we drop check.
                        if (!(target instanceof Playable) || target.isDead() || target == caster) {
                            continue;
                        }

                        // Retrieve the player behind that target.
                        final Player player = target.getActingPlayer();

                        // If player is neutral or enemy, go further.
                        if (!(player.isAlliedWithKetra())) {
                            // If the NPC got that player registered in aggro list, go further.
                            if (((Attackable) npc).getAggroList().containsKey(player)) {
                                // Save current target for future use.
                                final WorldObject oldTarget = npc.getTarget();

                                // Curse the heretic or his pet.
                                npc.getAI().tryToCast((isPet && player.getSummon() != null) ? caster.getSummon() : caster, FrequentSkill.VARKA_KETRA_PETRIFICATION.getSkill());

                                // Revert to old target && drop the loop.
                                npc.setTarget(oldTarget);
                                break;
                            }
                        }
                    }
                    break;
            }
        }

        // Continue normal behavior.
        return super.onSkillSee(npc, caster, skill, targets, isPet);
    }

    /**
     * That method drops current alliance and retrograde badge.<BR> If any Varka quest is in progress, it stops the
     * quest (and drop all related qItems) :
     *
     * @param player The player to check.
     */
    private static void testKetraDemote(Player player) {
        if (player.isAlliedWithKetra()) {
            // Drop the alliance (old friends become aggro).
            player.setAllianceWithVarkaKetra(0);

            final PcInventory inventory = player.getInventory();

            // Drop by 1 the level of that alliance (symbolized by a quest item).
            for (int i = 7215; i >= 7211; i--) {
                ItemInstance item = inventory.getItemByItemId(i);
                if (item != null) {
                    // Destroy the badge.
                    player.destroyItemByItemId("Quest", i, item.getCount(), player, true);

                    // Badge lvl 1 ; no addition of badge of lower level.
                    if (i != 7211) {
                        player.addItem("Quest", i - 1, 1, player, true);
                    }

                    break;
                }
            }

            for (String mission : KETRA_QUESTS) {
                QuestState pst = player.getQuestList().getQuestState(mission);
                if (pst != null) {
                    pst.exitQuest(true);
                }
            }
        }
    }
}