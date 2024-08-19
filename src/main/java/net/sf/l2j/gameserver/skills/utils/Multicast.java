package net.sf.l2j.gameserver.skills.utils;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
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
public class Multicast implements Runnable {

    private static final int NPC_ID = 50010;

    private final AtomicInteger counter = new AtomicInteger(0);

    private final Creature caster;
    private final Creature target;
    private final L2Skill skill;
    private final int counts;
    private final int chance;
    private final int hitTime;
    private final EffectPoint effectPoint;

    private ScheduledFuture<?> task;

    public Multicast(Creature caster, Creature target, L2Skill skill, int counts, int chance) {
        this(caster, target, skill, counts, chance, 500);
    }

    public Multicast(Creature caster, Creature target, L2Skill skill, int counts, int chance, int hitTime) {
        this.caster = caster;
        this.target = target;
        this.skill = skill;
        this.counts = counts;
        this.chance = chance;
        this.hitTime = hitTime;
        this.effectPoint = spawn();
    }

    private EffectPoint spawn() {
        NpcTemplate template = NpcData.getInstance().getTemplate(NPC_ID);
        EffectPoint npc = new EffectPoint(IdFactory.getInstance().getNextId(), template, caster);
        npc.setInvul(true);
        npc.setXYZ(target);
        npc.setTarget(target);
        npc.spawnMe();
        return npc;
    }

    private void cast() {
        task = ThreadPool.scheduleAtFixedRate(this, hitTime - 200, hitTime - 200);
        effectPoint.broadcastPacket(new MagicSkillUse(effectPoint, target, skill.getId(), skill.getLevel(), hitTime, 0));
    }

    public static void start(Creature caster, Creature target, L2Skill skill, int counts, int chance) {
        if (Rnd.calcChance(chance, 100)) {
            new Multicast(caster, target, skill, counts, chance).cast();
        }
    }

    public static void start(Creature caster, Creature target, L2Skill skill, int counts, int chance, int hitTime) {
        if (Rnd.calcChance(chance, 100)) {
            new Multicast(caster, target, skill, counts, chance, hitTime).cast();
        }
    }

    @Override
    public void run() {
        if (target.isDead() || target.isInvul() || counter.incrementAndGet() == counts) {
            effectPoint.deleteMe();
            task.cancel(false);
            task = null;
            return;
        }

        skill.useSkill(effectPoint, new WorldObject[]{target});
        effectPoint.broadcastPacket(new MagicSkillUse(effectPoint, target, skill.getId(), skill.getLevel(), hitTime - 200, 0));
    }

}
