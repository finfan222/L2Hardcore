package net.sf.l2j.gameserver.network;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.HexUtil;
import net.sf.l2j.commons.mmocore.IClientFactory;
import net.sf.l2j.commons.mmocore.IMMOExecutor;
import net.sf.l2j.commons.mmocore.IPacketHandler;
import net.sf.l2j.commons.mmocore.MMOConnection;
import net.sf.l2j.commons.mmocore.ReceivablePacket;
import net.sf.l2j.gameserver.network.GameClient.GameClientState;
import net.sf.l2j.gameserver.network.clientpackets.Action;
import net.sf.l2j.gameserver.network.clientpackets.AddTradeItem;
import net.sf.l2j.gameserver.network.clientpackets.AllyDismiss;
import net.sf.l2j.gameserver.network.clientpackets.AllyLeave;
import net.sf.l2j.gameserver.network.clientpackets.AnswerJoinPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.AnswerTradeRequest;
import net.sf.l2j.gameserver.network.clientpackets.Appearing;
import net.sf.l2j.gameserver.network.clientpackets.AttackRequest;
import net.sf.l2j.gameserver.network.clientpackets.AuthLogin;
import net.sf.l2j.gameserver.network.clientpackets.CannotMoveAnymore;
import net.sf.l2j.gameserver.network.clientpackets.CharacterRestore;
import net.sf.l2j.gameserver.network.clientpackets.DlgAnswer;
import net.sf.l2j.gameserver.network.clientpackets.DummyPacket;
import net.sf.l2j.gameserver.network.clientpackets.EnterWorld;
import net.sf.l2j.gameserver.network.clientpackets.FinishRotating;
import net.sf.l2j.gameserver.network.clientpackets.GameGuardReply;
import net.sf.l2j.gameserver.network.clientpackets.Logout;
import net.sf.l2j.gameserver.network.clientpackets.MoveBackwardToLocation;
import net.sf.l2j.gameserver.network.clientpackets.MultiSellChoose;
import net.sf.l2j.gameserver.network.clientpackets.ObserverReturn;
import net.sf.l2j.gameserver.network.clientpackets.PetitionVote;
import net.sf.l2j.gameserver.network.clientpackets.RequestAcquireSkill;
import net.sf.l2j.gameserver.network.clientpackets.RequestAcquireSkillInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestActionUse;
import net.sf.l2j.gameserver.network.clientpackets.RequestAllyCrest;
import net.sf.l2j.gameserver.network.clientpackets.RequestAllyInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestAnswerFriendInvite;
import net.sf.l2j.gameserver.network.clientpackets.RequestAnswerJoinAlly;
import net.sf.l2j.gameserver.network.clientpackets.RequestAnswerJoinParty;
import net.sf.l2j.gameserver.network.clientpackets.RequestAnswerJoinPledge;
import net.sf.l2j.gameserver.network.clientpackets.RequestAskJoinPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestAutoSoulShot;
import net.sf.l2j.gameserver.network.clientpackets.RequestBBSwrite;
import net.sf.l2j.gameserver.network.clientpackets.RequestBlock;
import net.sf.l2j.gameserver.network.clientpackets.RequestBuyItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestBuyProcure;
import net.sf.l2j.gameserver.network.clientpackets.RequestBuySeed;
import net.sf.l2j.gameserver.network.clientpackets.RequestBypassToServer;
import net.sf.l2j.gameserver.network.clientpackets.RequestChangeMoveType;
import net.sf.l2j.gameserver.network.clientpackets.RequestChangePartyLeader;
import net.sf.l2j.gameserver.network.clientpackets.RequestChangePetName;
import net.sf.l2j.gameserver.network.clientpackets.RequestChangeWaitType;
import net.sf.l2j.gameserver.network.clientpackets.RequestCharacterCreate;
import net.sf.l2j.gameserver.network.clientpackets.RequestCharacterDelete;
import net.sf.l2j.gameserver.network.clientpackets.RequestConfirmCancelItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestConfirmGemStone;
import net.sf.l2j.gameserver.network.clientpackets.RequestConfirmRefinerItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestConfirmSiegeWaitingList;
import net.sf.l2j.gameserver.network.clientpackets.RequestConfirmTargetItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestCrystallizeItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestCursedWeaponList;
import net.sf.l2j.gameserver.network.clientpackets.RequestCursedWeaponLocation;
import net.sf.l2j.gameserver.network.clientpackets.RequestDeleteMacro;
import net.sf.l2j.gameserver.network.clientpackets.RequestDestroyItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestDismissAlly;
import net.sf.l2j.gameserver.network.clientpackets.RequestDismissPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestDropItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestDuelAnswerStart;
import net.sf.l2j.gameserver.network.clientpackets.RequestDuelStart;
import net.sf.l2j.gameserver.network.clientpackets.RequestDuelSurrender;
import net.sf.l2j.gameserver.network.clientpackets.RequestEnchantItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestEvaluate;
import net.sf.l2j.gameserver.network.clientpackets.RequestExAcceptJoinMPCC;
import net.sf.l2j.gameserver.network.clientpackets.RequestExAskJoinMPCC;
import net.sf.l2j.gameserver.network.clientpackets.RequestExEnchantSkill;
import net.sf.l2j.gameserver.network.clientpackets.RequestExEnchantSkillInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestExFishRanking;
import net.sf.l2j.gameserver.network.clientpackets.RequestExMPCCShowPartyMembersInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestExMagicSkillUseGround;
import net.sf.l2j.gameserver.network.clientpackets.RequestExOustFromMPCC;
import net.sf.l2j.gameserver.network.clientpackets.RequestExPledgeCrestLarge;
import net.sf.l2j.gameserver.network.clientpackets.RequestExSetPledgeCrestLarge;
import net.sf.l2j.gameserver.network.clientpackets.RequestExitPartyMatchingWaitingRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestFriendDel;
import net.sf.l2j.gameserver.network.clientpackets.RequestFriendInvite;
import net.sf.l2j.gameserver.network.clientpackets.RequestFriendList;
import net.sf.l2j.gameserver.network.clientpackets.RequestGMCommand;
import net.sf.l2j.gameserver.network.clientpackets.RequestGameStart;
import net.sf.l2j.gameserver.network.clientpackets.RequestGetBossRecord;
import net.sf.l2j.gameserver.network.clientpackets.RequestGetItemFromPet;
import net.sf.l2j.gameserver.network.clientpackets.RequestGiveItemToPet;
import net.sf.l2j.gameserver.network.clientpackets.RequestGiveNickName;
import net.sf.l2j.gameserver.network.clientpackets.RequestGmList;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaEquip;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaItemInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaItemList;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaUnequip;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaUnequipInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestHennaUnequipList;
import net.sf.l2j.gameserver.network.clientpackets.RequestItemList;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinAlly;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinParty;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinPledge;
import net.sf.l2j.gameserver.network.clientpackets.RequestJoinSiege;
import net.sf.l2j.gameserver.network.clientpackets.RequestLinkHtml;
import net.sf.l2j.gameserver.network.clientpackets.RequestListPartyMatchingWaitingRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestListPartyWaiting;
import net.sf.l2j.gameserver.network.clientpackets.RequestMagicSkillUse;
import net.sf.l2j.gameserver.network.clientpackets.RequestMakeMacro;
import net.sf.l2j.gameserver.network.clientpackets.RequestManagePartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestManorList;
import net.sf.l2j.gameserver.network.clientpackets.RequestNewCharacter;
import net.sf.l2j.gameserver.network.clientpackets.RequestOlympiadMatchList;
import net.sf.l2j.gameserver.network.clientpackets.RequestOlympiadObserverEnd;
import net.sf.l2j.gameserver.network.clientpackets.RequestOustFromPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestOustPartyMember;
import net.sf.l2j.gameserver.network.clientpackets.RequestOustPledgeMember;
import net.sf.l2j.gameserver.network.clientpackets.RequestPCCafeCouponUse;
import net.sf.l2j.gameserver.network.clientpackets.RequestPackageSend;
import net.sf.l2j.gameserver.network.clientpackets.RequestPackageSendableItemList;
import net.sf.l2j.gameserver.network.clientpackets.RequestPetGetItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestPetUseItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestPetition;
import net.sf.l2j.gameserver.network.clientpackets.RequestPetitionCancel;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeCrest;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeMemberInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeMemberList;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeMemberPowerInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgePower;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgePowerGradeList;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeReorganizeMember;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeSetAcademyMaster;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeSetMemberPowerGrade;
import net.sf.l2j.gameserver.network.clientpackets.RequestPledgeWarList;
import net.sf.l2j.gameserver.network.clientpackets.RequestPreviewItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreBuy;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreManageBuy;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreManageSell;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreQuitBuy;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreQuitSell;
import net.sf.l2j.gameserver.network.clientpackets.RequestPrivateStoreSell;
import net.sf.l2j.gameserver.network.clientpackets.RequestProcureCropList;
import net.sf.l2j.gameserver.network.clientpackets.RequestQuestAbort;
import net.sf.l2j.gameserver.network.clientpackets.RequestQuestList;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeBookDestroy;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeBookOpen;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeItemMakeInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeItemMakeSelf;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopListSet;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopMakeInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopMakeItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopManagePrev;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopManageQuit;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecipeShopMessageSet;
import net.sf.l2j.gameserver.network.clientpackets.RequestRecordInfo;
import net.sf.l2j.gameserver.network.clientpackets.RequestRefine;
import net.sf.l2j.gameserver.network.clientpackets.RequestRefineCancel;
import net.sf.l2j.gameserver.network.clientpackets.RequestReplyStartPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestReplyStopPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestReplySurrenderPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestRestart;
import net.sf.l2j.gameserver.network.clientpackets.RequestRestartPoint;
import net.sf.l2j.gameserver.network.clientpackets.RequestSSQStatus;
import net.sf.l2j.gameserver.network.clientpackets.RequestSellItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestSendL2FriendSay;
import net.sf.l2j.gameserver.network.clientpackets.RequestSetAllyCrest;
import net.sf.l2j.gameserver.network.clientpackets.RequestSetCrop;
import net.sf.l2j.gameserver.network.clientpackets.RequestSetPledgeCrest;
import net.sf.l2j.gameserver.network.clientpackets.RequestSetSeed;
import net.sf.l2j.gameserver.network.clientpackets.RequestShortCutDel;
import net.sf.l2j.gameserver.network.clientpackets.RequestShortCutReg;
import net.sf.l2j.gameserver.network.clientpackets.RequestShowBoard;
import net.sf.l2j.gameserver.network.clientpackets.RequestShowMiniMap;
import net.sf.l2j.gameserver.network.clientpackets.RequestSiegeAttackerList;
import net.sf.l2j.gameserver.network.clientpackets.RequestSiegeDefenderList;
import net.sf.l2j.gameserver.network.clientpackets.RequestSkillList;
import net.sf.l2j.gameserver.network.clientpackets.RequestSocialAction;
import net.sf.l2j.gameserver.network.clientpackets.RequestStartPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestStopPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestSurrenderPersonally;
import net.sf.l2j.gameserver.network.clientpackets.RequestSurrenderPledgeWar;
import net.sf.l2j.gameserver.network.clientpackets.RequestTargetCancel;
import net.sf.l2j.gameserver.network.clientpackets.RequestTutorialClientEvent;
import net.sf.l2j.gameserver.network.clientpackets.RequestTutorialLinkHtml;
import net.sf.l2j.gameserver.network.clientpackets.RequestTutorialPassCmdToServer;
import net.sf.l2j.gameserver.network.clientpackets.RequestTutorialQuestionMark;
import net.sf.l2j.gameserver.network.clientpackets.RequestUnEquipItem;
import net.sf.l2j.gameserver.network.clientpackets.RequestUserCommand;
import net.sf.l2j.gameserver.network.clientpackets.RequestWithdrawParty;
import net.sf.l2j.gameserver.network.clientpackets.RequestWithdrawPartyRoom;
import net.sf.l2j.gameserver.network.clientpackets.RequestWithdrawPledge;
import net.sf.l2j.gameserver.network.clientpackets.RequestWriteHeroWords;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.clientpackets.SendBypassBuildCmd;
import net.sf.l2j.gameserver.network.clientpackets.SendProtocolVersion;
import net.sf.l2j.gameserver.network.clientpackets.SendTimeCheck;
import net.sf.l2j.gameserver.network.clientpackets.SendWarehouseDepositList;
import net.sf.l2j.gameserver.network.clientpackets.SendWarehouseWithdrawList;
import net.sf.l2j.gameserver.network.clientpackets.SetPrivateStoreListBuy;
import net.sf.l2j.gameserver.network.clientpackets.SetPrivateStoreListSell;
import net.sf.l2j.gameserver.network.clientpackets.SetPrivateStoreMsgBuy;
import net.sf.l2j.gameserver.network.clientpackets.SetPrivateStoreMsgSell;
import net.sf.l2j.gameserver.network.clientpackets.SnoopQuit;
import net.sf.l2j.gameserver.network.clientpackets.StartRotating;
import net.sf.l2j.gameserver.network.clientpackets.TradeDone;
import net.sf.l2j.gameserver.network.clientpackets.TradeRequest;
import net.sf.l2j.gameserver.network.clientpackets.UseItem;
import net.sf.l2j.gameserver.network.clientpackets.ValidatePosition;
import net.sf.l2j.gameserver.network.clientpackets.ship.CannotMoveAnymoreInVehicle;
import net.sf.l2j.gameserver.network.clientpackets.ship.RequestGetOffVehicle;
import net.sf.l2j.gameserver.network.clientpackets.ship.RequestGetOnVehicle;
import net.sf.l2j.gameserver.network.clientpackets.ship.RequestMoveToLocationInVehicle;

