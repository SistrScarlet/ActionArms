package net.sistr.actionarms.item.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.SAAGunData;
import net.sistr.actionarms.item.util.AnimationContext;
import net.sistr.actionarms.item.util.Cartridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SAAGunComponentTest {

    static final float TICK = 1f / 20f;
    static final BulletData TEST_BULLET = new BulletData("test_bullet", 9f, 12f);
    static final int CYLINDER_CAPACITY = 6;

    // タイミングパラメータ
    static final float COCK_LENGTH = 0.1f; // コック所要時間
    static final float FIRE_COOL_LENGTH = 0.05f; // 射撃後クールダウン
    static final float EJECT_LENGTH = 0.15f; // 排莢所要時間
    static final float LOAD_LENGTH = 0.15f; // 装填所要時間

    static final int COCK_TICKS = 2;
    static final int FIRE_COOL_TICKS = 1;
    static final int EJECT_TICKS = 3;
    static final int LOAD_TICKS = 3;
    static final int FP_MARGIN = 1;

    static final SAAGunData TEST_GUN_DATA =
            new SAAGunData(
                    "test_saa",
                    CYLINDER_CAPACITY,
                    COCK_LENGTH,
                    FIRE_COOL_LENGTH,
                    EJECT_LENGTH,
                    LOAD_LENGTH,
                    /* baseSpreadAngle= */ 3f,
                    /* aimSpreadAngle= */ 0.5f,
                    /* movementSpreadIncrease= */ 2f);

    SAAGunComponent gun;
    List<String> soundLog;
    List<String> animationLog;
    List<BulletData> firedBullets;

    SAAGunComponent.SoundContext stubSoundContext;
    AnimationContext stubAnimationContext;
    SAAGunComponent.FireContext stubFireContext;

    @BeforeEach
    void setUp() {
        gun = new SAAGunComponent(TEST_GUN_DATA);
        soundLog = new ArrayList<>();
        animationLog = new ArrayList<>();
        firedBullets = new ArrayList<>();

        stubSoundContext = sound -> soundLog.add(sound);
        stubAnimationContext = (anim, sec) -> animationLog.add(anim);
        stubFireContext = (g, bullet) -> firedBullets.add(bullet);
    }

    // === ヘルパー ===

    void tick() {
        gun.tick(stubSoundContext, TICK, true);
    }

    void tickN(int n) {
        for (int i = 0; i < n; i++) {
            tick();
        }
    }

    void tickAndExpect(
            int expectedTicks, java.util.function.BooleanSupplier condition, String msg) {
        tickN(expectedTicks);
        if (condition.getAsBoolean()) return;
        for (int i = 0; i < FP_MARGIN; i++) {
            tick();
            if (condition.getAsBoolean()) return;
        }
        fail(msg + " (" + expectedTicks + "+" + FP_MARGIN + " ticks で未達)");
    }

    /** 全薬室に装填するショートカット */
    void loadAllChambers() {
        gun.openGate();
        for (int i = 0; i < CYLINDER_CAPACITY; i++) {
            gun.loadAtGate(TEST_BULLET);
            tickAndExpect(
                    LOAD_TICKS,
                    () ->
                            gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN
                                    || gun.getPhase() == SAAGunComponent.Phase.IDLE,
                    "装填完了 " + i);
        }
        // 全弾装填で自動的にゲートが閉じるので closeGate() は不要
    }

    /** 全薬室装填+コック済みにするショートカット */
    void setReadyToFire() {
        // コックでシリンダーが回転するので、回転先に弾がある必要がある
        loadAllChambers();
        gun.cockHammer(stubSoundContext, stubAnimationContext);
        tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了");
    }

    // === テスト ===

    @Nested
    class 初期状態 {
        @Test
        void ハンマーは未コック() {
            assertFalse(gun.isHammerCocked());
        }

        @Test
        void ゲートは閉じている() {
            assertFalse(gun.isGateOpen());
        }

        @Test
        void シリンダーは全て空() {
            assertEquals(0, gun.getCylinder().countLoaded());
        }

        @Test
        void 射撃できない() {
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void コックできる() {
            assertTrue(gun.canCockHammer());
        }
    }

    @Nested
    class コッキング {
        @Test
        void コックするとシリンダーが回転してハンマーが起きる() {
            gun.getCylinder().loadAtGate(TEST_BULLET);
            int indexBefore = gun.getCylinder().getFiringIndex();

            gun.cockHammer(stubSoundContext, stubAnimationContext);
            tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了");

            assertEquals(
                    (indexBefore - 1 + CYLINDER_CAPACITY) % CYLINDER_CAPACITY,
                    gun.getCylinder().getFiringIndex());
            assertTrue(gun.isHammerCocked());
        }

        @Test
        void コック済みで再コックしても何も起こらない() {
            setReadyToFire();
            assertFalse(gun.canCockHammer());
        }

        @Test
        void ゲート開放中は空薬莢をゲート位置から排莢する() {
            // ゲート位置に空薬莢を配置: 装填→射撃位置に移動→射撃→ゲート位置に戻す
            gun.getCylinder().loadAtGate(TEST_BULLET);
            gun.getCylinder().cockRotate();
            gun.getCylinder().shootFiring();
            gun.getCylinder().loadRotate();
            gun.openGate();

            assertTrue(gun.canEjectAtGate());
            gun.ejectAtGate(stubSoundContext, stubAnimationContext);
            tickAndExpect(
                    EJECT_TICKS, () -> gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN, "排莢完了");

            assertTrue(gun.getCylinder().gateChamber().isEmpty());
        }

        @Test
        void ゲート開放中は未発射の弾でも排莢操作が可能() {
            gun.getCylinder().loadAtGate(TEST_BULLET);
            gun.openGate();

            assertTrue(gun.canEjectAtGate());
        }

        @Test
        void 排莢後に全薬室空ならCW回転する() {
            // 射撃位置に直接装填→射撃→コック回転でゲートに空薬莢が来る
            gun.getCylinder().firingChamber().loadCartridge(new Cartridge(TEST_BULLET));
            gun.getCylinder().shootFiring();
            gun.getCylinder().cockRotate();
            // ゲート位置に空薬莢が1つだけある状態
            assertTrue(gun.getCylinder().gateChamber().shouldEject());
            gun.openGate();
            int indexBefore = gun.getCylinder().getFiringIndex();

            gun.ejectAtGate(stubSoundContext, stubAnimationContext);
            tickAndExpect(
                    EJECT_TICKS, () -> gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN, "排莢完了");

            // 全薬室空(E,E,E)なのでCW回転する
            assertEquals(
                    (indexBefore - 1 + CYLINDER_CAPACITY) % CYLINDER_CAPACITY,
                    gun.getCylinder().getFiringIndex());
        }
    }

    @Nested
    class 射撃 {
        @Test
        void コック済みで射撃できる() {
            setReadyToFire();
            assertTrue(gun.canPullTrigger());
        }

        @Test
        void 射撃すると弾が発射される() {
            setReadyToFire();
            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            assertEquals(1, firedBullets.size());
            assertEquals(TEST_BULLET, firedBullets.get(0));
        }

        @Test
        void 射撃後ハンマーが落ちる() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertFalse(gun.isHammerCocked());
        }

        @Test
        void 射撃後にシリンダーは回転しない() {
            setReadyToFire();
            int indexBefore = gun.getCylinder().getFiringIndex();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertEquals(indexBefore, gun.getCylinder().getFiringIndex());
        }

        @Test
        void 空薬室でドライファイアする() {
            // 空のままコック
            gun.cockHammer(stubSoundContext, stubAnimationContext);
            tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了");

            assertTrue(gun.canPullTrigger());
            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            assertEquals(0, firedBullets.size());
            assertFalse(gun.isHammerCocked());
            assertTrue(soundLog.contains("DRY_FIRE"));
        }

        @Test
        void 未コック状態では射撃できない() {
            gun.getCylinder().loadAtGate(TEST_BULLET);
            assertFalse(gun.canPullTrigger());
        }
    }

    @Nested
    class ゲート操作 {
        @Test
        void ゲートを開閉できる() {
            assertFalse(gun.isGateOpen());
            gun.openGate();
            assertTrue(gun.isGateOpen());
            gun.closeGate();
            assertFalse(gun.isGateOpen());
        }

        @Test
        void ゲート開放中は射撃できない() {
            setReadyToFire();
            gun.openGate();
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void ゲート開放中はコックできない() {
            gun.openGate();
            assertFalse(gun.canCockHammer());
        }
    }

    @Nested
    class 装填 {
        @Test
        void ゲート開放中に装填できる() {
            gun.openGate();
            assertTrue(gun.canLoadAtGate());
            assertTrue(gun.loadAtGate(TEST_BULLET));
        }

        @Test
        void ゲート閉じ中は装填できない() {
            assertFalse(gun.canLoadAtGate());
        }

        @Test
        void 全薬室装填済みで装填できない() {
            loadAllChambers();
            // 全弾装填後は自動でゲートが閉じる
            assertFalse(gun.canLoadAtGate());
        }

        @Test
        void 装填後に隣の空薬室へ回転する() {
            gun.openGate();
            gun.loadAtGate(TEST_BULLET);
            tickAndExpect(
                    LOAD_TICKS, () -> gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN, "装填完了");
            // 装填後、隣に空薬室があるので回転する
            assertTrue(gun.getCylinder().gateChamber().isEmpty());
        }

        @Test
        void 全弾装填後にゲートが自動的に閉じる() {
            loadAllChambers();
            assertFalse(gun.isGateOpen());
            assertEquals(SAAGunComponent.Phase.IDLE, gun.getPhase());
        }
    }

    @Nested
    class ファニング {
        // ファニング = トリガー引き続け + コック連打
        // コンポーネントレベルでは cockHammer() → コック完了 → pullTrigger() の繰り返し
        // コントローラーが「トリガー保持中にコック完了 → 即pullTrigger()」を行う

        @Test
        void コック完了後にトリガー保持で即射撃できる() {
            loadAllChambers();
            gun.cockHammer(stubSoundContext, stubAnimationContext);
            tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了");

            // コック完了 → トリガー保持中なので即射撃（コントローラーの動作をシミュレート）
            assertTrue(gun.canPullTrigger());
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertEquals(1, firedBullets.size());
        }

        @Test
        void ゲート開放中はコックできない() {
            loadAllChambers();
            gun.openGate();
            assertFalse(gun.canCockHammer());
        }
    }

    @Nested
    class 複合フロー {
        @Test
        void 全弾射撃して全弾排莢して全弾装填する() {
            loadAllChambers();

            // 全弾射撃
            for (int i = 0; i < CYLINDER_CAPACITY; i++) {
                gun.cockHammer(stubSoundContext, stubAnimationContext);
                tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了 " + i);
                gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
                tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCockHammer(), "射撃クールダウン完了 " + i);
            }
            assertEquals(CYLINDER_CAPACITY, firedBullets.size());

            // 全弾排莢
            gun.openGate();
            for (int i = 0; i < CYLINDER_CAPACITY; i++) {
                gun.ejectAtGate(stubSoundContext, stubAnimationContext);
                tickAndExpect(
                        EJECT_TICKS,
                        () -> gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN,
                        "排莢完了 " + i);
            }
            assertEquals(0, gun.getCylinder().countLoaded());

            // 全弾装填
            for (int i = 0; i < CYLINDER_CAPACITY; i++) {
                assertTrue(gun.loadAtGate(TEST_BULLET), "装填 " + i);
                tickAndExpect(
                        LOAD_TICKS,
                        () ->
                                gun.getPhase() == SAAGunComponent.Phase.GATE_OPEN
                                        || gun.getPhase() == SAAGunComponent.Phase.IDLE,
                        "装填完了 " + i);
            }
            assertEquals(CYLINDER_CAPACITY, gun.getCylinder().countLoaded());
            // 全弾装填で自動的にゲートが閉じる
        }

        @Test
        void ファニングで全弾撃ち切る() {
            loadAllChambers();

            // 全弾をコック→即射撃（ファニングのシミュレーション）
            for (int i = 0; i < CYLINDER_CAPACITY; i++) {
                gun.cockHammer(stubSoundContext, stubAnimationContext);
                tickAndExpect(COCK_TICKS, gun::isHammerCocked, "コック完了 " + i);
                // トリガー保持中なのでコック完了と同時に射撃
                gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
                if (i < CYLINDER_CAPACITY - 1) {
                    tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCockHammer(), "射撃クールダウン完了 " + i);
                }
            }
            assertEquals(CYLINDER_CAPACITY, firedBullets.size());
        }
    }
}
