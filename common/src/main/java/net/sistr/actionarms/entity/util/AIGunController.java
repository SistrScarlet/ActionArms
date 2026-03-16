package net.sistr.actionarms.entity.util;

/** AI（Mob等）が銃を操作するためのコントローラー。銃の種類に依存しない統一インターフェース。 */
public interface AIGunController {

    void setGoal(GunGoal goal);

    GunGoal getGoal();

    /**
     * 操作クールダウンの倍率を設定する。各操作後に発生するAI側のクールダウンに乗算される。
     *
     * <p>1.0 = ベース速度、2.0 = クールダウン2倍（遅い）、0.5 = クールダウン半分（速い）。
     */
    void setCooldownMultiplier(float multiplier);

    GunStatus getStatus();

    TickResult tick(float timeDelta);

    enum GunGoal {
        /** 何もしない。 */
        IDLE,
        /** リロードする。戦闘後などに行い、次の戦闘に備える。 */
        RELOAD,
        /** 銃を撃てる状態にする。必要に応じてリロードも行う。 */
        READY,
        /** 攻撃する。READY + 射撃。 */
        ATTACK
    }

    record GunStatus(
            /** 攻撃行動が取れるか。弾が完全に無い等なら false。 */
            boolean canAttack,
            /** リロード行動が取れるか。満タンなら false。 */
            boolean canReload) {}

    record TickResult(
            /** このtickで射撃が発生したか。 */
            boolean fired) {}
}
