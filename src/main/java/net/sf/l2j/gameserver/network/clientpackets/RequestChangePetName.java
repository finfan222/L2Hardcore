package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.PlayerDao;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class RequestChangePetName extends L2GameClientPacket {

    private String _name;

    @Override
    protected void readImpl() {
        _name = readS();
    }

    @Override
    protected void runImpl() {
        final Player player = getClient().getPlayer();
        if (player == null) {
            return;
        }

        // No active pet.
        if (!player.hasPet()) {
            return;
        }

        // Name length integrity check.
        if (_name.isEmpty() || _name.length() > 16) {
            player.sendPacket(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS);
            return;
        }

        // Pet is already named.
        final Pet pet = (Pet) player.getSummon();
        if (pet.getName() != null) {
            player.sendPacket(SystemMessageId.NAMING_YOU_CANNOT_SET_NAME_OF_THE_PET);
            return;
        }

        // Invalid name pattern.
        if (!StringUtil.isValidString(_name, "^[A-Za-z0-9]{1,16}$")) {
            player.sendPacket(SystemMessageId.NAMING_PETNAME_CONTAINS_INVALID_CHARS);
            return;
        }

        // Name is a npc name.
        if (NpcData.getInstance().getTemplateByName(_name) != null) {
            return;
        }

        // Name already exists on another pet.
        if (PlayerDao.petNameAlreadyExists(_name)) {
            player.sendPacket(SystemMessageId.NAMING_ALREADY_IN_USE_BY_ANOTHER_PET);
            return;
        }

        pet.setName(_name);
        pet.sendPetInfosToOwner();
    }

}