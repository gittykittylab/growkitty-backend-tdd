# Java 동시성 제어 과제 보고서

## 1. 프로젝트 개요

이 프로젝트는 하나의 사용자에 대해 동시에 여러 포인트 충전 또는 사용 요청이 들어왔을 때, **정상적으로 처리되도록 동시성 제어를 구현**하는 과제입니다. Java에서의 스레드 안전, 락 사용, 메모리 누수 등을 실습하며 학습하였습니다.

---

## 2. 문제 상황 및 배경

동시에 여러 요청이 들어오면 다음과 같은 문제가 발생할 수 있습니다:

- **Race Condition**: 두 개 이상의 쓰레드가 공유 자원에 동시에 접근하여 결과가 꼬이는 현상
- **데이터 불일치**: 포인트가 중복 차감되거나 잘못 충전됨
---

## 3. Java 동시성 제어 방식

| 방식 | 설명 | 특징 |
|------|------|------|
| `synchronized` | 임계 구역(critical section)을 지정하여 한 번에 하나의 쓰레드만 접근 허용 | 단순하고 강력함, 성능 저하 가능성 |
| `ConcurrentHashMap` | 내부적으로 세그먼트 단위 락을 걸어 높은 동시 처리 지원 | Thread-safe, 단 `computeIfAbsent`는 주의 필요 |
| `Collections.synchronizedMap` | 모든 연산에 락을 거는 데코레이터 | 느리지만 안전, 복합 연산에 주의 |
| `WeakHashMap` | key가 GC 대상이 되면 자동 삭제되는 map | 메모리 누수 방지에 유리, Thread-safe 아님 |

---

## 4. 적용한 동기화 코드 설명

동기화 처리 방식은 다음과 같은 흐름으로 구성됩니다:

1. 사용자별 lock 객체를 저장할 Map을 `Collections.synchronizedMap(new WeakHashMap<>())`으로 초기화합니다.
2. `userLocks` 맵에서 `id`로 lock 객체를 가져옵니다.
3. 없다면 새 `Object()`를 만들어 저장합니다. 이 작업은 `synchronized(userLocks)`로 감싸 동시성 문제를 방지합니다.
4. 이후 해당 lock 객체로 `synchronized(lock)`을 걸어 포인트 충전/사용 로직을 안전하게 수행합니다.

이 구조는 다음과 같은 이유로 안전합니다:

- `WeakHashMap`으로 메모리 누수 방지
- `Collections.synchronizedMap`으로 단일 연산 보호
- 복합 연산 (`get → put`)은 명시적 `synchronized` 블록으로 감싸 구조 손상 방지

---

## 5. computeIfAbsent() 대신 명시적 동기화를 사용한 이유

`userLocks.computeIfAbsent(id, k -> new Object())`는 thread-safe 하지 않습니다.  
이유는 다음과 같습니다:

- `computeIfAbsent`는 단일 원자 연산처럼 보이지만, 내부적으로는 `get → 판단 → put`으로 분리되어 동작합니다.
- 특히 `WeakHashMap`이나 `Collections.synchronizedMap`과 함께 사용할 경우, 복수의 쓰레드가 동시에 접근할 수 있으며 구조 손상이 발생할 수 있습니다.

따라서 명시적으로 다음과 같은 동기화 방식으로 구현합니다:

- 먼저 `synchronized(userLocks)` 블록 내에서 `id`에 해당하는 lock 객체가 존재하는지 확인합니다.
- 없다면 새로 생성하여 `userLocks`에 저장합니다.
- 이후 해당 lock을 이용하여 `synchronized(lock)` 블록을 구성합니다.

---

## 6. 메모리 누수 방지 전략

- `ConcurrentHashMap`은 key에 대한 **강한 참조**를 유지 → GC가 key(id)를 청소하지 못함 → lock 쌓임
- `WeakHashMap`은 key가 더 이상 외부에서 참조되지 않으면 GC가 자동 수거 → lock 메모리 누수 방지
- 단점: `WeakHashMap`은 **쓰레드 안전하지 않음**
- 해결: `Collections.synchronizedMap(new WeakHashMap<>())`으로 감싸서 쓰레드 안정성 확보

```java
private final Map<Long, Object> userLocks = Collections.synchronizedMap(new WeakHashMap<>());
```

---

## 7. 테스트 전략 요약

- **ExecutorService**로 동시에 100개의 요청을 실행
- **FixedThreadPool(10)**: 동시에 최대 10개의 작업이 처리됨
- **CountDownLatch**로 모든 작업 종료를 대기
- 작업 완료 후 결과 검증: 사용한 만큼 정확히 차감되었는가?

---

## 8. 테스트 코드 설명 (예시 기반)

다음은 100개의 쓰레드가 하나의 사용자에게 동시에 포인트를 사용하는 테스트 흐름을 설명한 것입니다.

1. 사용자 포인트를 미리 10000원으로 설정
2. 100개의 쓰레드가 각각 100원씩 사용 요청을 보냄
3. 각 요청은 `use()` 메서드를 호출하며, 내부에서 락으로 동기화 처리됨
4. 모든 요청이 완료된 뒤 `userPointTable`에서 잔액 조회
5. 최종 포인트는 0원이 되어야 함

※ 실제 코드 내 예외처리, 로그 출력, assertEquals 등은 상황에 맞게 조정

---

## 9. 개선 방향

- `userLocks`의 크기가 사용자 수에 비례하여 증가할 수 있음  
  → **WeakHashMap**으로 메모리 회수 유도
- 더욱 정교한 락 방식 고려 가능

---

## 10. 개발 환경

- **Java**: 17
- **Spring Boot**: 3.x
- **Gradle**: 8.4
- **Test**: JUnit 5
- **IDE**: IntelliJ IDEA