package net.sistr.actionarms.item.util;

import static org.junit.jupiter.api.Assertions.*;

import net.sistr.actionarms.item.data.BulletData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CylinderTest {

    static final int CAPACITY = 6;
    static final BulletData TEST_BULLET = new BulletData("test_bullet", 9f, 12f);

    Cylinder cylinder;

    @BeforeEach
    void setUp() {
        cylinder = new Cylinder(CAPACITY);
    }

    @Nested
    class 初期状態 {
        @Test
        void 全薬室が空() {
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.firingChamber().isEmpty());
                cylinder.cockRotate();
            }
        }

        @Test
        void 容量が正しい() {
            assertEquals(CAPACITY, cylinder.getCapacity());
        }

        @Test
        void 射撃位置は0() {
            assertEquals(0, cylinder.getFiringIndex());
        }

        @Test
        void ゲート位置は1() {
            assertEquals(1, cylinder.gateIndex());
        }
    }

    @Nested
    class コック回転 {
        @Test
        void 時計回りに回転する() {
            assertEquals(0, cylinder.getFiringIndex());
            cylinder.cockRotate();
            assertEquals(CAPACITY - 1, cylinder.getFiringIndex());
        }

        @Test
        void 一周すると最初に戻る() {
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.cockRotate();
            }
            assertEquals(0, cylinder.getFiringIndex());
        }

        @Test
        void コック後に旧射撃位置の薬室がゲートに来る() {
            // firingIndex=0 の薬室に直接装填してから cockRotate
            cylinder.firingChamber().loadCartridge(new Cartridge(TEST_BULLET));
            int firedIndex = cylinder.getFiringIndex(); // 0
            cylinder.cockRotate();
            // 旧射撃位置(0)がゲート位置に来る
            assertEquals(firedIndex, cylinder.gateIndex());
        }
    }

    @Nested
    class 装填回転 {
        @Test
        void 反時計回りに回転する() {
            assertEquals(0, cylinder.getFiringIndex());
            cylinder.loadRotate();
            assertEquals(1, cylinder.getFiringIndex());
        }

        @Test
        void コック回転と逆方向() {
            cylinder.cockRotate();
            cylinder.loadRotate();
            assertEquals(0, cylinder.getFiringIndex());
        }
    }

    @Nested
    class 装填 {
        @Test
        void ゲート位置の空の薬室に装填できる() {
            assertTrue(cylinder.loadAtGate(TEST_BULLET));
            assertFalse(cylinder.gateChamber().isEmpty());
        }

        @Test
        void 装填済みの薬室には装填できない() {
            cylinder.loadAtGate(TEST_BULLET);
            assertFalse(cylinder.loadAtGate(TEST_BULLET));
        }

        @Test
        void ゲートに装填し装填回転を繰り返して全装填できる() {
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.loadAtGate(TEST_BULLET));
                cylinder.loadRotate();
            }
            assertTrue(cylinder.isAllLoaded());
        }
    }

    @Nested
    class 射撃 {
        @Test
        void 射撃位置の装填済み薬室を射撃すると弾が返る() {
            // 射撃位置の薬室に直接装填
            cylinder.firingChamber().loadCartridge(new Cartridge(TEST_BULLET));
            var bullet = cylinder.shootFiring();
            assertTrue(bullet.isPresent());
            assertEquals(TEST_BULLET, bullet.get());
        }

        @Test
        void 射撃後は空薬莢が残る() {
            cylinder.firingChamber().loadCartridge(new Cartridge(TEST_BULLET));
            cylinder.shootFiring();
            assertFalse(cylinder.firingChamber().isEmpty());
            assertFalse(cylinder.canShootFiring());
        }

        @Test
        void 空の薬室は射撃できない() {
            var bullet = cylinder.shootFiring();
            assertTrue(bullet.isEmpty());
        }
    }

    @Nested
    class 排莢 {
        @Test
        void ゲート位置の空薬莢を排莢できる() {
            // 射撃位置に装填 → 射撃 → コック回転で空薬莢がゲートに来る
            cylinder.firingChamber().loadCartridge(new Cartridge(TEST_BULLET));
            cylinder.shootFiring();
            cylinder.cockRotate();
            // 旧射撃位置がゲートに来ている
            assertTrue(cylinder.shouldEjectAtGate());
            var ejected = cylinder.ejectAtGate();
            assertTrue(ejected.isPresent());
            assertTrue(cylinder.gateChamber().isEmpty());
        }

        @Test
        void 実弾を排莢するとカートリッジが返る() {
            cylinder.loadAtGate(TEST_BULLET);
            var ejected = cylinder.ejectAtGate();
            assertTrue(ejected.isPresent());
            assertTrue(ejected.get().canShoot());
            assertTrue(cylinder.gateChamber().isEmpty());
        }

        @Test
        void 空の薬室は排莢できない() {
            var ejected = cylinder.ejectAtGate();
            assertTrue(ejected.isEmpty());
        }
    }

    @Nested
    class 複合操作 {
        @Test
        void 全薬室に装填して全薬室から射撃できる() {
            // ゲートから装填 → loadRotate で次の薬室をゲートに
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.loadRotate();
            }
            // 全薬室から射撃（cockRotate で次の薬室を射撃位置に）
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.canShootFiring(), "薬室" + i + "が射撃可能");
                assertTrue(cylinder.shootFiring().isPresent());
                cylinder.cockRotate();
            }
        }

        @Test
        void 全弾射撃後コック回転で全排莢できる() {
            // 全装填
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.loadRotate();
            }
            // 全射撃
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.shootFiring();
                cylinder.cockRotate();
            }
            // 全排莢（コック回転で空薬莢がゲートに来る）
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(
                        cylinder.ejectAtGate().isPresent(),
                        "薬室" + cylinder.gateIndex() + "の排莢");
                cylinder.cockRotate();
            }
            // 全再装填（loadRotate で回しながら）
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(
                        cylinder.loadAtGate(TEST_BULLET),
                        "薬室" + cylinder.gateIndex() + "の再装填");
                cylinder.loadRotate();
            }
        }

        @Test
        void 装填数を数えられる() {
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.loadRotate();
            cylinder.loadAtGate(TEST_BULLET);
            assertEquals(2, cylinder.countLoaded());
        }

        @Test
        void 空の状態で装填数は0() {
            assertEquals(0, cylinder.countLoaded());
        }

        @Test
        void 全装填で装填数はキャパシティ() {
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.loadRotate();
            }
            assertEquals(CAPACITY, cylinder.countLoaded());
        }

        @Test
        void 全装填でisAllLoadedがtrue() {
            assertFalse(cylinder.isAllLoaded());
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.loadRotate();
            }
            assertTrue(cylinder.isAllLoaded());
        }
    }
}
