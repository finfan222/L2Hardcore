package net.sf.l2j.gameserver.scripting.quest;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.SocialType;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestActionTask;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.skills.L2Skill;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <ul>
 *     <li>1. Мы ищем рыцаря по имени Sir Claus Vesper, говорим с ним проходя через критерии 39 lvl, Human Knight квест создается и он отправляется нас к высшему рыцарю Sir Eric Rodemal (cond=1)</li>
 *     <li>2. В Адене мы беседуем с Sir Eric Rodemal, он рассказывает что паладином может стать только тот, кто прошел 3 испытания. Испытания можно пройти только получив 3 благословения. (cond=2)</li>
 *     <li>
 *         3. Для получения благословений необходимо найти 3 любых High Priest в 3 разных городах (в храмах Эйнхасад - Света).
 *         <ul>
 *             <li>С этого момента, каждый жрец в своем chat window имеет открытый диалог желтого цвета - "Попросить о возможности молитвы".</li>
 *             <li>В каждом монастыре - собственная молитва (бафф-эффект, который увеличивает что-либо или дает).</li>
 *             <li>Каждый храм имеет свое условие проведения молитв. Например храм в Aden требует 300.000 аден, для возможности молиться. Если игрок НИ разу не ПКашил, то 100.000</li>
 *             <li>Пройдя молитву, получаем pray_aden=true в дальнейшем - проверяя этот фактор в будущих случаях и не давая молиться повторно</li>
 *             <li>Персонаж также получает бафф-эффект вечного типа без возможности снятия его и не учитывающегося в списке бафов. (Например макс. бафы +1)</li>
 *         </ul>
 *     </li>
 *     <li>4. Пройдя молитву cond=3,4,5</li>
 *     <li>5. Когда игрок имеет cond=5, ему необходимо снова поговорим с Sir Eric Rodemal, который рассказывает о 3-х испытаниях cond=6</li>
 *     <li>6. "Испытание Храбрости и Отваги" - необходимо сразить нежить в одном из мест. Случайное ол-во нежити (максимум 100) cond=7 при чтении прегамента и cond=8 после убийства нежити и получения нового пергамента</li>
 *     <li>7. "Испытание Магии и Духа" - необходимо уничтожать нежить заклинание Holy Blessing/Holy Strike выданное на время действия квеста (случ. кол-во макс. 100) cond=9, после набора cond=10.</li>
 *     <li>8. "Испытание Добродетеля" - Необходимо убить квестового монстра, на которого не действует оружие или магия кроме "Holy Strike" и "Holy Blessing". Игрок может делать квест с другими паладинами (игроками). cond=11, после убийства cond=12</li>
 *     <li>9. Выполнив последнее испытание, игрок на ходу меняет профессию на Paladin и cond=13.</li>
 *     <li>10. Если поговорить с Sir Eric Rodemal еще раз при cond=13, то cond=14 и он поздравляет героя с этим достижением и дает в награду клинок Дюрендаль (если это первый Паладин на сервере). Этот клинок нельзя передавать, и если игрок не появлялся в игре более 7 дней, то он исчезает из его инвентаря выдаваясь другому будущему паладину. Если носитель Дюрендаля имеется, то игрок получает награду в кол-ве 500.000 аден, 300 средних эликсиров. quest=COMPLETED</li>
 *     <li>11. "Дюрендаль" - клинок растущий с ростом игрока. Каждые 40/52/61 уровень он вырастает на Grade автоматически, либо после перезахода в игру. Клинок в базе хранится в одном экземпляре и ресторится при запуске сервера. Если клинок попал к новому игроку, то его Grade падает до нужного, но не ниже C. Клинок могут носить только Paladin и их производные. Докочавшись до 76-ого клинок автоматически исчезает (предупреждая об этом игрока). Клинок умеет разговаривать. В дальнейшем перейдет к новому паладину, которому требуется помощь.</li>
 * </ul>
 *
 * @author finfan
 */
public class CQ001_PathOfPaladin extends Quest {

    private static final String QUEST_NAME = "CQ001_PathOfPaladin";
    private static final String PRAY_TASK_NAME = "PaladinTask";
    private static final int PRAY_TASK_TIME = 60_000;
    private static final String[] PRAY_TEXT = {
        "Молю, Ейнхасад, услышь меня и ответь на мои молитвы!",
        "Существует необходимость в ответе на мои молитвы. Прошу Ейнхасад... помоги мне с этим...",
        "Ты что, не слышишь мои молитвы? Сколько мне еще пытаться тебя просить...",
        "Богиня Света - Эйнхасад! Пожалуйста, откликнись! Дай мне знак... хотя бы какой-нибудь..."
    };
    private static final String PRAY_SUCCESS = "Вы ощущаете мощный прилив магической энергии не из этого мира. Словно вас коснулся сам Свет. Это был ясный знак, что Эйнхасад услышала ваши молитвы. Идите с миром - неся ее Свет.";

