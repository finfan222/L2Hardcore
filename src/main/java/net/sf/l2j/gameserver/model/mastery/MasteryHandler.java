package net.sf.l2j.gameserver.model.mastery;

/**
 * @author finfan
 */
public interface MasteryHandler {

    void onLearn(Mastery mastery, MasteryData masteryData);

    void onUnlearn(Mastery mastery, MasteryData masteryData);

}
