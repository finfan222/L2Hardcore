package net.sf.l2j.gameserver.model.mastery;

import lombok.Getter;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.mastery.handlers.AegisOfWarMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.BladeThatBanishesEvilMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.CitadelMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.CrusaderismMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.FaithCureMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.HolyLightMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.HolyResurrectionMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.HolyVengeanceMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.LinkedHealMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.MasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.ProtectorateMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.ShieldsmanMasteryHandler;
import net.sf.l2j.gameserver.model.mastery.handlers.WeaponerMasteryHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author finfan
 */
@Getter
public enum MasteryType {

    CITADEL(new CitadelMasteryHandler(), 20, "Позволяет вам лучше понимать обращение вашей экипировкой тяжелой брони в бою. Стратегия позволяет значительно снижать получаемый урон.", ClassId.KNIGHT, ClassId.ELVEN_KNIGHT, ClassId.PALUS_KNIGHT),
    SHIELDSMAN(new ShieldsmanMasteryHandler(), 20, "Раскрыт потенциал профессионального владения щитами. Вы обрели возможность использования навыка 'Barricade', который снижает весь получаемый урон до 0 если атака была сблокирована щитом.", ClassId.KNIGHT, ClassId.ELVEN_KNIGHT, ClassId.PALUS_KNIGHT),
    WEAPONER(new WeaponerMasteryHandler(), 20, "Вы лучше управляетесь с оружием ближнего боя. Теперь вы можете использовать его скрытые преимущества.", ClassId.KNIGHT, ClassId.WARRIOR, ClassId.PALUS_KNIGHT, ClassId.ORC_RAIDER, ClassId.ARTISAN),
    LIVING_ARMOUR(null, 30, "'Абсолютная защита' больше не останавливает вас, теперь, вы способны передвигаться, а значит - на гораздо большие подвиги.", ClassId.KNIGHT, ClassId.ELVEN_KNIGHT, ClassId.PALUS_KNIGHT),
    RESISTANCE(null, 30, "Раскрыты пропорции магических элементов, вы обрели способность сопротивляться любой магии и ее эффекту с некоторой вероятностью.", ClassId.KNIGHT, ClassId.ELVEN_KNIGHT),
    VETERAN(null, 30, "Вы узнали как лучше подставлять \"плечо\" под удар, в связи с чем, ваша броня будет иметь минимальные показатели поломки и максимальную эффективность в обороне.", ClassId.KNIGHT, ClassId.WARRIOR),
    HOLY_VENGEANCE(new HolyVengeanceMasteryHandler(), 40, "Состояние святого отмщения выпадает карой на тех, кто попытается вам навредить, оно напрямую зависит от эффективности вашего заклинания Holy Strike. С некоторой периодичностью, враги будут наказаны. Но нужно быть на чеку, совершая зло - вы сами рискуете обрести наказание.", ClassId.PALADIN),
    CRUSADERISM(new CrusaderismMasteryHandler(), 40, "Вы присягнули на верность святости Эйнхасад и она наделила вас обетом крестоносца Эйнхасад - ваши боевые качества будут расти с ростом вашего уровня.", ClassId.PALADIN),
    PROVOKING_BALAMB(null, 40, "Ваши провокации становятся более действенными и чувствительными против врагов, их воля ломается, из-за чего они будут атаковать только вас.", ClassId.PALADIN, ClassId.TEMPLE_KNIGHT, ClassId.SHILLIEN_KNIGHT),
    BLADE_THAT_BANISHES_EVIL(new BladeThatBanishesEvilMasteryHandler(), 50, "Раскрыт потенциал уничтожения зла - абсолютной силой. Вы будете ощущать как силой света Эйнхасад - заряжается ваше оружие. Такой клинок способен поразить любое зло - нанеся ему сокрушительные повреждения или даже уничтожив его с одного удара.", ClassId.PALADIN),
    HOLY_LIGHT(new HolyLightMasteryHandler(), 50, "Вы познали тайны исцеления как никто другой. Все заклинания применяющие эффект исцеления - стали более эффективнее и могут иметь критически-положительный эффект. Несите свет в этот темный мир.", ClassId.PALADIN, ClassId.BISHOP, ClassId.ELVEN_ORACLE, ClassId.TEMPLE_KNIGHT),
    AEGIS_OF_WAR(new AegisOfWarMasteryHandler(), 50, "Ваше владение круговой защитой от Aegis Stance становится абсолютным. Теперь при активном навыке, при блокировании атак, враг может быть оглушен на короткий промежуток времени.", ClassId.PALADIN),
    LINKED_HEAL(new LinkedHealMasteryHandler(), 60, "Ваши навыки исцеления достигают новых вершин. Теперь, вы способны исцеляя цель - исцелять и себя без дополнительных затрат маны.", ClassId.PALADIN, ClassId.BISHOP),
    HOLY_RESURRECT(new HolyResurrectionMasteryHandler(), 60, "Вы овладели заклинанием воскрешения, которое способно поднять мертвого к жизни вернув ему утерянный опыт и восстановив все запасы энергии.", ClassId.PALADIN),
    PROTECTORATE(new ProtectorateMasteryHandler(), 60, "Великий навык защиты и обороны, показатель мастерства спасения союзников. Теперь, вы обладаете способностью укрывать членов своей группы под собственной протекцией - не позволяя взять их в целевой статус пока они находятся рядом с вами.", ClassId.PALADIN),
    FAITH_CURE(new FaithCureMasteryHandler(), 70, "Когда вы ощущаете боль, ее ощущает и Эйнхасад. Чтобы исцелить раны, ваша вера способна создавать заклинание, которое будет регенерировать повреждения каждый раз, когда порог боли будет превзойден."),
    PALADINS_WRATH(null, 70, "Истинная мощь обретаемая, когда смерть подходит к вам в упор."),
    /**
     * Игрок получает способность, которая при активации перевоплощает его на глазах у всех в броню Apella Set. С этого момента игрок получает бафф эффект, который позволяет НЕ получать урон, а накапливать его.
     */
    KADMUS_ARMOR(null, 80, "Вы обрели тайное искусство рыцарей Элморадена, и теперь можете воззвать к броне Кадмуса, чтобы усилить весь свой боевой потенциал. Ваша броня трансформируется и поглощает весь входящий урон. Каждая ваша следующая атака, будет включать в себя и дополнительное увеличение в размере накопленного урона вашей броней.", ClassId.PHOENIX_KNIGHT, ClassId.EVAS_TEMPLAR),
    PRESSURE_SLAM(null, 80, "Навык владения Shield Slam способностью становится абсолютным и теперь он способен накладывать молчание, если вы попадает им в момент каста заклинания жертвы дополнительно снимая положительный эффект с нее.", ClassId.PHOENIX_KNIGHT, ClassId.HELL_KNIGHT),
    ONE_WITH_THE_PHOENIX(null, 80, "Возрождение из пепла под действием Soul of the Phoenix больше не снимается при перерождении, однако вы всегда теряете здоровья, обжигаясь жаром-благословения могущественной птицы.", ClassId.PHOENIX_KNIGHT)
    ;

    private final MasteryHandler handler;
    private final int requiredLevel;
    private final String description;
    private final ClassId[] classes;

    MasteryType(MasteryHandler handler, int requiredLevel, String description, ClassId... classes) {
        this.handler = handler;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.classes = classes;
    }

    public String getCapitalizedName() {
        return StringUtils.capitalize(name());
    }

    public boolean isPassive() {
        return handler == null;
    }

    public boolean isRequiredClass(ClassId classId) {
        return ArrayUtils.contains(classes, classId);
    }
}