    private static final int SIR_CLAUS_VESPER = 30417;
    private static final int SIR_ERIC_RODEMAI = 30868;
    private static final int[] QUEST_PRIESTS = {
        30037, // Levian (Gludin Town)
        30289, // Raymond (Gludio Town)
        30070, // Sylvain (Dion Town)
        30905, // Squillari (Heine Town)
        30120, // Maximillian (Giran Town)
        30857, // Orven (Aden Town)
        30191, // Hollin (Oren Castle Town)
        31279, // Gregory (Goddard Town)
        31968, // Baryl (Shuttgard)
        31348 // Agripel (Rune Town)
    };

    public static Map<Integer, L2Skill> PRAY_BUFFS = new HashMap<>();

    public CQ001_PathOfPaladin() {
        super(1001, "Light in the Dark");

        addStartNpc(SIR_CLAUS_VESPER);
        addTalkId(SIR_ERIC_RODEMAI);
        addTalkId(QUEST_PRIESTS);

        PRAY_BUFFS.put(30037, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30289, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30070, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30905, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30120, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30857, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(30191, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(31279, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(31968, SkillTable.getInstance().getInfo(1, 1));
        PRAY_BUFFS.put(31348, SkillTable.getInstance().getInfo(1, 1));

    }

    @Override
    protected void initializeConditions() {
        condition.level = 39;
        condition.classId = ClassId.KNIGHT;
    }

    @Override
    public String onAdvEvent(String html, Npc npc, Player player) {
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return html;
        }

        if (html.equalsIgnoreCase("30417-cq001-1.htm")) {
            st.setState(QuestStatus.STARTED, player, npc, html);
            playSound(player, SOUND_ACCEPT);
            st.setCond(1);
        }

        return super.onAdvEvent(html, npc, player);
    }

    @Override
    public String onTalk(Npc npc, Player player) {
        String html = getNoQuestMsg();
        QuestState st = player.getQuestList().getQuestState(QUEST_NAME);
        if (st == null) {
            return html;
        }

        int npcId = npc.getNpcId();
        int cond = st.getCond();
        switch (st.getState()) {
            case CREATED: {
                return condition.checkAllValidations(player) ? "30417-cq001-1.htm" : "30417-cq001-0.htm";
            }

            case STARTED: {
                if (cond == 1) {
                    if (npcId == SIR_CLAUS_VESPER) {
                        html = generateHtml(SIR_CLAUS_VESPER, "cq001-2");
                    }
                }
                break;
            }

            case COMPLETED: {
                html = getAlreadyCompletedMsg();
                break;
            }
        }

        return html;
    }

    private void pray(QuestState questState, Npc npc) {
        if (questState.isHasTask(PRAY_TASK_NAME) || questState.getPlayer().isUncontrollable()) {
            return;
        }

        questState.getPlayer().sendMessage("Вы начинаете читать молитвы Эйнхасад в надежде, что она ответит вам.");
        playSound(questState.getPlayer(), SOUND_ACCEPT);
        playSocialAction(questState.getPlayer(), SocialType.BOW);
        ThreadPool.schedule(() -> questState.addTask(PRAY_TASK_NAME, ThreadPool.scheduleAtFixedRate(new PrayTask(questState, npc.getNpcId(), AbnormalEffect.HOLD_1), PRAY_TASK_TIME, PRAY_TASK_TIME)), 2000);
    }

    private static class PrayTask extends QuestActionTask {

        private final int npcId;
        private final AtomicInteger chance = new AtomicInteger();
        private final AtomicInteger counter = new AtomicInteger();

        public PrayTask(QuestState questState, int npcId, AbnormalEffect abnormalEffect) {
            super(questState, abnormalEffect);
            this.npcId = npcId;
            onStart();
        }

        @Override
        public void onStart() {
            super.onStart();
            questState.getPlayer().setUncontrollable(true);
        }

        @Override
        public void onFinish() {
            super.onFinish();
            playSound(player, SOUND_ACCEPT);
            playSocialAction(player, SocialType.VICTORY);
            player.sendPacket(new CreatureSay(player, SayType.CRITICAL_ANNOUNCE, PRAY_SUCCESS));
            if (questState.get("priest1") == null) {
                questState.set("priest1", npcId);
                questState.setCond(2);
            } else if (questState.get("priest2") == null) {
                questState.set("priest2", npcId);
                questState.setCond(3);
            } else if (questState.get("priest3") == null) {
                questState.set("priest3", npcId);
                questState.setCond(4);
            }
            PRAY_BUFFS.get(npcId).applyEffects(player, player);
            questState.getPlayer().setUncontrollable(false);
            questState.removeTask(PRAY_TASK_NAME);
        }

        @Override
        public void run() {
            chance.addAndGet(counter.incrementAndGet());
            if (Rnd.calcChance(chance.get(), 100)) {
                onFinish();
            } else {
                player.sendMessage(Rnd.get(PRAY_TEXT));
            }
        }

    }

    private String generateHtml(int npcId, String add) {
        return npcId + add + ".htm";
    }
}
