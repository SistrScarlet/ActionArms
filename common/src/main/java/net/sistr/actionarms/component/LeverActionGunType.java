package net.sistr.actionarms.component;

public record LeverActionGunType(String name,
                                 float fireCoolLength,
                                 float reloadLength,
                                 float leverDownLength,
                                 float leverUpLength,
                                 float cycleCancelableLength,
                                 float reloadCancelableLength,
                                 float cycleCoolLength,
                                 float reloadCoolLength,
                                 int reloadCount) {
}
