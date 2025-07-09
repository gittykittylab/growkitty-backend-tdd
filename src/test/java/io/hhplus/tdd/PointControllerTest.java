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
}