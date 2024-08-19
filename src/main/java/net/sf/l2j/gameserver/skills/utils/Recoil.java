package net.sf.l2j.gameserver.skills.utils;

import lombok.extern.slf4j.Slf4j;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.EffectPoint;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author finfan
 */
@Slf4j
public class Recoil implements Runnable {

    private static final int NPC_ID = 50010;

    private final AtomicInteger counter = new AtomicInteger();

    private final EffectPoint effectPoint;
    private final L2Skill skill;
    private final int count;
    private final int radius;
    private final boolean playerPriority;

    private ScheduledFuture<?> task;
    private Creature target;

    private final ReentrantLock locker = new ReentrantLock(true);

    private Recoil(Creature owner, Creature target, L2Skill skill, boolean playerPriority, int count, int radius) {
        this.skill = skill;
        this.playerPriority = playerPriority;
        this.count = count;
        this.radius = radius;
        this.effectPoint = spawn(owner, target);
    }

    private EffectPoint spawn(Creature owner, Creature target) {
        NpcTemplate template = NpcData.getInstance().getTemplate(NPC_ID);
        EffectPoint npc = new EffectPoint(IdFactory.getInstance().getNextId(), template, owner);
        npc.setInvul(true);
        npc.setXYZ(target.getX(), target.getY(), target.getZ());
        npc.spawnMe();
        return npc;
    }

    private void cast() {
        locker.lock();
        try {
            WorldObject lastTarget = effectPoint.getTarget();
            if (lastTarget != null) {
                effectPoint.setXYZ(lastTarget);
                effectPoint.broadcastPacket(new ValidateLocation(effectPoint));
            }

            List<Creature> list = effectPoint.getKnownTypeInRadius(Creature.class, radius)
                .stream()
                .filter(target -> target.isAttackableWithoutForceBy(effectPoint.getActingPlayer()) // берем всех кого можно атаковать игроком и кто не является мертвым или бессмертным
                    && (!target.isAlikeDead() && !target.isDead()) && !target.isInvul())
                .sorted(Comparator.comparing(target -> target.distance2D(effectPoint))) // сортируем в зависимости от расстояния (ближние в первую очередь)
                .toList();

            // сортируем в зависимости от приоритета, если Player в приоритете, то берем их первыми
            if (playerPriority) {
                list.sort(Comparator.comparing(e -> e instanceof Player));
            }

            target = Rnd.get(list.stream().limit(count).toList());
        } finally {
            locker.unlock();
        }

        if (target != null) {
            effectPoint.setTarget(target);
            int time = Formulas.calcProjectileFlyTime(effectPoint, target, 400);
            effectPoint.broadcastPacket(new MagicSkillUse(effectPoint, target, skill.getId(), skill.getLevel(), time, 0));
            task = ThreadPool.schedule(this, time);
        } else {
            stop();
        }
    }

    private void stop() {
        effectPoint.deleteMe();
        counter.set(0);
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    public static void start(Creature owner, Creature target, L2Skill skill, int chance, int count, boolean playerPriority, int radius) {
        if (!skill.isProjectile()) {
            return;
        }

        if (owner == null || (chance > 0 && !Rnd.calcChance(chance, 100))) {
            return;
        }

        new Recoil(owner, target, skill, playerPriority, count, radius).cast();
    }

    @Override
    public void run() {
        skill.useSkill(effectPoint.getOwner(), new WorldObject[]{target});
        if (counter.incrementAndGet() < count) {
            cast();
        } else {
            stop();
        }
    }
}
