package net.sistr.actionarms.item;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.entity.util.AIGunController;
import net.sistr.actionarms.item.util.GlftModelItem;

public interface GunItem extends GlftModelItem {

    /**
     * AI用のGunControllerを生成する。銃の種類に依存しない統一インターフェースを提供する。
     *
     * @param user 銃を使用するエンティティ
     * @param stackSupplier 銃のItemStackを取得するSupplier
     * @param inventorySupplier 弾薬を取得するインベントリのSupplier
     * @return AIGunController
     */
    AIGunController createAIController(
            LivingEntity user,
            Supplier<ItemStack> stackSupplier,
            Supplier<Optional<Inventory>> inventorySupplier);
}
