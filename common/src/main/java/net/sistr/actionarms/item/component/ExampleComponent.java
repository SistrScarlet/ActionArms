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

    // === 従来のexecuteメソッドの使用例 ===
    public static void incrementExample(ItemStack stack) {
        IItemComponent.execute(ExampleComponent::new, stack, (component) -> {
            component.count++;
            return IItemComponent.ComponentResult.MODIFIED; // 変更があったので保存
        });
    }

    // === ComponentResultの使い分け例 ===
    public static void incrementWithLimit(ItemStack stack, int maxCount) {
        IItemComponent.execute(ExampleComponent::new, stack, component -> {
            if (component.count >= maxCount) {
                return IItemComponent.ComponentResult.NO_CHANGE; // 上限に達している、変更なし
            }
            component.count++;
            return IItemComponent.ComponentResult.MODIFIED; // 正常な変更
        });
    }

    public static void resetIfCorrupted(ItemStack stack) {
        IItemComponent.execute(ExampleComponent::new, stack, component -> {
            if (component.count < 0) {
                component.count = 0; // 負の値は異常なのでリセット
                return IItemComponent.ComponentResult.MODIFIED;
            }
            return IItemComponent.ComponentResult.NO_CHANGE;
        });
    }

    // === 新しいqueryメソッドの使用例（読み取り専用） ===
    public static int getCount(ItemStack stack) {
        return IItemComponent.query(ExampleComponent::new, stack, component -> component.count);
    }

    // === 新しいupdateメソッドの使用例（常に保存） ===
    public static void setCount(ItemStack stack, int newCount) {
        IItemComponent.update(ExampleComponent::new, stack, component -> {
            component.count = newCount;
        });
    }

    public static void addCount(ItemStack stack, int amount) {
        IItemComponent.update(ExampleComponent::new, stack, component -> {
            component.count += amount;
        });
    }

}
