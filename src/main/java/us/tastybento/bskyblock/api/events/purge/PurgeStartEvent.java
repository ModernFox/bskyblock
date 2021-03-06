package us.tastybento.bskyblock.api.events.purge;

import java.util.List;
import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired when islands to remove have been chosen and before starting to remove them.
 * You can remove or add islands to remove.
 * Canceling this event will cancel the purge
 *
 * @author Poslovitch
 * @since 1.0
 */
public class PurgeStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final UUID user;
    private List<UUID> islandsList;

    /**
     * Called to create the event
     * @param user - the UUID of the player who launched the purge, may be null if purge is launched using the console.
     * @param islandsList - the list of islands to remove, based on their leader's UUID 
     */
    public PurgeStartEvent(UUID user, List<UUID> islandsList) {
        this.user = user;
        this.islandsList = islandsList;
    }

    /**
     * @return the user who launched the purge, may be null if purge is launched using the console.
     */
    public UUID getUser( ){
        return this.user;
    }

    /**
     * @return the list of islands to remove, based on their leader's UUID 
     */
    public List<UUID> getIslandsList() {
        return this.islandsList;
    }

    /**
     * Convenience method to directly add an island owner's UUID to the list
     * @param - the owner's UUID from the island to remove
     */
    public void add(UUID islandOwner) {
        if(!this.islandsList.contains(islandOwner)) islandsList.add(islandOwner);
    }

    /**
     * Convenience method to directly remove an island owner's UUID to the list
     * @param - the owner's UUID from the island to remove
     */
    public void remove(UUID islandOwner) {
        if(this.islandsList.contains(islandOwner)) islandsList.remove(islandOwner);
    }

    /**
     * Replace the island list
     * @param - a new island owners' UUIDs list
     */
    public void setIslandsList(List<UUID> islandsList) {
        this.islandsList = islandsList;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
