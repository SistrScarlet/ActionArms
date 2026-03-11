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
        void 回転すると射撃位置が進む() {
            assertEquals(0, cylinder.getFiringIndex());
            cylinder.cockRotate();
            assertEquals(1, cylinder.getFiringIndex());
        }

        @Test
        void 一周すると最初に戻る() {
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.cockRotate();
            }
            assertEquals(0, cylinder.getFiringIndex());
        }
    }

    @Nested
    class 装填回転 {
        @Test
        void 反時計回りに回転する() {
            assertEquals(0, cylinder.getFiringIndex());
            cylinder.loadRotate();
            assertEquals(CAPACITY - 1, cylinder.getFiringIndex());
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
        void ゲートに装填しコック回転すると射撃位置に来る() {
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
            assertTrue(cylinder.canShootFiring());
        }
    }

    @Nested
    class 射撃 {
        @Test
        void 射撃位置の装填済み薬室を射撃すると弾が返る() {
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
            var bullet = cylinder.shootFiring();
            assertTrue(bullet.isPresent());
            assertEquals(TEST_BULLET, bullet.get());
        }

        @Test
        void 射撃後は空薬莢が残る() {
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
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
            // ゲートに装填 → コック回転で射撃位置へ → 射撃 → コック回転でゲート位置へ戻す
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
            cylinder.shootFiring();
            // 射撃した薬室(index=1)は今firingIndex=1にある。ゲートはindex=2。
            // 空薬莢をゲート位置に持ってくるため、loadRotateで戻す。
            // firingIndex=0に戻すとゲート=1(空薬莢のある薬室)になる。
            cylinder.loadRotate();
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
            // ゲート位置に装填 → コック回転を繰り返して全薬室に装填
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.cockRotate();
            }
            // 全薬室から射撃
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.canShootFiring(), "薬室" + i + "が射撃可能");
                assertTrue(cylinder.shootFiring().isPresent());
                cylinder.cockRotate();
            }
        }

        @Test
        void 全排莢してから全装填できる() {
            // 全装填
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.cockRotate();
            }
            // 全射撃
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.shootFiring();
                cylinder.cockRotate();
            }
            // 全排莢（ゲート位置で排莢 → コック回転）
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.ejectAtGate().isPresent(), "薬室" + cylinder.gateIndex() + "の排莢");
                cylinder.cockRotate();
            }
            // 全再装填
            for (int i = 0; i < CAPACITY; i++) {
                assertTrue(cylinder.loadAtGate(TEST_BULLET), "薬室" + cylinder.gateIndex() + "の再装填");
                cylinder.cockRotate();
            }
        }

        @Test
        void 装填数を数えられる() {
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
            cylinder.loadAtGate(TEST_BULLET);
            cylinder.cockRotate();
            cylinder.cockRotate();
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
                cylinder.cockRotate();
            }
            assertEquals(CAPACITY, cylinder.countLoaded());
        }

        @Test
        void 全装填でisAllLoadedがtrue() {
            assertFalse(cylinder.isAllLoaded());
            for (int i = 0; i < CAPACITY; i++) {
                cylinder.loadAtGate(TEST_BULLET);
                cylinder.cockRotate();
            }
            assertTrue(cylinder.isAllLoaded());
        }
    }
}
