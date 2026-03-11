package net.sistr.actionarms.item.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.LeverActionGunData;
import net.sistr.actionarms.item.data.MagazineData;
import net.sistr.actionarms.item.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LeverActionGunComponentTest {

    static final float TICK = 1f / 20f;

    static final BulletData TEST_BULLET = new BulletData("test_bullet", 9f, 12f);

    // 期待 tick 数（float 精度で +1 tick 必要な場合がある）
    // fireCoolLength=0.1 → 2 ticks, leverDownLength=0.15 → 3 ticks,
    // leverUpLength=0.1 → 2 ticks,
    // cycleCoolLength=0.05 → 1 tick, reloadLength=0.2 → 4 ticks, etc.
    static final int FIRE_COOL_TICKS = 2;
    static final int LEVER_DOWN_TICKS = 3; // leverDownLength の前半フェーズ
    static final int LEVER_UP_TICKS = 2; // leverUpLength の後半フェーズ
    static final int CYCLE_COOL_TICKS = 1;
    static final int RELOAD_TICKS = 4;
    static final int RELOAD_COOL_TICKS = 1;
    // キャンセル可能になるまでの tick 数
    // cycleTime が cycleCancelableLength(=0.05) 以下になるタイミング
    // leverDownLength(0.15) - cycleCancelableLength(0.05) = 0.10 → 2 ticks
    static final int CYCLE_CANCEL_OPEN_TICKS = 2;
    // reloadTime が reloadCancelableLength(=0.05) 以下になるタイミング
    // reloadLength(0.2) - reloadCancelableLength(0.05) = 0.15 → 3 ticks
    static final int RELOAD_CANCEL_OPEN_TICKS = 3;

    static final LeverActionGunData TEST_GUN_DATA =
            new LeverActionGunData(
                    "test_gun",
                    /* fireCoolLength= */ 0.1f,
                    /* leverDownLength= */ 0.15f,
                    /* leverUpLength= */ 0.1f,
                    /* cycleCoolLength= */ 0.05f,
                    /* cycleCancelableLength= */ 0.05f,
                    /* reloadLength= */ 0.2f,
                    /* reloadCoolLength= */ 0.05f,
                    /* reloadCancelableLength= */ 0.05f,
                    /* reloadCount= */ 1,
                    /* baseSpreadAngle= */ 3f,
                    /* aimSpreadAngle= */ 0.5f,
                    /* movementSpreadIncrease= */ 2f);

    static final MagazineData TEST_MAGAZINE_DATA =
            new MagazineData("test_magazine", 7, bullet -> true);

    // float 精度マージン: 累積減算で == 0 判定に +1 tick 必要な場合がある
    static final int FP_MARGIN = 1;

    LeverActionGunComponent gun;
    List<String> soundLog;
    List<String> animationLog;
    List<BulletData> firedBullets;
    List<Cartridge> ejectedCartridges;

    LeverActionPlaySoundContext stubSoundContext;
    AnimationContext stubAnimationContext;
    FireTrigger.FireStartContext stubFireContext;
    CyclingLever.CycleTickContext stubCycleContext;
    Reloadable.ReloadTickContext emptyReloadContext;

    @BeforeEach
    void setUp() {
        gun = new LeverActionGunComponent(TEST_GUN_DATA, TEST_MAGAZINE_DATA);
        soundLog = new ArrayList<>();
        animationLog = new ArrayList<>();
        firedBullets = new ArrayList<>();
        ejectedCartridges = new ArrayList<>();

        stubSoundContext = sound -> soundLog.add(sound.name());
        stubAnimationContext = (anim, sec) -> animationLog.add(anim);
        stubFireContext = (g, bullet) -> firedBullets.add(bullet);
        stubCycleContext = ejectedCartridges::add;
        emptyReloadContext = emptyReloadTickContext();
    }

    // === ヘルパー ===

    void tick() {
        gun.tick(stubSoundContext, stubCycleContext, emptyReloadContext, TICK, true);
    }

    void tick(Reloadable.ReloadTickContext reloadContext) {
        gun.tick(stubSoundContext, stubCycleContext, reloadContext, TICK, true);
    }

    void tickN(int n) {
        for (int i = 0; i < n; i++) {
            tick();
        }
    }

    void tickN(int n, Reloadable.ReloadTickContext reloadContext) {
        for (int i = 0; i < n; i++) {
            tick(reloadContext);
        }
    }

    /** 期待 tick 数で条件を満たすことを検証する。 float 精度により +{@link #FP_MARGIN} tick まで許容する。条件が満たされない場合はテスト失敗。 */
    void tickAndExpect(int expectedTicks, BooleanSupplier condition, String message) {
        tickN(expectedTicks);
        if (condition.getAsBoolean()) return;
        // float 精度マージン
        for (int i = 0; i < FP_MARGIN; i++) {
            tick();
            if (condition.getAsBoolean()) return;
        }
        fail(message + " (" + expectedTicks + "+" + FP_MARGIN + " ticks で未達)");
    }

    void tickAndExpect(
            int expectedTicks,
            BooleanSupplier condition,
            String message,
            Reloadable.ReloadTickContext reloadContext) {
        tickN(expectedTicks, reloadContext);
        if (condition.getAsBoolean()) return;
        for (int i = 0; i < FP_MARGIN; i++) {
            tick(reloadContext);
            if (condition.getAsBoolean()) return;
        }
        fail(message + " (" + expectedTicks + "+" + FP_MARGIN + " ticks で未達)");
    }

    /** チャンバーに直接装填 + hammerReady にする（ショートカット） */
    void setReadyToFire() {
        gun.getChamber().loadCartridge(new Cartridge(TEST_BULLET));
        gun.setHammerReady(true);
    }

    /** サイクルの各段階を順に完了させる（段階ごとに FP マージン適用） */
    void completeCycle() {
        tickAndExpect(LEVER_DOWN_TICKS, gun::isLeverDown, "レバー下がる");
        tickAndExpect(LEVER_UP_TICKS, () -> !gun.isCycling(), "サイクル完了");
        tickAndExpect(CYCLE_COOL_TICKS, () -> gun.canCycleLever(), "サイクルクールダウン完了");
    }

    /** マガジンに弾を入れてサイクル完了まで進める（正規ルート） */
    void loadViaNormalRoute() {
        gun.getMagazine().addFirstBullet(TEST_BULLET);
        gun.cycleLever(stubSoundContext, stubAnimationContext);
        completeCycle();
    }

    Reloadable.ReloadStartContext stubReloadStartContext(boolean hasBullet) {
        return predicate -> hasBullet;
    }

    static Reloadable.ReloadTickContext emptyReloadTickContext() {
        return new Reloadable.ReloadTickContext() {
            @Override
            public List<BulletData> popBullets(Predicate<BulletData> predicate, int count) {
                return List.of();
            }

            @Override
            public void returnBullets(List<BulletData> bullets) {}
        };
    }

    Reloadable.ReloadTickContext stubReloadContext(List<BulletData> available) {
        return new Reloadable.ReloadTickContext() {
            final List<BulletData> pool = new ArrayList<>(available);

            @Override
            public List<BulletData> popBullets(Predicate<BulletData> predicate, int count) {
                var result = new ArrayList<BulletData>();
                var iter = pool.iterator();
                while (iter.hasNext() && result.size() < count) {
                    BulletData b = iter.next();
                    if (predicate.test(b)) {
                        result.add(b);
                        iter.remove();
                    }
                }
                return result;
            }

            @Override
            public void returnBullets(List<BulletData> bullets) {
                pool.addAll(bullets);
            }
        };
    }

    // === テスト ===

    @Nested
    class 初期状態 {
        @Test
        void 射撃できない() {
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void サイクルできる() {
            assertTrue(gun.canCycleLever());
        }

        @Test
        void チャンバーは空() {
            assertTrue(gun.getChamber().isEmpty());
        }

        @Test
        void マガジンは空() {
            assertTrue(gun.getMagazine().isEmpty());
        }

        @Test
        void ハンマーは未準備() {
            assertFalse(gun.isHammerReady());
        }

        @Test
        void レバーは上() {
            assertFalse(gun.isLeverDown());
        }
    }

    @Nested
    class 射撃 {
        @Test
        void チャンバー装填済みハンマー準備済みで射撃できる() {
            setReadyToFire();
            assertTrue(gun.canPullTrigger());
        }

        @Test
        void 正規ルートで射撃可能状態に到達できる() {
            loadViaNormalRoute();
            assertTrue(gun.getChamber().canShoot());
            assertTrue(gun.isHammerReady());
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
        void 射撃後ハンマーは未準備になる() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertFalse(gun.isHammerReady());
        }

        @Test
        void 射撃後チャンバーに空薬莢が残る() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertTrue(gun.getChamber().isInCartridge());
            assertFalse(gun.getChamber().canShoot());
        }

        @Test
        void 射撃後は再射撃できない() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void 射撃サウンドとアニメーションが再生される() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertTrue(soundLog.contains("FIRE"));
            assertTrue(animationLog.contains("fire"));
        }

        @Test
        void ハンマー未準備では射撃できない() {
            gun.getChamber().loadCartridge(new Cartridge(TEST_BULLET));
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void チャンバー空でドライファイアする() {
            gun.setHammerReady(true);
            assertTrue(gun.canPullTrigger());
            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            assertTrue(soundLog.contains("DRY_FIRE"));
            assertTrue(animationLog.contains("dry_fire"));
            assertEquals(0, firedBullets.size());
        }
    }

    @Nested
    class サイクル {
        @Test
        void サイクル開始できる() {
            assertTrue(gun.canCycleLever());
            assertTrue(gun.cycleLever(stubSoundContext, stubAnimationContext));
            assertTrue(gun.isCycling());
        }

        @Test
        void サイクル前半完了でレバーが下がり排莢される() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCycleLever(), "射撃クールダウン完了");

            gun.cycleLever(stubSoundContext, stubAnimationContext);
            tickAndExpect(LEVER_DOWN_TICKS, gun::isLeverDown, "レバーが下がる");

            assertTrue(gun.isLeverDown());
            assertEquals(1, ejectedCartridges.size());
        }

        @Test
        void サイクル完了でマガジンから装填される() {
            gun.getMagazine().addFirstBullet(TEST_BULLET);
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();

            assertTrue(gun.getChamber().canShoot());
            assertTrue(gun.isHammerReady());
            assertFalse(gun.isLeverDown());
            assertFalse(gun.isCycling());
        }

        @Test
        void マガジン空でサイクルするとチャンバーも空のまま() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();

            assertFalse(gun.getChamber().canShoot());
            assertTrue(gun.isHammerReady());
        }

        @Test
        void サイクル中は再サイクルできない() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            assertFalse(gun.canCycleLever());
        }

        @Test
        void チャンバー空のサイクルアニメーション() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            assertTrue(soundLog.contains("CYCLE"));
            assertTrue(animationLog.contains("cycle_empty"));
        }

        @Test
        void チャンバーに薬莢がある場合のサイクルアニメーション() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCycleLever(), "射撃クールダウン完了");
            animationLog.clear();

            gun.cycleLever(stubSoundContext, stubAnimationContext);
            assertTrue(animationLog.contains("cycle"));
        }
    }

    @Nested
    class リロード {
        @Test
        void マガジンに空きがあり弾があればリロードできる() {
            assertTrue(gun.canLoadBullet(stubReloadStartContext(true)));
        }

        @Test
        void マガジンが満タンならリロードできない() {
            for (int i = 0; i < TEST_MAGAZINE_DATA.capacity(); i++) {
                gun.getMagazine().addFirstBullet(TEST_BULLET);
            }
            assertFalse(gun.canLoadBullet(stubReloadStartContext(true)));
        }

        @Test
        void インベントリに弾がなければリロードできない() {
            assertFalse(gun.canLoadBullet(stubReloadStartContext(false)));
        }

        @Test
        void リロード完了でマガジンに弾が入る() {
            var reloadContext = stubReloadContext(List.of(TEST_BULLET));
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));

            tickAndExpect(RELOAD_TICKS, () -> !gun.isReloading(), "リロード完了", reloadContext);

            assertEquals(1, gun.getMagazine().getBullets().size());
        }

        @Test
        void リロード中は再リロードできない() {
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));
            assertFalse(gun.canLoadBullet(stubReloadStartContext(true)));
        }

        @Test
        void サイクル中はリロードできない() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            assertFalse(gun.canLoadBullet(stubReloadStartContext(true)));
        }

        @Test
        void リロードのサウンドとアニメーションが再生される() {
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));
            assertTrue(soundLog.contains("RELOAD"));
            assertTrue(animationLog.stream().anyMatch(a -> a.startsWith("reload")));
        }
    }

    @Nested
    class クールダウン {
        @Test
        void 射撃クールダウン中はサイクルできない() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertFalse(gun.canCycleLever());
        }

        @Test
        void 射撃クールダウン中はリロードできない() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertFalse(gun.canLoadBullet(stubReloadStartContext(true)));
        }

        @Test
        void 射撃クールダウンは期待tick数で完了する() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCycleLever(), "射撃クールダウン完了");
        }

        @Test
        void サイクルクールダウンは期待tick数で完了する() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();
            assertTrue(gun.canCycleLever());
        }

        @Test
        void リロードクールダウンは期待tick数で完了する() {
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));
            int totalReloadTicks = RELOAD_TICKS + RELOAD_COOL_TICKS;
            tickAndExpect(
                    totalReloadTicks,
                    () -> gun.canLoadBullet(stubReloadStartContext(true)),
                    "リロードクールダウン完了");
        }

        @Test
        void 射撃クールダウン完了前はサイクル不可() {
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            // クールダウン完了の 1 tick 前
            tickN(Math.max(0, FIRE_COOL_TICKS - 1));
            assertFalse(gun.canCycleLever(), "クールダウン完了前にサイクルできてはいけない");
        }
    }

    @Nested
    class キャンセル {
        // canPullTrigger の条件: cycleTime <= cycleCancelableLength
        // cycleTime はカウントダウンなので、キャンセル可能なのは終了間際

        @Test
        void サイクル開始直後はキャンセル不可() {
            setReadyToFire();
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void サイクル終了間際にキャンセル可能になる() {
            setReadyToFire();
            gun.cycleLever(stubSoundContext, stubAnimationContext);

            tickAndExpect(CYCLE_CANCEL_OPEN_TICKS, gun::canPullTrigger, "キャンセルウィンドウ開始");

            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            assertFalse(gun.isCycling());
        }

        @Test
        void キャンセルウィンドウ前は射撃不可() {
            setReadyToFire();
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            tickN(Math.max(0, CYCLE_CANCEL_OPEN_TICKS - 1));
            assertFalse(gun.canPullTrigger(), "キャンセルウィンドウ前に射撃できてはいけない");
        }

        @Test
        void リロード開始直後はキャンセル不可() {
            setReadyToFire();
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));
            assertFalse(gun.canPullTrigger());
        }

        @Test
        void リロード終了間際にキャンセル可能になる() {
            setReadyToFire();
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));

            tickAndExpect(RELOAD_CANCEL_OPEN_TICKS, gun::canPullTrigger, "リロードキャンセルウィンドウ開始");

            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            // trigger() はリロードもキャンセルする
            assertFalse(gun.isReloading());
        }

        @Test
        void リロードキャンセルウィンドウ前は射撃不可() {
            setReadyToFire();
            gun.loadBullet(stubSoundContext, stubAnimationContext, stubReloadStartContext(true));
            tickN(Math.max(0, RELOAD_CANCEL_OPEN_TICKS - 1));
            assertFalse(gun.canPullTrigger(), "リロードキャンセルウィンドウ前に射撃できてはいけない");
        }
    }

    @Nested
    class 複合フロー {
        @Test
        void 正規ルートでリロードからサイクルして射撃する() {
            var reloadContext = stubReloadContext(List.of(TEST_BULLET));

            // 1. リロード
            assertTrue(
                    gun.loadBullet(
                            stubSoundContext, stubAnimationContext, stubReloadStartContext(true)));
            tickAndExpect(RELOAD_TICKS, () -> !gun.isReloading(), "リロード完了", reloadContext);
            tickAndExpect(RELOAD_COOL_TICKS, () -> gun.canCycleLever(), "リロードクールダウン完了");

            assertEquals(1, gun.getMagazine().getBullets().size());

            // 2. サイクル
            assertTrue(gun.cycleLever(stubSoundContext, stubAnimationContext));
            completeCycle();

            assertTrue(gun.getChamber().canShoot());
            assertTrue(gun.isHammerReady());
            assertTrue(gun.getMagazine().isEmpty());

            // 3. 射撃
            assertTrue(gun.canPullTrigger());
            assertTrue(gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext));
            assertEquals(1, firedBullets.size());
        }

        @Test
        void マガジン満タンまでリロードを繰り返す() {
            var bullets = new ArrayList<BulletData>();
            for (int i = 0; i < TEST_MAGAZINE_DATA.capacity(); i++) {
                bullets.add(TEST_BULLET);
            }
            var reloadContext = stubReloadContext(bullets);

            for (int round = 0; round < TEST_MAGAZINE_DATA.capacity(); round++) {
                assertTrue(
                        gun.loadBullet(
                                stubSoundContext,
                                stubAnimationContext,
                                stubReloadStartContext(true)),
                        "リロード " + (round + 1) + " 回目に失敗");

                tickAndExpect(
                        RELOAD_TICKS,
                        () -> !gun.isReloading(),
                        "リロード " + (round + 1) + " 回目完了",
                        reloadContext);
                // 最終ラウンド以外はクールダウン後に次のリロードが可能か確認
                if (round < TEST_MAGAZINE_DATA.capacity() - 1) {
                    tickAndExpect(
                            RELOAD_COOL_TICKS,
                            () -> gun.canLoadBullet(stubReloadStartContext(true)),
                            "リロード " + (round + 1) + " 回目クールダウン完了");
                }
            }

            assertTrue(gun.getMagazine().isFull());
        }

        @Test
        void 非アクティブ時はサイクルとリロードが進行しない() {
            gun.cycleLever(stubSoundContext, stubAnimationContext);

            for (int i = 0; i < LEVER_DOWN_TICKS + LEVER_UP_TICKS + CYCLE_COOL_TICKS + 10; i++) {
                gun.tick(stubSoundContext, stubCycleContext, emptyReloadContext, TICK, false);
            }

            assertTrue(gun.isCycling(), "active=false でサイクルが進行してはいけない");
        }

        @Test
        void 射撃後サイクルで再装填して再び射撃できる() {
            // マガジンに2発入れてサイクル
            gun.getMagazine().addFirstBullet(TEST_BULLET);
            gun.getMagazine().addFirstBullet(TEST_BULLET);
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();

            // 1発目
            assertTrue(gun.canPullTrigger());
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCycleLever(), "射撃クールダウン完了");

            // サイクルで再装填
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();

            // 2発目
            assertTrue(gun.canPullTrigger());
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            assertEquals(2, firedBullets.size());
        }

        @Test
        void サイクルで排莢と装填が同時に行われる() {
            // チャンバーに空薬莢、マガジンに弾
            setReadyToFire();
            gun.pullTrigger(stubSoundContext, stubAnimationContext, stubFireContext);
            tickAndExpect(FIRE_COOL_TICKS, () -> gun.canCycleLever(), "射撃クールダウン完了");
            gun.getMagazine().addFirstBullet(TEST_BULLET);

            // サイクル開始
            assertTrue(gun.getChamber().isInCartridge()); // 空薬莢がある
            gun.cycleLever(stubSoundContext, stubAnimationContext);
            completeCycle();

            // 排莢されて新しい弾が装填される
            assertEquals(1, ejectedCartridges.size());
            assertTrue(gun.getChamber().canShoot());
            assertTrue(gun.getMagazine().isEmpty());
        }
    }
}
