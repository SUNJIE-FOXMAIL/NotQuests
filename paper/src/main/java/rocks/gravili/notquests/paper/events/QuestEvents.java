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

package rocks.gravili.notquests.paper.events;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;
import rocks.gravili.notquests.paper.events.notquests.other.PlayerJumpEvent;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.*;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.WorldEnterTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.WorldLeaveTrigger;

import java.util.Locale;

import static rocks.gravili.notquests.paper.commands.NotQuestColors.debugHighlightGradient;


public class QuestEvents implements Listener {
    private final NotQuests main;


    public QuestEvents(NotQuests main) {
        this.main = main;
    }



    @EventHandler
    private void onSmeltEvent(InventoryClickEvent e) {
        final Entity entity = e.getWhoClicked();
        if (entity instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final SmeltObjective smeltObjective) {
                                    final InventoryType inventoryType = e.getInventory().getType();


                                    // questPlayer.sendDebugMessage("InventoryType: " + inventoryType.name());
                                    //questPlayer.sendDebugMessage("CurrentItem type: " + e.getCurrentItem().getType());
                                    //questPlayer.sendDebugMessage("View Type: " + e.getView().getType().name());


                                    if(inventoryType != InventoryType.FURNACE && inventoryType != InventoryType.BLAST_FURNACE && inventoryType != InventoryType.SMOKER){
                                        continue;
                                    }


                                    final ItemStack currentItem = e.getCurrentItem();
                                    if(main.getUtilManager().isItemEmpty(currentItem)){
                                        //questPlayer.sendDebugMessage("Invalid item for smelt objective (1)");
                                        continue;
                                    }

                                    if (!smeltObjective.isSmeltAnyItem() && !currentItem.isSimilar(smeltObjective.getItemToSmelt())) {
                                        //questPlayer.sendDebugMessage("Invalid item for smelt objective (2). CurrentItem: " + currentItem.getType().name() + " ItemToSmelt: " + smeltObjective.getItemToSmelt().getType().name());
                                        continue;
                                    }

                                    questPlayer.sendDebugMessage("Valid item for smelt objective");


                                    int amount = currentItem.getAmount();
                                    final ItemStack cursor = e.getCursor();



                                    switch (e.getClick()) {
                                        case LEFT:
                                            if (!main.getUtilManager().isItemEmpty(cursor)) {
                                                questPlayer.sendDebugMessage("Inventory craft event: Cursor is not empty");

                                                if (!cursor.isSimilar(currentItem)) {
                                                    amount = 0;
                                                }
                                                if (cursor.getAmount() + currentItem.getAmount() > cursor.getMaxStackSize()) {
                                                    amount = 0;
                                                }
                                            }
                                            break;

                                        case RIGHT:
                                            if (!main.getUtilManager().isItemEmpty(cursor)) {
                                                questPlayer.sendDebugMessage("Inventory craft event: Cursor is not empty");

                                                if (!cursor.isSimilar(currentItem)) {
                                                    amount = 0;
                                                }
                                                if (cursor.getAmount() + currentItem.getAmount() > cursor.getMaxStackSize()) {
                                                    amount = 0;
                                                }
                                            }
                                            amount = (amount+1)/2;
                                            break;
                                        case NUMBER_KEY:
                                            //If the hotbar is full, the item will not be crafted but it will still trigger this event for some reason. That's
                                            //why we manually have to set the amount to 0 here
                                            if (player.getInventory().getItem(e.getHotbarButton()) != null) {
                                                amount = 0;
                                            }
                                            break;

                                        case DROP:
                                            if (!main.getUtilManager().isItemEmpty(cursor)) {
                                                amount = 0;
                                            }
                                            amount = 1;
                                            break;
                                        case CONTROL_DROP:
                                            // If we are holding items, craft-via-drop fails (vanilla behavior)
                                            // Cursor is either null or AIR
                                            if (!main.getUtilManager().isItemEmpty(cursor)) {
                                                amount = 0;
                                            }

                                            break;
                                        case SWAP_OFFHAND:
                                            if(!main.getUtilManager().isItemEmpty(player.getInventory().getItemInOffHand())){
                                                amount = 0;
                                            }
                                            break;
                                        case SHIFT_LEFT:
                                        case SHIFT_RIGHT:
                                            if (amount == 0) {
                                                break;
                                            }

                                            amount = Math.min(getInventorySpaceLeftForItem(player.getInventory(), currentItem ) ,amount);

                                            break;
                                        default:
                                            amount = 0;
                                    }


                                    questPlayer.sendDebugMessage("Amount: " + amount);

                                    if (amount == 0) {
                                        continue;
                                    }


                                    activeObjective.addProgress(amount);


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



    public final int getInventorySpaceLeftForItem(final Inventory inventory, final ItemStack item) {
        int remaining = 0;
        for (final ItemStack itemStack : inventory.getStorageContents()) {
            if(main.getUtilManager().isItemEmpty(itemStack)){
                remaining += item.getMaxStackSize();
            }else{
                if(itemStack.isSimilar(item)){
                    remaining += item.getMaxStackSize() - itemStack.getAmount();
                }
            }

        }
        return remaining;
    }


    @EventHandler
    private void onCraftItemEvent(CraftItemEvent e) {
        final Entity entity = e.getWhoClicked();
        if (entity instanceof final Player player && e.getInventory().getResult() != null) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final CraftItemsObjective craftItemsObjective) {
                                    final ItemStack result = e.getRecipe().getResult();
                                    final ItemStack cursor = e.getCursor();

                                    //Check if the Material of the crafted item is equal to the Material needed in the CraftItemsObjective
                                    if (!craftItemsObjective.isCraftAnyItem() && !(craftItemsObjective.getItemToCraft().getType() == result.getType())) {
                                        continue;
                                    }

                                    //If the objectiv-item which needs to be crafted has an ItemMeta...
                                    if (!craftItemsObjective.isCraftAnyItem() && craftItemsObjective.getItemToCraft().getItemMeta() != null) {
                                        //then check if the ItemMeta of the crafted item is equal to the ItemMeta needed in the CraftItemsObjective
                                        if (!craftItemsObjective.getItemToCraft().getItemMeta().equals(result.getItemMeta())) {
                                            continue;
                                        }
                                    }

                                    questPlayer.sendDebugMessage("Inventory craft event. Click type: " + debugHighlightGradient + e.getClick().name() + "</gradient>");


                                    //Now we gotta figure out the real amount of items which have been crafted, which is trickier than expected:


                                    int recipeAmount = getCraftAmount(result, cursor, e.getClick(), e.getWhoClicked(), e.getHotbarButton(), e.getInventory(), e.getView(), questPlayer);


                                    // No use continuing if we haven't actually crafted a thing
                                    if (recipeAmount == 0) {
                                        continue;
                                    }


                                    activeObjective.addProgress(recipeAmount);


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

    public final int getCraftAmount(final ItemStack result, final ItemStack cursor, final ClickType click, final HumanEntity whoClicked, final int hotbarButton, final CraftingInventory craftingInventory, final InventoryView inventoryView, final QuestPlayer questPlayer){

        int recipeAmount = result.getAmount();

        switch (click) {
            case LEFT:
            case RIGHT:
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    questPlayer.sendDebugMessage("Inventory craft event: Cursor is not empty");

                    if (!cursor.isSimilar(result)) {
                        recipeAmount = 0;
                    }
                    if (cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                        recipeAmount = 0;
                    }
                }
                break;
            case NUMBER_KEY:
                //If the hotbar is full, the item will not be crafted but it will still trigger this event for some reason. That's
                //why we manually have to set the amount to 0 here
                if (whoClicked.getInventory().getItem(hotbarButton) != null) {
                    recipeAmount = 0;
                }
                break;

            case DROP:
            case CONTROL_DROP:
                // If we are holding items, craft-via-drop fails (vanilla behavior)
                // Cursor is either null or AIR
                if (!main.getUtilManager().isItemEmpty(cursor)) {
                    recipeAmount = 0;
                }

                break;

            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (recipeAmount == 0) {
                    break;
                }

                int maxCraftable = getMaxCraftAmount(craftingInventory);
                int capacity = fits(result, inventoryView.getBottomInventory());

                // If we can't fit everything, increase "space" to include the items dropped by
                // crafting
                // (Think: Uncrafting 8 iron blocks into 1 slot)
                if (capacity < maxCraftable) {
                    maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;
                }
                recipeAmount = maxCraftable;
                break;
            case SWAP_OFFHAND:
                if(!main.getUtilManager().isItemEmpty(whoClicked.getInventory().getItemInOffHand())){
                    recipeAmount = 0;
                }
                break;
            default:
                recipeAmount = 0;
        }

        return recipeAmount;
    }


    private int getMaxCraftAmount(CraftingInventory inv) {
        if (inv.getResult() == null)
            return 0;

        int resultCount = inv.getResult().getAmount();
        int materialCount = Integer.MAX_VALUE;

        for (ItemStack is : inv.getMatrix())
            if (is != null && is.getAmount() < materialCount)
                materialCount = is.getAmount();

        return resultCount * materialCount;
    }

    private int fits(ItemStack stack, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        int result = 0;

        for (ItemStack is : contents)
            if (is == null)
                result += stack.getMaxStackSize();
            else if (is.isSimilar(stack))
                result += Math.max(stack.getMaxStackSize() - is.getAmount(), 0);

        return result;
    }






    @EventHandler(ignoreCancelled = true)
    public void onPlayerJump(final PlayerJumpEvent e) {

        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer == null) {
            return;
        }
        if (questPlayer.getActiveQuests().size() == 0) {
            return;
        }

        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()) {
                    if (activeObjective.getObjective() instanceof JumpObjective) {
                        activeObjective.addProgress(1);
                    }
                }

            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }


