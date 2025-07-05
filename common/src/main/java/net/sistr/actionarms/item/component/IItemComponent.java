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
                                                   ItemStack stack,
                                                   ExecuteFunction<T> function) {
        execute(constructor.get(), stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent> void execute(Supplier<T> constructor,
                                                   NbtCompound nbt,
                                                   ExecuteFunction<T> function) {
        execute(constructor.get(), nbt, function);
    }

    private static <T extends IItemComponent> void execute(T component,
                                                           NbtCompound nbt,
                                                           ExecuteFunction<T> function) {
        component.read(nbt);
        var result = function.execute(component);
        if (result == ComponentResult.MODIFIED) {
            component.write(nbt);
        }
    }

    static <T extends IItemComponent, R> R query(Supplier<T> constructor,
                                                 ItemStack stack,
                                                 Function<T, R> function) {
        return query(constructor.get(), stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent, R> R query(Supplier<T> constructor,
                                                 NbtCompound nbt,
                                                 Function<T, R> function) {
        return query(constructor.get(), nbt, function);
    }

    /**
     * コンポーネントから値を読み取ります（読み取り専用、保存しません）。
     *
     * @param component コンポーネント
     * @param nbt       対象のItemStackのnbt
     * @param function  読み取り処理
     * @param <T>       コンポーネントの型
     * @param <R>       戻り値の型
     * @return 読み取った値
     */
    private static <T extends IItemComponent, R> R query(T component,
                                                         NbtCompound nbt,
                                                         Function<T, R> function) {
        component.read(nbt);
        return function.apply(component);
    }

    static <T extends IItemComponent> void update(Supplier<T> constructor,
                                                  ItemStack stack,
                                                  Consumer<T> function) {
        update(constructor.get(), stack.getOrCreateNbt(), function);
    }

    static <T extends IItemComponent> void update(Supplier<T> constructor,
                                                  NbtCompound nbt,
                                                  Consumer<T> function) {
        update(constructor.get(), nbt, function);
    }

    /**
     * コンポーネントを更新します（常に保存されます）。
     *
     * @param component コンポーネント
     * @param nbt       対象のItemStackのnbt
     * @param function  更新処理
     * @param <T>       コンポーネントの型
     */
    private static <T extends IItemComponent> void update(T component,
                                                          NbtCompound nbt,
                                                          Consumer<T> function) {
        component.read(nbt);
        function.accept(component);
        component.write(nbt);
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
