package net.sistr.actionarms.item.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import net.sistr.actionarms.item.data.BulletData;
import net.sistr.actionarms.item.data.SAAGunData;
import net.sistr.actionarms.item.util.Cartridge;
import net.sistr.actionarms.item.util.Chamber;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * smartGateRotate の27パターン網羅テスト。
 *
 * <p>ゲート位置の時計回り側(cw)、ゲート位置、反時計回り側(ccw) の3薬室の状態の全組み合わせ。
 *
 * <p>優先順: <br>
 * 1. cw(時計回り側) に空薬莢 → cockRotate <br>
 * 2. ゲート位置に空薬莢 → 回転なし <br>
 * 3. ccw(反時計回り側) に空薬莢 → loadRotate <br>
 * 4. ゲート位置が空 → 回転なし <br>
 * 5. cw に空薬室 → cockRotate <br>
 * 6. ccw に空薬室 → loadRotate <br>
 * 7. いずれも該当しない → 回転なし
 */
class SmartGateRotateTest {

    static final BulletData TEST_BULLET = new BulletData("test_bullet", 9f, 12f);
    static final int CAPACITY = 6;
    static final SAAGunData TEST_GUN_DATA =
            new SAAGunData("test", CAPACITY, 0.1f, 0.05f, 0.15f, 0.15f, 3f, 0.5f, 2f);

    enum S {
        EMPTY,
        SPENT,
        LOADED
    }

    enum R {
        NONE,
        CW, // cockRotate（時計回り、firingIndex--）
        CCW // loadRotate（反時計回り、firingIndex++）
    }

    // @formatter:off
    static Stream<Arguments> patterns() {
        return Stream.of(
                //          cw       gate     ccw      expected
                // 全空
                Arguments.of(S.EMPTY, S.EMPTY, S.EMPTY, R.NONE),
                Arguments.of(S.EMPTY, S.EMPTY, S.SPENT, R.CCW),
                Arguments.of(S.EMPTY, S.EMPTY, S.LOADED, R.NONE),
                // gate=SPENT
                Arguments.of(S.EMPTY, S.SPENT, S.EMPTY, R.NONE),
                Arguments.of(S.EMPTY, S.SPENT, S.SPENT, R.NONE),
                Arguments.of(S.EMPTY, S.SPENT, S.LOADED, R.NONE),
                // gate=LOADED, cw=EMPTY
                Arguments.of(S.EMPTY, S.LOADED, S.EMPTY, R.CW),
                Arguments.of(S.EMPTY, S.LOADED, S.SPENT, R.CCW),
                Arguments.of(S.EMPTY, S.LOADED, S.LOADED, R.CW),
                // cw=SPENT
                Arguments.of(S.SPENT, S.EMPTY, S.EMPTY, R.CW),
                Arguments.of(S.SPENT, S.EMPTY, S.SPENT, R.CW),
                Arguments.of(S.SPENT, S.EMPTY, S.LOADED, R.CW),
                Arguments.of(S.SPENT, S.SPENT, S.EMPTY, R.CW),
                Arguments.of(S.SPENT, S.SPENT, S.SPENT, R.CW),
                Arguments.of(S.SPENT, S.SPENT, S.LOADED, R.CW),
                Arguments.of(S.SPENT, S.LOADED, S.EMPTY, R.CW),
                Arguments.of(S.SPENT, S.LOADED, S.SPENT, R.CW),
                Arguments.of(S.SPENT, S.LOADED, S.LOADED, R.CW),
                // cw=LOADED, gate=EMPTY
                Arguments.of(S.LOADED, S.EMPTY, S.EMPTY, R.NONE),
                Arguments.of(S.LOADED, S.EMPTY, S.SPENT, R.CCW),
                Arguments.of(S.LOADED, S.EMPTY, S.LOADED, R.NONE),
                // cw=LOADED, gate=SPENT
                Arguments.of(S.LOADED, S.SPENT, S.EMPTY, R.NONE),
                Arguments.of(S.LOADED, S.SPENT, S.SPENT, R.NONE),
                Arguments.of(S.LOADED, S.SPENT, S.LOADED, R.NONE),
                // cw=LOADED, gate=LOADED
                Arguments.of(S.LOADED, S.LOADED, S.EMPTY, R.CCW),
                Arguments.of(S.LOADED, S.LOADED, S.SPENT, R.CCW),
                Arguments.of(S.LOADED, S.LOADED, S.LOADED, R.NONE));
    }

    // @formatter:on

    @ParameterizedTest(name = "cw={0} gate={1} ccw={2} → {3}")
    @MethodSource("patterns")
    void smartGateRotateのパターン(S cw, S gate, S ccw, R expected) {
        var gun = new SAAGunComponent(TEST_GUN_DATA);
        var cylinder = gun.getCylinder();

        // firingIndex=0, gate=1, cw=gate-1=0, ccw=gate+1=2
        int gateIdx = cylinder.gateIndex(); // 1
        int cwIdx = (gateIdx - 1 + CAPACITY) % CAPACITY; // 0
        int ccwIdx = (gateIdx + 1) % CAPACITY; // 2

        setChamberState(cylinder.getChamberAt(cwIdx), cw);
        setChamberState(cylinder.getChamberAt(gateIdx), gate);
        setChamberState(cylinder.getChamberAt(ccwIdx), ccw);

        int indexBefore = cylinder.getFiringIndex();
        gun.smartGateRotate();
        int indexAfter = cylinder.getFiringIndex();

        int expectedIndex =
                switch (expected) {
                    case NONE -> indexBefore;
                    case CW -> (indexBefore - 1 + CAPACITY) % CAPACITY;
                    case CCW -> (indexBefore + 1) % CAPACITY;
                };

        assertEquals(
                expectedIndex,
                indexAfter,
                "cw="
                        + cw
                        + " gate="
                        + gate
                        + " ccw="
                        + ccw
                        + " → expected "
                        + expected
                        + " (firingIndex: "
                        + indexBefore
                        + "→"
                        + expectedIndex
                        + ") but was "
                        + indexAfter);
    }

    private void setChamberState(Chamber chamber, S state) {
        switch (state) {
            case EMPTY:
                break;
            case LOADED:
                chamber.loadCartridge(new Cartridge(TEST_BULLET));
                break;
            case SPENT:
                chamber.loadCartridge(new Cartridge(TEST_BULLET));
                chamber.shoot();
                break;
        }
    }
}