    @EventHandler
    public void interactEvent(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer == null) {
            return;
        }
        if (questPlayer.getActiveQuests().size() == 0) {
            return;
        }


        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()) {
                    if (activeObjective.getObjective() instanceof InteractObjective interactObjective) {
                        String materialName = "AIR";
                        if (e.getClickedBlock() != null) {
                            materialName = e.getClickedBlock().getBlockData().getMaterial().name();
                        }
                        questPlayer.sendDebugMessage("Found InteractObjective Objective in PlayerInteractEvent. Clicked Block material: <highlight>" + materialName
                                + "</highlight>. Action: <highlight2>" + e.getAction() + "</highlight2>."
                        );

                        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !interactObjective.isRightClick()) {
                            return;
                        }
                        if (e.getAction() == Action.LEFT_CLICK_BLOCK && !interactObjective.isLeftClick()) {
                            return;
                        }
                        if (e.getClickedBlock() == null || e.getClickedBlock().getLocation().getWorld() == null || interactObjective.getLocationToInteract().getWorld() == null) {
                            return;
                        }

                        if (!e.getClickedBlock().getLocation().getWorld().getName().equalsIgnoreCase(interactObjective.getLocationToInteract().getWorld().getName())) {
                            return;
                        }
                        if (e.getClickedBlock().getLocation().distance(interactObjective.getLocationToInteract()) > interactObjective.getMaxDistance()) {
                            return;
                        }

                        activeObjective.addProgress(1);
                        if (interactObjective.isCancelInteraction()) {
                            e.setCancelled(true);
                        }

                    }
                }

            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer == null) {
            return;
        }
        if (questPlayer.getActiveQuests().size() == 0) {
            return;
        }

        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()) {
                    if (activeObjective.getObjective() instanceof RunCommandObjective runCommandObjective) {
                        questPlayer.sendDebugMessage("Found RunCommand Objective in PlayerCommandPreprocessEvent. Command: <highlight>" + e.getMessage()
                                + "</highlight> Objective command to run: <highlight2>" + runCommandObjective.getCommandToRun() + "</highlight2>."
                        );

                        if (runCommandObjective.isIgnoreCase() && !e.getMessage().equalsIgnoreCase(runCommandObjective.getCommandToRun())) {
                            continue;
                        }
                        if (!runCommandObjective.isIgnoreCase() && !e.getMessage().equals(runCommandObjective.getCommandToRun())) {
                            continue;
                        }

                        activeObjective.addProgress(1);
                        if (runCommandObjective.isCancelCommand()) {
                            e.setCancelled(true);
                        }

                    }
                }

            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();


    }


    @EventHandler
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger() instanceof WorldEnterTrigger worldEnterTrigger) {
                            if (e.getPlayer().getWorld().getName().equals(worldEnterTrigger.getWorldToEnterName())) {
                                handleGeneralTrigger(questPlayer, activeTrigger);

                            }

                        } else if (activeTrigger.getTrigger() instanceof WorldLeaveTrigger worldLeaveTrigger) {
                            if (e.getFrom().getName().equals(worldLeaveTrigger.getWorldToLeaveName())) {
                                handleGeneralTrigger(questPlayer, activeTrigger);
                            }

                        }
                    }


                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    private void onEntityBreed(EntityBreedEvent e) {
        if (!e.isCancelled()) {
            if (e.getBreeder() instanceof final Player player) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
                if (questPlayer == null) {
                    return;
                }
                if (questPlayer.getActiveQuests().size() == 0) {
                    return;
                }

                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof BreedObjective breedObjective) {
                                if(breedObjective.getEntityToBreedType().equalsIgnoreCase("any") ||  breedObjective.getEntityToBreedType().equalsIgnoreCase(e.getEntityType().toString())){
                                    activeObjective.addProgress(1);
                                }

                            }
                        }

                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();

            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            final Player player = e.getPlayer();
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof BreakBlocksObjective breakBlocksObjective) {
                                    if (breakBlocksObjective.getBlockToBreak().equalsIgnoreCase("any") || breakBlocksObjective.getBlockToBreak().equalsIgnoreCase(e.getBlock().getType().name())) {
                                        activeObjective.addProgress(1);
                                    }
                                } else if (activeObjective.getObjective() instanceof PlaceBlocksObjective placeBlocksObjective) { //Deduct if Block is Broken for PlaceBlocksObjective
                                    if (placeBlocksObjective.getBlockToPlace().equalsIgnoreCase("any") || placeBlocksObjective.getBlockToPlace().equalsIgnoreCase(e.getBlock().getType().name())) {
                                        if (placeBlocksObjective.isDeductIfBlockBroken()) {
                                            activeObjective.removeProgress(1, false);
                                        }
                                    }
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

    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockPlace(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            final Player player = e.getPlayer();
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                //This is for the BreakBlocksObjective. It should deduct the progress if the player placed the same block again (if willDeductIfBlockPlaced() is set to true)
                                if (activeObjective.getObjective() instanceof BreakBlocksObjective breakBlocksObjective) {
                                    if (breakBlocksObjective.getBlockToBreak().equalsIgnoreCase("any") || breakBlocksObjective.getBlockToBreak().equalsIgnoreCase(e.getBlock().getType().name())) {
                                        if (breakBlocksObjective.isDeductIfBlockPlaced()) {
                                            activeObjective.removeProgress(1, false);
                                        }
                                    }
                                } else if (activeObjective.getObjective() instanceof PlaceBlocksObjective placeBlocksObjective) {
                                    if (placeBlocksObjective.getBlockToPlace().equalsIgnoreCase("any") || placeBlocksObjective.getBlockToPlace().equalsIgnoreCase(e.getBlock().getType().name())) {
                                        activeObjective.addProgress(1);
                                    }
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


    @EventHandler
    private void onPickupItemEvent(EntityPickupItemEvent e) {
        final Entity entity = e.getEntity();
        if (entity instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final CollectItemsObjective collectItemsObjective) {


                                    //Check if the Material of the collected item is equal to the Material needed in the CollectItemsObjective
                                    if (!collectItemsObjective.isCollectAnyItem() && !(collectItemsObjective.getItemToCollect().getType() == e.getItem().getItemStack().getType())) {
                                        continue;
                                    }

                                    //If the objective-item which needs to be collected has an ItemMeta...
                                    if (!collectItemsObjective.isCollectAnyItem() && collectItemsObjective.getItemToCollect().getItemMeta() != null) {
                                        //then check if the ItemMeta of the collected item is equal to the ItemMeta needed in the CollectItemsObjective
                                        if (!collectItemsObjective.getItemToCollect().getItemMeta().equals(e.getItem().getItemStack().getItemMeta())) {
                                            continue;
                                        }
                                    }

                                    activeObjective.addProgress(e.getItem().getItemStack().getAmount());

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


    @EventHandler
    private void onDropItemEvent(PlayerDropItemEvent e) { //DEFAULT ENABLED FOR ITEM DROPS UNLIKE FOR BLOCK BREAKS
        final Entity player = e.getPlayer();

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof final CollectItemsObjective collectItemsObjective) {
                                if (!collectItemsObjective.isDeductIfItemIsDropped()) {
                                    continue;
                                }

                                //Check if the Material of the collected item is equal to the Material needed in the CollectItemsObjective
                                if (!collectItemsObjective.isCollectAnyItem() && !(collectItemsObjective.getItemToCollect().getType() == e.getItemDrop().getItemStack().getType())) {
                                    continue;
                                }

                                //If the objective-item which needs to be collected has an ItemMeta...
                                if (!collectItemsObjective.isCollectAnyItem() && collectItemsObjective.getItemToCollect().getItemMeta() != null) {
                                    //then check if the ItemMeta of the collected item is equal to the ItemMeta needed in the CollectItemsObjective
                                    if (!collectItemsObjective.getItemToCollect().getItemMeta().equals(e.getItemDrop().getItemStack().getItemMeta())) {
                                        continue;
                                    }
                                }

                                activeObjective.removeProgress(e.getItemDrop().getItemStack().getAmount(), false);

                            }
                        }

                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();
            }
        }


    }


    @EventHandler
    private void onEntityDeath(EntityDeathEvent e) { //KillMobs objectives & Death triggers

        //Death Triggers
        if (e.getEntity() instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {

                    //Iterator<ActiveQuest> iter = questPlayer.getActiveQuests().iterator(); //Why was that needed?

                    for (int i = 0; i < questPlayer.getActiveQuests().size(); i++) {
                        final ActiveQuest activeQuest = questPlayer.getActiveQuests().get(i);
                        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                            if (activeTrigger.getTrigger().getTriggerType().equals("DEATH")) {
                                handleGeneralTrigger(questPlayer, activeTrigger);

                            }
                        }
                    }


                }
            }
        }


        //KillMobs objectives
        final Player player = e.getEntity().getKiller();
        if (player != null) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.getObjective() instanceof KillMobsObjective killMobsObjective) {
                                if (activeObjective.isUnlocked()) {
                                    final EntityType killedMob = e.getEntity().getType();
                                    if (killMobsObjective.getMobToKill().equalsIgnoreCase("any") || killMobsObjective.getMobToKill().equalsIgnoreCase(killedMob.toString())) {
                                        if (e.getEntity() != e.getEntity().getKiller()) { //Suicide prevention

                                            //Extra Flags
                                            if (!killMobsObjective.getNameTagContainsAny().isBlank()) {
                                                if (e.getEntity().getCustomName() == null || e.getEntity().getCustomName().isBlank()) {
                                                    continue;
                                                }
                                                boolean foundOneNotFitting = false;
                                                for (final String namePart : killMobsObjective.getNameTagContainsAny().toLowerCase(Locale.ROOT).split(" ")) {
                                                    if (!e.getEntity().getCustomName().toLowerCase(Locale.ROOT).contains(namePart)) {
                                                        foundOneNotFitting = true;
                                                    }
                                                }
                                                if (foundOneNotFitting) {
                                                    continue;
                                                }
                                            }
                                            if (!killMobsObjective.getNameTagEquals().isBlank()) {
                                                if (e.getEntity().getCustomName() == null || e.getEntity().getCustomName().isBlank() || !e.getEntity().getCustomName().equalsIgnoreCase(killMobsObjective.getNameTagEquals())) {
                                                    continue;
                                                }
                                            }

                                            activeObjective.addProgress(1);
                                        }

                                    }
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

    @EventHandler
    private void onConsumeItemEvent(PlayerItemConsumeEvent e) { //DEFAULT ENABLED FOR ITEM DROPS UNLIKE FOR BLOCK BREAKS
        final Player player = e.getPlayer();

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.getObjective() instanceof ConsumeItemsObjective consumeItemsObjective) {
                            if (activeObjective.isUnlocked()) {

                                //Check if the Material of the consumed item is equal to the Material needed in the ConsumeItemsObjective
                                if (!consumeItemsObjective.isConsumeAnyItem() && !(consumeItemsObjective.getItemToConsume().getType() == e.getItem().getType())) {
                                    continue;
                                }

                                //If the objective-item which needs to be crafted has an ItemMeta...
                                if (!consumeItemsObjective.isConsumeAnyItem() && consumeItemsObjective.getItemToConsume().getItemMeta() != null) {
                                    //then check if the ItemMeta of the consumed item is equal to the ItemMeta needed in the ConsumeItemsObjective
                                    if (!consumeItemsObjective.getItemToConsume().getItemMeta().equals(e.getItem().getItemMeta())) {
                                        continue;
                                    }
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


    @EventHandler
    private void onDisconnectEvent(PlayerQuitEvent e) { //Disconnect objectives
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType().equals("DISCONNECT")) {
                            handleGeneralTrigger(questPlayer, activeTrigger);
                        }
                    }


                }
            }
        }
    }




    /**
     * This method handles the most commonly used type of trigger, which should simply add to the progress.
     * Apart from adding the progress, this method checks for the triggers applyOn and the triggers worldName
     *
     * @param questPlayer   is the QuestPlayer object, used to check the world of the player
     * @param activeTrigger is the trigger which we need in order to add progress to it
     */
    private void handleGeneralTrigger(final QuestPlayer questPlayer, final ActiveTrigger activeTrigger) {

        //Handle Trigger applyOn
        if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Trigger applies to a specific objective of the Quest and not the Quest itself
            //Get the active Objective for which the trigger applies to
            final ActiveObjective activeObjective = activeTrigger.getActiveQuest().getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
            //Return, if the active objective which the trigger needs doesn't exist or is not yet unlocked (so hidden)
            if (activeObjective == null || !activeObjective.isUnlocked()) {
                return;
            }
        }

        //Handle Trigger World Name
        if (!activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
            final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
            //If the player is not in the world which the Trigger needs, cancel.
            if (qPlayer == null || !qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                return;
            }
        }

        //Finally, we can add to the trigger and check if it can trigger now if the progress is full
        activeTrigger.addAndCheckTrigger(activeTrigger.getActiveQuest());


    }

    //For ReachLocation
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!main.getConfiguration().isMoveEventEnabled()) {
            return;
        }


        if (e.getTo() == null) {
            return;
        }

        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockY() != e.getTo().getBlockY() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            checkIfInReachLocation(e, e.getTo());
        }

    }

    public void checkIfInReachLocation(final PlayerMoveEvent e, final Location currentLocation) {
        if (!e.isCancelled()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final ReachLocationObjective reachLocationObjective) {

                                    final Location minLocation = reachLocationObjective.getMinLocation();
                                    if (minLocation.getWorld() != null && currentLocation.getWorld() != null && !currentLocation.getWorld().equals(minLocation.getWorld())) {
                                        continue;
                                    }
                                    final Location maxLocation = reachLocationObjective.getMaxLocation();
                                    if (currentLocation.getX() >= minLocation.getX() && currentLocation.getX() <= maxLocation.getX()) {
                                        if (currentLocation.getZ() >= minLocation.getZ() && currentLocation.getZ() <= maxLocation.getZ()) {
                                            if (currentLocation.getY() >= minLocation.getY() && currentLocation.getY() <= maxLocation.getY()) {
                                                activeObjective.addProgress(1);
                                            }
                                        }
                                    }
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


    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPluginEnable(final PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("MythicMobs") && !main.getIntegrationsManager().isMythicMobsEnabled()) {
            // Turn on support for the plugin
            main.getIntegrationsManager().enableMythicMobs();
        } else if (event.getPlugin().getName().equals("Citizens") && !main.getIntegrationsManager().isCitizensEnabled()) {
            // Turn on support for the plugin
            main.getIntegrationsManager().enableCitizens();
        }

    }


    @EventHandler
    public void playerChatEvent(PlayerCommandPreprocessEvent e) {
        if (e.getMessage().startsWith("/notquests continueConversation ")) {
            final Player player = e.getPlayer();
            if (player.hasPermission("notquests.use")) {
                handleConversation(player, e.getMessage().split("/notquests continueConversation ")[1]);
                e.setCancelled(true);
            }
        }
    }

    private void handleConversation(Player player, String option) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId()) == null) {
            return;
        }
        //Check if the player has an open conversation
        final ConversationPlayer conversationPlayer = main.getConversationManager().getOpenConversation(player.getUniqueId());
        if (conversationPlayer != null) {
            conversationPlayer.chooseOption(option);
        } else {
            questPlayer.sendDebugMessage("Tried to choose conversation option, but the conversationPlayer was not found! Active conversationPlayers count: <highlight>" + main.getConversationManager().getOpenConversations().size());
            questPlayer.sendDebugMessage("All active conversationPlayers: <highlight>" + main.getConversationManager().getOpenConversations().toString());
            questPlayer.sendDebugMessage("Current QuestPlayer: <highlight>" + questPlayer);
        }
    }


    @EventHandler
    public void onPlayerSneak(final PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }

        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer == null) {
            return;
        }
        if (questPlayer.getActiveQuests().size() == 0) {
            return;
        }

        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()) {
                    if (activeObjective.getObjective() instanceof SneakObjective) {
                        activeObjective.addProgress(1);
                    }
                }

            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }

}
