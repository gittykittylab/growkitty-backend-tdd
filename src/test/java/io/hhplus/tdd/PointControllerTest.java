package io.hhplus.tdd;


import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointController;

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

}