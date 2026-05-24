package horror.blueice129.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public class BarebonesInventory {
    public final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
    public final DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);

    private BarebonesInventory() {}

    public static BarebonesInventory from(PlayerInventory inventory) {
        BarebonesInventory barebonesInventory = new BarebonesInventory();
        for (int i = 0; i < barebonesInventory.main.size(); i++) {
            barebonesInventory.main.set(i, inventory.main.get(i).copy());
        }
        for (int i = 0; i < barebonesInventory.armor.size(); i++) {
            barebonesInventory.armor.set(i, inventory.armor.get(i).copy());
        }
        for (int i = 0; i < barebonesInventory.offHand.size(); i++) {
            barebonesInventory.offHand.set(i, inventory.offHand.get(i).copy());
        }
        return barebonesInventory;
    }

    public void copyTo(PlayerInventory inventory) {
        for (int i = 0; i < inventory.main.size(); i++) {
            inventory.main.set(i, this.main.get(i).copy());
        }
        for (int i = 0; i < inventory.armor.size(); i++) {
            inventory.armor.set(i, this.armor.get(i).copy());
        }
        for (int i = 0; i < inventory.offHand.size(); i++) {
            inventory.offHand.set(i, this.offHand.get(i).copy());
        }
    }
}
