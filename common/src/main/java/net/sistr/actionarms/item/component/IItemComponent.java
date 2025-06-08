package net.sistr.actionarms.item.component;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IItemComponent {

    void read(NbtCompound nbt);

    void write(NbtCompound nbt);

    static <T extends IItemComponent> void execute(Supplier<T> constructor,
                                                   NbtCompound nbt,
                                                   ExecuteFunction<T> function) {
        var component = constructor.get();
        component.read(nbt);
        var result = function.execute(component);
        if (result == ComponentResult.MODIFIED) {
            component.write(nbt);
        }
    }

    static <T extends IItemComponent> void execute(Supplier<T> constructor,
                                                   ItemStack stack,
                                                   ExecuteFunction<T> function) {
        execute(constructor, stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent, R> R query(Supplier<T> constructor,
                                                 NbtCompound nbt,
                                                 Function<T, R> function) {
        var component = constructor.get();
        component.read(nbt);
        return function.apply(component);
    }

    /**
     * コンポーネントから値を読み取ります（読み取り専用、保存しません）。
     *
     * @param constructor コンポーネントのコンストラクタ
     * @param stack       対象のItemStack
     * @param function    読み取り処理
     * @param <T>         コンポーネントの型
     * @param <R>         戻り値の型
     * @return 読み取った値
     */
    static <T extends IItemComponent, R> R query(Supplier<T> constructor,
                                                 ItemStack stack,
                                                 Function<T, R> function) {
        return query(constructor, stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent> void update(Supplier<T> constructor,
                                                  NbtCompound nbt,
                                                  Consumer<T> function) {
        var component = constructor.get();
        component.read(nbt);
        function.accept(component);
        component.write(nbt);
    }

    /**
     * コンポーネントを更新します（常に保存されます）。
     *
     * @param constructor コンポーネントのコンストラクタ
     * @param stack       対象のItemStack
     * @param function    更新処理
     * @param <T>         コンポーネントの型
     */
    static <T extends IItemComponent> void update(Supplier<T> constructor,
                                                  ItemStack stack,
                                                  Consumer<T> function) {
        update(constructor, stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent> NbtCompound getComponentNbt(Supplier<T> constructor, ItemStack stack) {
        var component = constructor.get();
        component.read(stack.getOrCreateNbt());
        var nbt = new NbtCompound();
        component.write(nbt);
        return nbt;
    }

    /**
     * コンポーネント操作の実行結果を表します。
     */
    enum ComponentResult {
        /**
         * 変更なし、保存不要
         */
        NO_CHANGE,
        /**
         * 変更あり、保存必要
         */
        MODIFIED
    }

    @FunctionalInterface
    interface ExecuteFunction<T extends IItemComponent> {
        /**
         * コンポーネントに対して操作を実行します。
         *
         * @param component 操作対象のコンポーネント
         * @return 操作の結果
         */
        ComponentResult execute(T component);
    }

}
