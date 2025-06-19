package net.sistr.actionarms.item.component;

public record LeverActionGunDataType(
        float fireCoolLength,
        float leverDownLength,
        float leverUpLength,
        float cycleCoolLength,
        float cycleCancelableLength,
        float reloadLength,
        float reloadCoolLength,
        float reloadCancelableLength,
        int reloadCount,
        float baseSpreadAngle,        // 基本拡散角（度）
        float aimSpreadAngle,     // エイム時拡散角（度）
        float movementSpreadIncrease // 移動時拡散角増加（度）
) {
}
