package rocks.gravili.notquestsspigot.events.hooks;

import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.objectives.hooks.jobsreborn.JobsRebornReachJobLevel;
import rocks.gravili.notquestsspigot.structs.ActiveObjective;
import rocks.gravili.notquestsspigot.structs.ActiveQuest;
import rocks.gravili.notquestsspigot.structs.QuestPlayer;

public class JobsRebornEvents implements Listener {
    private final NotQuests main;

    public JobsRebornEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onJobsLevelUp(JobsLevelUpEvent e) {
        if (!e.isCancelled()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof JobsRebornReachJobLevel jobsRebornReachJobLevel) {
                                    if (!e.getJob().getName().equalsIgnoreCase(jobsRebornReachJobLevel.getJobName())) {
                                        continue;
                                    }
                                    activeObjective.addProgress(1);
                                }
                            }
                        }
                        activeQuest.removeCompletedObjectives(true);
                    }
                    questPlayer.removeCompletedQuests();
                }
            }
        }
    }


}
