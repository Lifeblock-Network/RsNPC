package com.smallaswater.npc.form.inventory.responsible;

import org.powernukkitx.Player;
import org.powernukkitx.event.player.PlayerTransferItemEvent;
import org.powernukkitx.item.Item;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * @author iGxnon
 * @date 2021/9/8
 */
@Data
@SuppressWarnings("unused")
public abstract class ResponseItem {

    private final Item item;

    private BiConsumer<PlayerTransferItemEvent, Player> clickItemListener;

    public ResponseItem(@NotNull Item item) {
        this.item = item;
    }

    public ResponseItem onClick(@NotNull BiConsumer<PlayerTransferItemEvent, Player> listener) {
        this.clickItemListener = listener;
        return this;
    }

    public void callClick(@NotNull PlayerTransferItemEvent event, @NotNull Player player) {
        this.clickItemListener.accept(event, player);
    }

    public Item getItem() {
        return item.clone();
    }
}
