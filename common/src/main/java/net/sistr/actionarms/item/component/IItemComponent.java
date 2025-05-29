package net.sistr.actionarms.item.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Supplier;

public interface IItemComponent {

    void read(NbtCompound nbt);

    void write(NbtCompound nbt);

    static <T extends IItemComponent> void execute(Supplier<T> constructor,
                                                   ItemStack stack,
                                                   ExecuteFunction<T> function) {
        var component = constructor.get();
        var nbt = stack.getOrCreateNbt();
        component.read(nbt);
        if (function.execute(component)) {
            component.write(nbt);
        }
    }

    @FunctionalInterface
    interface ExecuteFunction<T extends IItemComponent> {
        /**
         * コンポーネントに対して操作を実行します。
         *
         * @param component 操作対象のコンポーネント
         * @return コンポーネントへ変更があり保存が必要な場合はtrue、そうでない場合はfalse
         */
        boolean execute(T component);
    }

}
