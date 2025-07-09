package io.hhplus.tdd;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointController;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PointControllerTest {
    // 테스트 대상
    private final PointController controller;
    // 의존성
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointControllerTest() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        controller = new PointController(userPointTable, pointHistoryTable);
    }

    @Test
    void returnCorrectUserPoint() { // 특정 유저의 포인트 조회
        long userId = 1L;
        long pointAmount = 100L;
        // 포인트 삽입
        userPointTable.insertOrUpdate(userId, pointAmount);
        // 포인트 조회
        UserPoint result = controller.point(userId);
        // 검증
        assertEquals(userId, result.id());
        assertEquals(pointAmount, result.point());
    }

    @Test
    void returnCorrectChargeHistory() { // 특정 유저의 포인트 충전 이력 조회
        long userId = 1L;
        long chargeAmount = 100L;
        long now = System.currentTimeMillis();

        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, now);

        List<PointHistory> result = controller.history(userId);

        for (PointHistory h : result){
            if (h.type() == TransactionType.CHARGE){
                assertEquals(userId, h.userId());
                assertEquals(chargeAmount, h.amount());
                assertEquals(TransactionType.CHARGE, h.type());
                assertTrue(h.updateMillis() >= now);
            }
        }
    }

    @Test
    void returnCorrectUseHistory() { // 특정 유저의 포인트 사용 이력 조회
        long userId = 1L;
        long usedAmount = 100L;
        long now = System.currentTimeMillis();

        pointHistoryTable.insert(userId, usedAmount, TransactionType.USE, now);

        List<PointHistory> result = controller.history(userId);

        for (PointHistory h : result){
            if (h.type() == TransactionType.USE){
                assertEquals(userId, h.userId());
                assertEquals(usedAmount, h.amount());
                assertEquals(TransactionType.USE, h.type());
                assertTrue(h.updateMillis() >= now);
            }
        }
    }

    @Test
    void returnCorrectCharge() { // 특정 유저의 포인트 충전
        long userId = 1L;
        long originAmount = 1000L;
        long chargeAmount = 100L;
        long exceedAmount = 60000L;
        long maxPoint = 50000L;
        long negativeAmount = -1L;
        long updatedAmount = originAmount + chargeAmount;
        long now = System.currentTimeMillis();

        // 기존 포인트 삽입
        userPointTable.insertOrUpdate(userId,originAmount);
        // 포인트 충전
        UserPoint result = controller.charge(userId, chargeAmount);

        // 포인트 충전액이 음수인 경우의 테스트
        IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                ()-> controller.charge(userId,negativeAmount));
        assertEquals("0보다 큰 금액만 충전할 수 있습니다.", exception2.getMessage());

        // 충전 내역 테스트
        assertEquals(userId, result.id());
        assertEquals(updatedAmount, result.point());
        assertTrue(result.updateMillis() >= now);

        // 포인트 최대 금액 테스트
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.charge(userId,exceedAmount));
        assertEquals("최대 충전 금액은 " + maxPoint + "원 입니다.", exception.getMessage());

        // 이력 테스트
        List<PointHistory> histResult = controller.history(userId);
        boolean chargeFound = false;
        for (PointHistory h : histResult) {
            if (h.type() == TransactionType.CHARGE && h.amount() == chargeAmount) {
                chargeFound = true;
                assertEquals(userId, h.userId());
                assertEquals(chargeAmount, h.amount());
                assertEquals(TransactionType.CHARGE, h.type());
                assertTrue(h.updateMillis() >= now);
            }
        }
        assertTrue(chargeFound,"이력 사항이 1개 이상 필요합니다.");
    }

    @Test
    void returnCorrectUse() { // 특정 유저의 포인트 사용
        long userId = 1L;
        long usedAmount = 100L;
        long originAmount = 500L;
        long exceedAmount = 600L;
        long updatedAmount = originAmount - usedAmount;
        long negativeAmount = -1L;
        long now = System.currentTimeMillis();

        // 기존 포인트 삽입
        userPointTable.insertOrUpdate(userId, originAmount);
        // 포인트 사용
        UserPoint result = controller.use(userId,usedAmount);

        // 포인트 사용액이 음수인 경우의 테스트
        IllegalArgumentException neException = assertThrows(
                IllegalArgumentException.class,
                ()-> controller.use(userId, negativeAmount));
        assertEquals("0보다 큰 금액만 사용할 수 있습니다.", neException.getMessage());

        // 포인트 사용 내역 테스트
        assertEquals(userId, result.id());
        assertEquals(updatedAmount, result.point());
        assertTrue(result.updateMillis() >= now);

        // 포인트 잔액 테스트
        IllegalArgumentException balException = assertThrows(
                IllegalArgumentException.class,
                ()->controller.use(userId,exceedAmount));
        assertEquals("잔액이 부족합니다.", balException.getMessage());

        // 이력 테스트
        boolean useFound = false;
        List<PointHistory> history = controller.history(userId);
        for (PointHistory h : history){
            if (h.type() == TransactionType.USE){
                useFound = true;
                assertEquals(userId, h.userId());
                assertEquals(usedAmount, h.amount());
                assertEquals(TransactionType.USE, h.type());
                assertTrue(h.updateMillis() >= now);
            }
        }
        assertTrue(useFound,"이력 사항이 1개 이상 필요합니다.");

    }
}