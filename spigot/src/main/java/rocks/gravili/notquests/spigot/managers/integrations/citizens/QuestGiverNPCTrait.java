/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.spigot.managers.integrations.citizens;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.conversation.Conversation;
import rocks.gravili.notquests.spigot.structs.Quest;

/**
 * This handles the QuestGiver NPC Trait which is given directly to Citizens NPCs via their API.
 * A lot of things from this ugly class have been copy-and-pasted from their wiki.
 * <p>
 * Only NPCs which have quests attached to them should have this trait. There are several methods in the plugin which remove the trait
 * from NPCs which do not have Quests stored on them - for example that cleanup runs when the plugin restarts.
 * <p>
 * Note: TODO: The available Quests are not stored in the NPC directly. Instead, each quest object stores the NPC. Because of this, it has to loop through. This should be improved in the future for better performance.
 *
 * @author Alessio Gravili
 */
public class QuestGiverNPCTrait extends Trait {

    NotQuests main = null;

    private int particleTimer = 0;

    public QuestGiverNPCTrait() {
        super("nquestgiver");
        this.main = NotQuests.getInstance();
    }

    // Here you should load up any values you have previously saved (optional).
    // This does NOT get called when applying the trait for the first time, only loading onto an existing npc at server start.
    // This is called AFTER onAttach so you can load defaults in onAttach and they will be overridden here.
    // This is called BEFORE onSpawn, npc.getEntity() will return null.
    public void load(DataKey key) {
        //SomeSetting = key.getBoolean("SomeSetting", false);
    }

    // Save settings for this NPC (optional). These values will be persisted to the Citizens saves file
    public void save(DataKey key) {
        // key.setInt("SomeSetting",SomeSetting);
    }

    /**
     * Called when a player clicks on the NPC. This will send the quest preview GUI / Text to the player, which lists
     * all available Quests for this NPC.
     * <p>
     * The available Quests are not stored in the NPC directly. Instead, each quest object stores the NPC. Because of this, it has
     * to loop through. This should be improved in the future for better performance.
     */
    @EventHandler
    public void click(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        //Handle a click on a NPC. The event has a getNPC() method.
        //Be sure to check event.getNPC() == this.getNPC() so you only handle clicks on this NPC!
        //npc.getTrait(FollowTrait.class) new FollowTrait();
        if (event.getNPC() == this.getNPC()) {
            final Player player = event.getClicker();
            main.getQuestManager().sendQuestsPreviewOfQuestShownNPCs(getNPC(), player);

            //Conversations
            final Conversation foundConversation = main.getConversationManager().getConversationForNPCID(getNPC().getId());
            if (foundConversation != null) {
                main.getConversationManager().playConversation(player, foundConversation);
            }
        }

    }

    /**
     * Called every tick. This spawns a particle above an NPCs head which has this Trait, showcasing to the player
     * that they have Quests and can be clicked.
     */
    @Override
    public void run() {

        //Disable if Server TPS is too low
        final double minimumTPS = main.getDataManager().getConfiguration().getCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow();
        if (minimumTPS >= 0) {
            if (main.getPerformanceManager().getTPS() < minimumTPS) {
                return;
            }
        }

        if (main.getDataManager().getConfiguration().isCitizensNPCQuestGiverIndicatorParticleEnabled() && npc.isSpawned()) {
            if (particleTimer >= main.getDataManager().getConfiguration().getCitizensNPCQuestGiverIndicatorParticleSpawnInterval()) {
                particleTimer = 0;

                final Entity npcEntity = getNPC().getEntity();
                final Location location = npcEntity.getLocation();

                npcEntity.getWorld().spawnParticle(main.getDataManager().getConfiguration().getCitizensNPCQuestGiverIndicatorParticleType(), location.getX() - 0.25 + (Math.random() / 2), location.getY() + 1.75 + (Math.random() / 2), location.getZ() - 0.25 + (Math.random() / 2), main.getDataManager().getConfiguration().getCitizensNPCQuestGiverIndicatorParticleCount());

                //System.out.println("§eSpawned particle!");
            }

            particleTimer += 1;

        }

    }


    /**
     * Run code when your trait is attached to a NPC. This is called BEFORE onSpawn, so npc.getEntity() will return null
     * This will just splurt out a debug message, so we know when the trait has been attached to the NPC.
     */
    @Override
    public void onAttach() {
        main.getLogManager().info("NPC with the ID <AQUA>" + npc.getId() + "</AQUA> and name <AQUA>" + npc.getName() + "</AQUA> has been assigned the Quest Giver trait!");
    }

    // Run code when the NPC is despawned. This is called before the entity actually despawns so npc.getEntity() is still valid.
    @Override
    public void onDespawn() {
    }

    //Run code when the NPC is spawned. Note that npc.getEntity() will be null until this method is called.
    //This is called AFTER onAttach and AFTER Load when the server is started.
    @Override
    public void onSpawn() {

    }

    /**
     * Run code when the NPC is removed. This will also remove from the Quest object that the NPC is attached to it - since the NPC does not exist anymore. Otherwise,
     * the plugin would try giving a non-existent NPC the NPC trait.
     * <p>
     * This method is not called when the NPC is just despawned, but when it's completely removed and thus won't exist anymore.
     */
    @Override
    public void onRemove() {
        //REMOVEEEE FROM QUEST
        main.getLogManager().info("NPC with the ID <AQUA>" + npc.getId() + " </AQUA>and name <AQUA>" + npc.getName() + " </AQUA>has been removed!");
        for (Quest quest : main.getQuestManager().getAllQuestsAttachedToNPC(getNPC())) {
            quest.removeNPC(getNPC());
        }
    }

}