import java.nio.ByteBuffer;

/**
 * The Stateful approach prevents the server from handling inconsistent packets.<BR>
 * <BR>
 * Note : If for a given exception a packet needs to be handled on more then one state, then it should be added to all
 * these states.
 */
@Slf4j
public final class GamePacketHandler implements IPacketHandler<GameClient>, IClientFactory<GameClient>, IMMOExecutor<GameClient> {

    @Override
    public ReceivablePacket<GameClient> handlePacket(ByteBuffer buf, GameClient client) {
        if (client.dropPacket()) {
            return null;
        }

        final int opcode = buf.get() & 0xFF;
        final GameClientState state = client.getState();

        ReceivablePacket<GameClient> msg = null;

        switch (state) {
            case CONNECTED:
                switch (opcode) {
                    case 0x00:
                        msg = new SendProtocolVersion();
                        break;
                    case 0x08:
                        msg = new AuthLogin();
                        break;
                    default:
                        printDebug(opcode, buf, state, client);
                        break;
                }
                break;

            case AUTHED:
                switch (opcode) {
                    case 0x09:
                        msg = new Logout();
                        break;
                    case 0x0b:
                        msg = new RequestCharacterCreate();
                        break;
                    case 0x0c:
                        msg = new RequestCharacterDelete();
                        break;
                    case 0x0d:
                        msg = new RequestGameStart();
                        break;
                    case 0x0e:
                        msg = new RequestNewCharacter();
                        break;
                    case 0x62:
                        msg = new CharacterRestore();
                        break;
                    case 0x68:
                        msg = new RequestPledgeCrest();
                        break;
                    default:
                        printDebug(opcode, buf, state, client);
                        break;
                }
                break;

            case ENTERING:
                switch (opcode) {
                    case 0x03:
                        msg = new EnterWorld();
                        break;

                    case 0xd0:
                        int id2 = -1;
                        if (buf.remaining() >= 2) {
                            id2 = buf.getShort() & 0xffff;
                        } else {
                            log.warn("{} sent a 0xd0 without the second opcode.", client);
                            break;
                        }

                        switch (id2) {
                            case 8:
                                msg = new RequestManorList();
                                break;
                            default:
                                printDebugDoubleOpcode(opcode, id2, buf, state, client);
                                break;
                        }
                        break;

                    case 63:
                        msg = new RequestQuestList();
                        break;

                    default:
                        printDebug(opcode, buf, state, client);
                        break;
                }
                break;

            case IN_GAME:
                switch (opcode) {
                    case 0x01:
                        msg = new MoveBackwardToLocation();
                        break;
                    // case 0x02:
                    // // Say ... not used any more ??
                    // break;
                    case 0x04:
                        msg = new Action();
                        break;
                    case 0x09:
                        msg = new Logout();
                        break;
                    case 0x0a:
                        msg = new AttackRequest();
                        break;
                    case 0x0f:
                        msg = new RequestItemList();
                        break;
                    // case 0x10:
                    // // RequestEquipItem ... not used any more, instead "useItem"
                    // break;
                    case 0x11:
                        msg = new RequestUnEquipItem();
                        break;
                    case 0x12:
                        msg = new RequestDropItem();
                        break;
                    case 0x14:
                        msg = new UseItem();
                        break;
                    case 0x15:
                        msg = new TradeRequest();
                        break;
                    case 0x16:
                        msg = new AddTradeItem();
                        break;
                    case 0x17:
                        msg = new TradeDone();
                        break;
                    case 0x1a:
                        msg = new DummyPacket();
                        break;
                    case 0x1b:
                        msg = new RequestSocialAction();
                        break;
                    case 0x1c:
                        msg = new RequestChangeMoveType();
                        break;
                    case 0x1d:
                        msg = new RequestChangeWaitType();
                        break;
                    case 0x1e:
                        msg = new RequestSellItem();
                        break;
                    case 0x1f:
                        msg = new RequestBuyItem();
                        break;
                    case 0x20:
                        msg = new RequestLinkHtml();
                        break;
                    case 0x21:
                        msg = new RequestBypassToServer();
                        break;
                    case 0x22:
                        msg = new RequestBBSwrite();
                        break;
                    case 0x23:
                        msg = new DummyPacket();
                        break;
                    case 0x24:
                        msg = new RequestJoinPledge();
                        break;
                    case 0x25:
                        msg = new RequestAnswerJoinPledge();
                        break;
                    case 0x26:
                        msg = new RequestWithdrawPledge();
                        break;
                    case 0x27:
                        msg = new RequestOustPledgeMember();
                        break;
                    // case 0x28:
                    // // RequestDismissPledge
                    // break;
                    case 0x29:
                        msg = new RequestJoinParty();
                        break;
                    case 0x2a:
                        msg = new RequestAnswerJoinParty();
                        break;
                    case 0x2b:
                        msg = new RequestWithdrawParty();
                        break;
                    case 0x2c:
                        msg = new RequestOustPartyMember();
                        break;
                    case 0x2d:
                        // RequestDismissParty
                        break;
                    case 0x2e:
                        msg = new DummyPacket();
                        break;
                    case 0x2f:
                        msg = new RequestMagicSkillUse();
                        break;
                    case 0x30:
                        msg = new Appearing();
                        break;
                    case 0x31:
                        if (Config.ALLOW_WAREHOUSE) {
                            msg = new SendWarehouseDepositList();
                        }
                        break;
                    case 0x32:
                        msg = new SendWarehouseWithdrawList();
                        break;
                    case 0x33:
                        msg = new RequestShortCutReg();
                        break;
                    case 0x34:
                        msg = new DummyPacket();
                        break;
                    case 0x35:
                        msg = new RequestShortCutDel();
                        break;
                    case 0x36:
                        msg = new CannotMoveAnymore();
                        break;
                    case 0x37:
                        msg = new RequestTargetCancel();
                        break;
                    case 0x38:
                        msg = new Say2();
                        break;
                    case 0x3c:
                        msg = new RequestPledgeMemberList();
                        break;
                    case 0x3e:
                        msg = new DummyPacket();
                        break;
                    case 0x3f:
                        msg = new RequestSkillList();
                        break;
                    // case 0x41:
                    // // MoveWithDelta ... unused ?? or only on ship ??
                    // break;
                    case 0x42:
                        msg = new RequestGetOnVehicle();
                        break;
                    case 0x43:
                        msg = new RequestGetOffVehicle();
                        break;
                    case 0x44:
                        msg = new AnswerTradeRequest();
                        break;
                    case 0x45:
                        msg = new RequestActionUse();
                        break;
                    case 0x46:
                        msg = new RequestRestart();
                        break;
                    // case 0x47:
                    // // RequestSiegeInfo
                    // break;
                    case 0x48:
                        msg = new ValidatePosition();
                        break;
                    // case 0x49:
                    // // RequestSEKCustom
                    // break;
                    case 0x4a:
                        msg = new StartRotating();
                        break;
                    case 0x4b:
                        msg = new FinishRotating();
                        break;
                    case 0x4d:
                        msg = new RequestStartPledgeWar();
                        break;
                    case 0x4e:
                        msg = new RequestReplyStartPledgeWar();
                        break;
                    case 0x4f:
                        msg = new RequestStopPledgeWar();
                        break;
                    case 0x50:
                        msg = new RequestReplyStopPledgeWar();
                        break;
                    case 0x51:
                        msg = new RequestSurrenderPledgeWar();
                        break;
                    case 0x52:
                        msg = new RequestReplySurrenderPledgeWar();
                        break;
                    case 0x53:
                        msg = new RequestSetPledgeCrest();
                        break;
                    case 0x55:
                        msg = new RequestGiveNickName();
                        break;
                    case 0x57:
                        msg = new RequestShowBoard();
                        break;
                    case 0x58:
                        msg = new RequestEnchantItem();
                        break;
                    case 0x59:
                        msg = new RequestDestroyItem();
                        break;
                    case 0x5b:
                        msg = new SendBypassBuildCmd();
                        break;
                    case 0x5c:
                        msg = new RequestMoveToLocationInVehicle();
                        break;
                    case 0x5d:
                        msg = new CannotMoveAnymoreInVehicle();
                        break;
                    case 0x5e:
                        msg = new RequestFriendInvite();
                        break;
                    case 0x5f:
                        msg = new RequestAnswerFriendInvite();
                        break;
                    case 0x60:
                        msg = new RequestFriendList();
                        break;
                    case 0x61:
                        msg = new RequestFriendDel();
                        break;
                    case 0x63:
                        msg = new RequestQuestList();
                        break;
                    case 0x64:
                        msg = new RequestQuestAbort();
                        break;
                    case 0x66:
                        msg = new RequestPledgeInfo();
                        break;
                    // case 0x67:
                    // // RequestPledgeExtendedInfo
                    // break;
                    case 0x68:
                        msg = new RequestPledgeCrest();
                        break;
                    case 0x69:
                        msg = new RequestSurrenderPersonally();
                        break;
                    // case 0x6a:
                    // // Ride
                    // break;
                    case 0x6b:
                        msg = new RequestAcquireSkillInfo();
                        break;
                    case 0x6c:
                        msg = new RequestAcquireSkill();
                        break;
                    case 0x6d:
                        msg = new RequestRestartPoint();
                        break;
                    case 0x6e:
                        msg = new RequestGMCommand();
                        break;
                    case 0x6f:
                        msg = new RequestListPartyWaiting();
                        break;
                    case 0x70:
                        msg = new RequestManagePartyRoom();
                        break;
                    case 0x71:
                        msg = new RequestJoinPartyRoom();
                        break;
                    case 0x72:
                        msg = new RequestCrystallizeItem();
                        break;
                    case 0x73:
                        msg = new RequestPrivateStoreManageSell();
                        break;
                    case 0x74:
                        msg = new SetPrivateStoreListSell();
                        break;
                    // case 0x75:
                    // msg = new RequestPrivateStoreManageCancel(data, _client);
                    // break;
                    case 0x76:
                        msg = new RequestPrivateStoreQuitSell();
                        break;
                    case 0x77:
                        msg = new SetPrivateStoreMsgSell();
                        break;
                    // case 0x78:
                    // // RequestPrivateStoreList
                    // break;
                    case 0x79:
                        msg = new RequestPrivateStoreBuy();
                        break;
                    // case 0x7a:
                    // // ReviveReply
                    // break;
                    case 0x7b:
                        msg = new RequestTutorialLinkHtml();
                        break;
                    case 0x7c:
                        msg = new RequestTutorialPassCmdToServer();
                        break;
                    case 0x7d:
                        msg = new RequestTutorialQuestionMark();
                        break;
                    case 0x7e:
                        msg = new RequestTutorialClientEvent();
                        break;
                    case 0x7f:
                        msg = new RequestPetition();
                        break;
                    case 0x80:
                        msg = new RequestPetitionCancel();
                        break;
                    case 0x81:
                        msg = new RequestGmList();
                        break;
                    case 0x82:
                        msg = new RequestJoinAlly();
                        break;
                    case 0x83:
                        msg = new RequestAnswerJoinAlly();
                        break;
                    case 0x84:
                        msg = new AllyLeave();
                        break;
                    case 0x85:
                        msg = new AllyDismiss();
                        break;
                    case 0x86:
                        msg = new RequestDismissAlly();
                        break;
                    case 0x87:
                        msg = new RequestSetAllyCrest();
                        break;
                    case 0x88:
                        msg = new RequestAllyCrest();
                        break;
                    case 0x89:
                        msg = new RequestChangePetName();
                        break;
                    case 0x8a:
                        msg = new RequestPetUseItem();
                        break;
                    case 0x8b:
                        msg = new RequestGiveItemToPet();
                        break;
                    case 0x8c:
                        msg = new RequestGetItemFromPet();
                        break;
                    case 0x8e:
                        msg = new RequestAllyInfo();
                        break;
                    case 0x8f:
                        msg = new RequestPetGetItem();
                        break;
                    case 0x90:
                        msg = new RequestPrivateStoreManageBuy();
                        break;
                    case 0x91:
                        msg = new SetPrivateStoreListBuy();
                        break;
                    // case 0x92:
                    // // RequestPrivateStoreBuyManageCancel
                    // break;
                    case 0x93:
                        msg = new RequestPrivateStoreQuitBuy();
                        break;
                    case 0x94:
                        msg = new SetPrivateStoreMsgBuy();
                        break;
                    // case 0x95:
                    // // RequestPrivateStoreBuyList
                    // break;
                    case 0x96:
                        msg = new RequestPrivateStoreSell();
                        break;
                    case 0x97:
                        msg = new SendTimeCheck();
                        break;
                    // case 0x98:
                    // // RequestStartAllianceWar
                    // break;
                    // case 0x99:
                    // // ReplyStartAllianceWar
                    // break;
                    // case 0x9a:
                    // // RequestStopAllianceWar
                    // break;
                    // case 0x9b:
                    // // ReplyStopAllianceWar
                    // break;
                    // case 0x9c:
                    // // RequestSurrenderAllianceWar
                    // break;
                    case 0x9d:
                        // RequestSkillCoolTime
                        break;
                    case 0x9e:
                        msg = new RequestPackageSendableItemList();
                        break;
                    case 0x9f:
                        msg = new RequestPackageSend();
                        break;
                    case 0xa0:
                        msg = new RequestBlock();
                        break;
                    // case 0xa1:
                    // // RequestCastleSiegeInfo
                    // break;
                    case 0xa2:
                        msg = new RequestSiegeAttackerList();
                        break;
                    case 0xa3:
                        msg = new RequestSiegeDefenderList();
                        break;
                    case 0xa4:
                        msg = new RequestJoinSiege();
                        break;
                    case 0xa5:
                        msg = new RequestConfirmSiegeWaitingList();
                        break;
                    // case 0xa6:
                    // // RequestSetCastleSiegeTime
                    // break;
                    case 0xa7:
                        msg = new MultiSellChoose();
                        break;
                    // case 0xa8:
                    // // NetPing
                    // break;
                    case 0xaa:
                        msg = new RequestUserCommand();
                        break;
                    case 0xab:
                        msg = new SnoopQuit();
                        break;
                    case 0xac: // we still need this packet to handle BACK button of craft dialog
                        msg = new RequestRecipeBookOpen();
                        break;
                    case 0xad:
                        msg = new RequestRecipeBookDestroy();
                        break;
                    case 0xae:
                        msg = new RequestRecipeItemMakeInfo();
                        break;
                    case 0xaf:
                        msg = new RequestRecipeItemMakeSelf();
                        break;
                    // case 0xb0:
                    // msg = new RequestRecipeShopManageList(data, client);
                    // break;
                    case 0xb1:
                        msg = new RequestRecipeShopMessageSet();
                        break;
                    case 0xb2:
                        msg = new RequestRecipeShopListSet();
                        break;
                    case 0xb3:
                        msg = new RequestRecipeShopManageQuit();
                        break;
                    case 0xb5:
                        msg = new RequestRecipeShopMakeInfo();
                        break;
                    case 0xb6:
                        msg = new RequestRecipeShopMakeItem();
                        break;
                    case 0xb7:
                        msg = new RequestRecipeShopManagePrev();
                        break;
                    case 0xb8:
                        msg = new ObserverReturn();
                        break;
                    case 0xb9:
                        msg = new RequestEvaluate();
                        break;
                    case 0xba:
                        msg = new RequestHennaItemList();
                        break;
                    case 0xbb:
                        msg = new RequestHennaItemInfo();
                        break;
                    case 0xbc:
                        msg = new RequestHennaEquip();
                        break;
                    case 0xbd:
                        msg = new RequestHennaUnequipList();
                        break;
                    case 0xbe:
                        msg = new RequestHennaUnequipInfo();
                        break;
                    case 0xbf:
                        msg = new RequestHennaUnequip();
                        break;
                    case 0xc0:
                        // Clan Privileges
                        msg = new RequestPledgePower();
                        break;
                    case 0xc1:
                        msg = new RequestMakeMacro();
                        break;
                    case 0xc2:
                        msg = new RequestDeleteMacro();
                        break;
                    case 0xc3:
                        msg = new RequestBuyProcure();
                        break;
                    case 0xc4:
                        msg = new RequestBuySeed();
                        break;
                    case 0xc5:
                        msg = new DlgAnswer();
                        break;
                    case 0xc6:
                        msg = new RequestPreviewItem();
                        break;
                    case 0xc7:
                        msg = new RequestSSQStatus();
                        break;
                    case 0xc8:
                        msg = new PetitionVote();
                        break;
                    case 0xCA:
                        msg = new GameGuardReply();
                        break;
                    case 0xcc:
                        msg = new RequestSendL2FriendSay();
                        break;
                    case 0xcd:
                        msg = new RequestShowMiniMap();
                        break;
                    case 0xce: // MSN dialogs so that you dont see them in the console.
                        break;
                    case 0xcf:
                        msg = new RequestRecordInfo();
                        break;

                    case 0xd0:
                        int id2 = -1;
                        if (buf.remaining() >= 2) {
                            id2 = buf.getShort() & 0xffff;
                        } else {
                            log.warn("{} sent a 0xd0 without the second opcode.", client);
                            break;
                        }

                        switch (id2) {
                            case 1:
                                msg = new RequestOustFromPartyRoom();
                                break;
                            case 2:
                                msg = new RequestDismissPartyRoom();
                                break;
                            case 3:
                                msg = new RequestWithdrawPartyRoom();
                                break;
                            case 4:
                                msg = new RequestChangePartyLeader();
                                break;
                            case 5:
                                msg = new RequestAutoSoulShot();
                                break;
                            case 6:
                                msg = new RequestExEnchantSkillInfo();
                                break;
                            case 7:
                                msg = new RequestExEnchantSkill();
                                break;
                            case 8:
                                msg = new RequestManorList();
                                break;
                            case 9:
                                msg = new RequestProcureCropList();
                                break;
                            case 0x0a:
                                msg = new RequestSetSeed();
                                break;
                            case 0x0b:
                                msg = new RequestSetCrop();
                                break;
                            case 0x0c:
                                msg = new RequestWriteHeroWords();
                                break;
                            case 0x0d:
                                msg = new RequestExAskJoinMPCC();
                                break;
                            case 0x0e:
                                msg = new RequestExAcceptJoinMPCC();
                                break;
                            case 0x0f:
                                msg = new RequestExOustFromMPCC();
                                break;
                            case 0x10:
                                msg = new RequestExPledgeCrestLarge();
                                break;
                            case 0x11:
                                msg = new RequestExSetPledgeCrestLarge();
                                break;
                            case 0x12:
                                msg = new RequestOlympiadObserverEnd();
                                break;
                            case 0x13:
                                msg = new RequestOlympiadMatchList();
                                break;
                            case 0x14:
                                msg = new RequestAskJoinPartyRoom();
                                break;
                            case 0x15:
                                msg = new AnswerJoinPartyRoom();
                                break;
                            case 0x16:
                                msg = new RequestListPartyMatchingWaitingRoom();
                                break;
                            case 0x17:
                                msg = new RequestExitPartyMatchingWaitingRoom();
                                break;
                            case 0x18:
                                msg = new RequestGetBossRecord();
                                break;
                            case 0x19:
                                msg = new RequestPledgeSetAcademyMaster();
                                break;
                            case 0x1a:
                                msg = new RequestPledgePowerGradeList();
                                break;
                            case 0x1b:
                                msg = new RequestPledgeMemberPowerInfo();
                                break;
                            case 0x1c:
                                msg = new RequestPledgeSetMemberPowerGrade();
                                break;
                            case 0x1d:
                                msg = new RequestPledgeMemberInfo();
                                break;
                            case 0x1e:
                                msg = new RequestPledgeWarList();
                                break;
                            case 0x1f:
                                msg = new RequestExFishRanking();
                                break;
                            case 0x20:
                                msg = new RequestPCCafeCouponUse();
                                break;
                            // couldnt find it 0x21 :S
                            case 0x22:
                                msg = new RequestCursedWeaponList();
                                break;
                            case 0x23:
                                msg = new RequestCursedWeaponLocation();
                                break;
                            case 0x24:
                                msg = new RequestPledgeReorganizeMember();
                                break;
                            // couldnt find it 0x25 :S
                            case 0x26:
                                msg = new RequestExMPCCShowPartyMembersInfo();
                                break;
                            case 0x27:
                                msg = new RequestDuelStart();
                                break;
                            case 0x28:
                                msg = new RequestDuelAnswerStart();
                                break;
                            case 0x29:
                                msg = new RequestConfirmTargetItem();
                                break;
                            case 0x2a:
                                msg = new RequestConfirmRefinerItem();
                                break;
                            case 0x2b:
                                msg = new RequestConfirmGemStone();
                                break;
                            case 0x2c:
                                msg = new RequestRefine();
                                break;
                            case 0x2d:
                                msg = new RequestConfirmCancelItem();
                                break;
                            case 0x2e:
                                msg = new RequestRefineCancel();
                                break;
                            case 0x2f:
                                msg = new RequestExMagicSkillUseGround();
                                break;
                            case 0x30:
                                msg = new RequestDuelSurrender();
                                break;
                            default:
                                printDebugDoubleOpcode(opcode, id2, buf, state, client);
                                break;
                        }
                        break;
                    default:
                        printDebug(opcode, buf, state, client);
                        break;
                }
                break;
        }
        return msg;
    }

    @Override
    public GameClient create(MMOConnection<GameClient> con) {
        return new GameClient(con);
    }

    @Override
    public void execute(ReceivablePacket<GameClient> rp) {
        rp.getClient().execute(rp);
    }

    private static void printDebug(int opcode, ByteBuffer buf, GameClientState state, GameClient client) {
        client.onUnknownPacket();

        if (!Config.PACKET_HANDLER_DEBUG) {
            return;
        }

        log.warn("{} sent unknown packet 0x{} on state {}.", client, Integer.toHexString(opcode), state.name());

        final int size = buf.remaining();
        final byte[] array = new byte[size];
        buf.get(array);

        log.warn(HexUtil.printData(array, size));
    }

    private static void printDebugDoubleOpcode(int opcode, int id2, ByteBuffer buf, GameClientState state, GameClient client) {
        client.onUnknownPacket();

        if (!Config.PACKET_HANDLER_DEBUG) {
            return;
        }

        log.warn("{} sent unknown packet 0x{}:{} on state {}.", client, Integer.toHexString(opcode), Integer.toHexString(id2), state.name());

        final int size = buf.remaining();
        final byte[] array = new byte[size];
        buf.get(array);

        log.warn(HexUtil.printData(array, size));
    }
}