package net.sf.l2j.gameserver.skills.utils;

import lombok.Data;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.EffectPoint;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author finfan
 */
public class Multicast {

    private static final int NPC_ID = 0;

    private final int counts;
    private final AtomicInteger counter = new AtomicInteger(0);

    private ScheduledFuture<?> future;

    private Multicast(int counts) {
        this.counts = counts;
    }

    private void create(Creature owner, Creature target, int id, int level) {
        NpcTemplate template = NpcData.getInstance().getTemplate(NPC_ID);
        EffectPoint npc = new EffectPoint(IdFactory.getInstance().getNextId(), template, owner);
        npc.setInvul(true);
        npc.setXYZ(owner);
        npc.setTarget(target);
        npc.spawnMe(owner.getX(), owner.getY(), owner.getZ());
        npc.broadcastPacket(new MagicSkillUse(npc, target, id, level, 500, 0));
        hitTask(npc, target, SkillTable.getInstance().getInfo(id, level));
    }

    private void hitTask(EffectPoint caster, Creature target, L2Skill skill) {
        future = ThreadPool.scheduleAtFixedRate(new MulticastTask(caster, target, skill), 600, 600);
    }

    @Data
    public class MulticastTask implements Runnable {

        private final EffectPoint caster;
        private final Creature target;
        private final L2Skill skill;

        @Override
        public void run() {
            if (target.isDead() || target.isInvul() || counter.get() == counts) {
                caster.deleteMe();
                future.cancel(false);
                return;
            }

            skill.useSkill(caster, new WorldObject[]{target});
            counter.incrementAndGet();
        }

    }

    public static void start(int counts, Creature owner, Creature target, int id, int level) {
        new Multicast(counts).create(owner, target, id, level);
    }

}
