package net.sf.l2j.gameserver.enums.actors;

import net.sf.l2j.gameserver.data.manager.CoupleManager;
import net.sf.l2j.gameserver.events.OnQuestAccept;
import net.sf.l2j.gameserver.model.Dialog;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.WeddingManagerNpc;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.instance.modules.DurabilityModule;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.handlers.SummonFriend;

import java.util.List;

/**
 * @author finfan
 */
public enum DialogAnswerType {

    REVIVE_REQUEST {
        @Override
        public void onAnswer(Player target, int answer) {
            Dialog dialog = target.getDialog();
            double revivePower = dialog.findAndGet("revivePower");
            boolean isRevivingPet = dialog.findAndGet("isRevivingPet");

            if (target.isReviveRequest()
                || (!target.isDead() && !isRevivingPet)
                || (isRevivingPet && target.getSummon() != null && !target.getSummon().isDead())) {
                return;
            }

            if (answer == 0) {
                if (target.isPhoenixBlessed()) {
                    target.stopPhoenixBlessing(null);
                }
            } else if (answer == 1) {
                if (!isRevivingPet) {
                    if (revivePower != 0) {
                        target.doRevive(revivePower);
                    } else {
                        target.doRevive();
                    }
                } else if (target.getSummon() != null) {
                    if (revivePower != 0) {
                        target.getSummon().doRevive(revivePower);
                    } else {
                        target.getSummon().doRevive();
                    }
                }
            }
        }
    },
    TELEPORT_REQUEST {
        @Override
        public void onAnswer(Player receiver, int answer) {
            Dialog dialog = receiver.getDialog();
            int requesterId = dialog.getPacket().getRequesterId();
            SummonFriend summonFriend = dialog.findAndGet("skill");
            if (requesterId == 0) {
                return;
            }

            Player requester = World.getInstance().getPlayer(requesterId);
            if (requester == null) {
                return;
            }

            if (answer == 1 && requester.getObjectId() == requesterId) {
                SummonFriend.teleportTo(receiver, requester, summonFriend);
            }
        }
    },
    WEDDING_REQUEST {
        @Override
        public void onAnswer(Player receiver, int answer) {
            Dialog dialog = receiver.getDialog();
            int requesterId = dialog.getPacket().getRequesterId();
            if (requesterId == 0) {
                return;
            }

            final Player requester = World.getInstance().getPlayer(requesterId);
            if (requester != null) {
                if (answer == 1) {
                    // Create the couple
                    CoupleManager.getInstance().addCouple(requester, receiver);

                    // Then "finish the job"
                    WeddingManagerNpc.justMarried(requester, receiver);
                } else {
                    receiver.sendMessage("You declined your partner's marriage request.");
                    requester.sendMessage("Your partner declined your marriage request.");
                }
                receiver.setUnderMarryRequest(false);
            }
            receiver.setUnderMarryRequest(false);
        }
    },
    OPEN_GATE {
        @Override
        public void onAnswer(Player player, int answer) {
            Dialog dialog = player.getDialog();
            Door door = dialog.findAndGet("door");
            if (door == null) {
                return;
            }

            if (answer == 1) {
                if (player.getTarget() == door) {
                    door.openMe();
                }
            } else {
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
    },
    CLOSE_GATE {
        @Override
        public void onAnswer(Player player, int answer) {
            Dialog dialog = player.getDialog();
            Door door = dialog.findAndGet("door");
            if (door == null) {
                return;
            }

            if (answer == 1) {
                if (player.getTarget() == door) {
                    door.closeMe();
                }
            } else {
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
    },
    REPAIR_ALL {
        @Override
        public void onAnswer(Player player, int answer) {
            if (answer == 1) {
                Dialog dialog = player.getDialog();
                Integer totalPrice = dialog.findAndGet("price");
                List<ItemInstance> items = dialog.findAndGet("items");
                if (items.isEmpty()) {
                    return;
                }

                if (totalPrice == null) {
                    player.setActiveEnchantItem(null);
                    player.sendPacket(ActionFailed.STATIC_PACKET);
                    return;
                }

                synchronized (items) {
                    if (!player.reduceAdena("RepairAll", totalPrice, null, true)) {
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                    } else {
                        items.forEach(item -> item.getModule(DurabilityModule.class).repair(player));
                        player.sendPacket(SystemMessageId.ITEMS_WAS_REPAIRED);
                    }
                }

                player.setActiveEnchantItem(null);
                player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
                player.setActiveEnchantItem(null);
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
    },
    REPAIR_SINGLE {
        @Override
        public void onAnswer(Player player, int answer) {
            if (answer == 1) {
                final Dialog dialog = player.getDialog();
                final Integer price = dialog.findAndGet("price");
                final ItemInstance item = dialog.findAndGet("item");
                if (price == null || item == null) {
                    player.setActiveEnchantItem(null);
                    player.sendPacket(EnchantResult.CANCELLED);
                    return;
                }

                synchronized (item) {
                    if (player.reduceAdena("RepairSingleItem", price, null, true)) {
                        item.getModule(DurabilityModule.class).repair(player);
                        player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ITEM_REPAIRED).addItemName(item));
                    } else {
                        player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
                    }
                }

                player.setActiveEnchantItem(null);
                player.sendPacket(EnchantResult.SUCCESS);
            } else {
                player.setActiveEnchantItem(null);
                player.sendPacket(EnchantResult.CANCELLED);
            }
        }
    },
    QUEST_SHARE {
        @Override
        public void onAnswer(Player player, int answer) {
            if (answer == 1) {
                Dialog dialog = player.getDialog();
                if (dialog == null) {
                    return;
                }

                // Check player being overweight.
                if (player.getWeightPenalty().ordinal() > 2 || player.getStatus().isOverburden()) {
                    player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
                    return;
                }

                Quest quest = dialog.findAndGet("quest");
                String event = dialog.findAndGet("event");
                Npc npc = dialog.findAndGet("npc");
                if (player.getQuestList().getQuestState(quest.getName()) == null) {
                    // Check available quest slot.
                    if (player.getQuestList().getAllQuests(false).size() >= 25) {
                        final NpcHtmlMessage html = new NpcHtmlMessage(0);
                        html.setHtml(Quest.getTooMuchQuestsMsg());
                        player.sendPacket(html);
                        player.sendPacket(ActionFailed.STATIC_PACKET);
                        return;
                    }

                    // Create new state.
                    quest.newQuestState(player);
                    quest.onAdvEvent(event, npc, player);
                    player.getEventListener().notify(new OnQuestAccept(player, npc, quest, event));
                }
            } else {
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
    },
    ;

    public abstract void onAnswer(Player player, int answer);
}
