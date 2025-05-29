package net.sistr.actionarms.item.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class ExampleComponent implements IItemComponent {
    public int count = 0;

    @Override
    public void read(NbtCompound nbt) {
        this.count = nbt.getInt("count");
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putInt("count", this.count);
    }

    public static void incrementExample(ItemStack stack) {
        IItemComponent.execute(ExampleComponent::new, stack, (component) -> {
            component.count++;
            return true;
        });
    }

}
