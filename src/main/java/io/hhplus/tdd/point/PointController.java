package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/point")
public class PointController {
    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    // ConcurrentHashMap - 락을 보관할 공간, 여러 쓰레드가 동시에 접근해도 안전하게 동작 가능
    // 사용자별 ID(Long)으로 Object(락객체) 저장
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public PointController(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return userPointTable.selectById(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        // 동시성 제어를 위한 lock(한 사용자의 id에 여러번 포인트 충전)
        Object lock = userLocks.computeIfAbsent(id, k -> new Object());
        // 동기화 코드
        synchronized (lock) {
            if (amount <= 0) {  //충전할 포인트가 음수로 입력되었을 때
                throw new IllegalArgumentException("0보다 큰 금액만 충전할 수 있습니다.");
            }
            UserPoint current = userPointTable.selectById(id);
            long now = System.currentTimeMillis();
            long maxPoint = 50000L;
            long updatedAmount = current.point() + amount;
            // 최대 충전 금액 제한
            if(updatedAmount > maxPoint){
                throw new IllegalArgumentException("최대 충전 금액은 " +maxPoint+ "원 입니다.");
            }
            // 포인트 충전 시 이력
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, now);
            // 업데이트된 유저 포인트 반환
            return userPointTable.insertOrUpdate(id, updatedAmount);
        }
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        //사용할 포인트가 음수로 입력되었을 때
        if (amount <= 0) {
            throw new IllegalArgumentException("0보다 큰 금액만 사용할 수 있습니다.");
        }
        UserPoint current = userPointTable.selectById(id);
        // 잔액 부족 확인
        if(current.point() < amount){
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        // 포인트 사용 시 이력
        long now = System.currentTimeMillis();
        pointHistoryTable.insert(id, amount, TransactionType.USE, now);

        long updatedAmount = current.point() - amount;
        return userPointTable.insertOrUpdate(id, updatedAmount);
    }
}
