package net.sf.l2j.gameserver.model.actor.container.monster;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

import java.util.ArrayList;

/**
 * A container holding all related informations of a {@link Monster} spoil state.<br>
 * <br>
 * A spoil occurs when a {@link Player} procs a spoil skill over a Monster.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SpoilState extends ArrayList<IntIntHolder> {

    private int spoilerId;

    public boolean isSpoiled() {
        return spoilerId > 0;
    }

    public boolean isActualSpoiler(Player player) {
        return player != null && player.getObjectId() == spoilerId;
    }

    public boolean isSweepable() {
        return !isEmpty();
    }

    @Override
    public void clear() {
        spoilerId = 0;
        super.clear();
    }
}