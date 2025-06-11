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
        int reloadCount
) {
}
