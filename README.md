여행 참가 웹 서비스
Java 17 | Redisson 분산 락 | Redis TTL 상태 전환 | 실시간 채팅

여행을 계획하는 사용자가 나라별로 실시간 채팅을 통해 여행 정보를 공유하고,
같이 갈 사람들을 모집하여 일정 인원이 모이면 결제까지 이어지는 웹 여행 플랫폼입니다.

🛠️ 기술 스택
Backend: Spring Boot, Spring Security, JPA, Redis (Redisson), MySQL
Frontend: React, TypeScript, WebSocket
Infra: Docker, Nginx, EC2, GitHub
Test: JMeter (동시성 테스트)
💡 주요 기능 및 구현 내용
상품 참가 기능
Redisson 기반 분산 락 적용 → 중복 참가 방지
JMeter로 20개 동시 요청 테스트 → DB 비관적 락 vs Redisson 성능 비교
결제 대기 상태 관리
Redis TTL 사용 → 결제 유효 시간 만료 시 자동 상태 전환
상태값: WAITING_PAYMENT, WAITING_LIST
실시간 채팅
WebSocket 기반 채팅 구현
→ 참가자 간 실시간 커뮤니케이션 가능
→ 각 나라별 채팅방 구분 구현
실시간 채팅
아임포트(Iamport) 연동을 통한 실제 결제 흐름 구현
운영 환경 구성
Docker Compose 기반 분리 배포
Nginx + EC2 활용한 실제 운영 환경 구성
시연 이미지
(https://docs.google.com/presentation/d/1IZ-uT5ffndQMo4kRtf8mLynbSewYMcrqGmeO-VIxQIk/edit?slide=id.p#slide=id.p)
