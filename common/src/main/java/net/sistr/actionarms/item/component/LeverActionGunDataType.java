package net.sistr.actionarms.item.component;

public record LeverActionGunDataType(float fireCoolLength,
                                     float reloadLength,
                                     float leverDownLength,
                                     float leverUpLength,
                                     float cycleCancelableLength,
                                     float reloadCancelableLength,
                                     float cycleCoolLength,
                                     float reloadCoolLength,
                                     int reloadCount) {
}
