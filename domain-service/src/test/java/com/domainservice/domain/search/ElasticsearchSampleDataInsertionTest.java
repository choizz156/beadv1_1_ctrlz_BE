package com.domainservice.domain.search;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import com.domainservice.domain.search.model.entity.dto.document.ProductPostDocumentEntity;
import com.domainservice.domain.search.repository.ProductPostElasticRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles({ "local", "secret" })
public class ElasticsearchSampleDataInsertionTest {

    @Autowired
    private ProductPostElasticRepository productPostElasticRepository;

    @Test
    @DisplayName("test1: CSV 파일을 읽어 ES에 모든 데이터 삽입 및 카테고리 분류")
    void test1() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/test.csv");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<ProductPostDocumentEntity> entities = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] allColumns = line.split(",");
                if (allColumns.length < 14)
                    continue;

                int len = allColumns.length;
                String id = allColumns[0].trim();
                String deleteStatus = allColumns[1].trim();
                LocalDateTime createdAt = LocalDateTime.parse(allColumns[2].trim(), formatter);
                LocalDateTime updatedAt = LocalDateTime.parse(allColumns[3].trim(), formatter);
                String userId = allColumns[4].trim();

                // 끝에서부터 고정 필드 추출 (likedCount, viewCount, tradeStatus, status)
                long likedCount = Long.parseLong(allColumns[len - 1].trim());
                long viewCount = Long.parseLong(allColumns[len - 2].trim());
                String tradeStatus = allColumns[len - 3].trim();
                String status = allColumns[len - 4].trim();

                // 가격(price) 필드 인덱스 찾기 (숫자로만 구성된 첫 필드, 제목/이름 이후인 index 7부터 탐색)
                int priceIdx = -1;
                for (int i = 7; i < len - 4; i++) {
                    if (allColumns[i].trim().matches("\\d+")) {
                        priceIdx = i;
                        break;
                    }
                }

                if (priceIdx == -1) {
                    log.warn("가격을 찾을 수 없는 라인 스킵: {}", line);
                    continue;
                }

                long price = Long.parseLong(allColumns[priceIdx].trim());
                String name = allColumns[priceIdx - 1].trim();

                // 제목(title): index 6부터 priceIdx-2까지 합침 (중간에 콤마가 있었을 경우 복원)
                StringBuilder titleBuilder = new StringBuilder();
                for (int i = 6; i <= priceIdx - 2; i++) {
                    if (i > 6)
                        titleBuilder.append(",");
                    titleBuilder.append(allColumns[i]);
                }
                String title = titleBuilder.toString().trim();

                // 설명(description): priceIdx+1부터 len-5까지 합침
                StringBuilder descBuilder = new StringBuilder();
                for (int i = priceIdx + 1; i <= len - 5; i++) {
                    if (i > priceIdx + 1)
                        descBuilder.append(",");
                    descBuilder.append(allColumns[i]);
                }
                String description = descBuilder.toString().trim();

                // CSV의 카테고리(index 5) 사용, 없으면 categorize 로직 사용
                String csvCategory = allColumns[5].trim();
                String categoryName = csvCategory.isEmpty() ? categorize(name, title, price) : csvCategory;

                ProductPostDocumentEntity entity = ProductPostDocumentEntity.builder()
                        .id(id)
                        .deleteStatus(deleteStatus)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .userId(userId)
                        .title(title)
                        .name(name)
                        .price(price)
                        .description(description)
                        .status(status)
                        .tradeStatus(tradeStatus)
                        .viewCount(viewCount)
                        .likedCount(likedCount)
                        .categoryName(categoryName)
                        .tags(Arrays.asList(name.split(" ")))
                        .primaryImageUrl("https://example.com/image.jpg")
                        .build();

                entities.add(entity);
                count++;

                // 벌크 삽입을 위해 1000개 단위로 저장
                if (count % 1000 == 0) {
                    productPostElasticRepository.saveAll(entities);
                    entities.clear();
                    log.info("{}개 데이터 삽입 완료...", count);
                }
            }

            if (!entities.isEmpty()) {
                productPostElasticRepository.saveAll(entities);
                log.info("최종 {}개 데이터 삽입 완료", count);
            }
        }
    }

    private String categorize(String name, String title, long price) {
        String text = (name + " " + title).toLowerCase();

        // 1. 수입명품 (비싼 브랜드 우선)
        if (text.contains("샤넬") || text.contains("로렉스") || text.contains("에르메스") ||
                text.contains("루이비통") || text.contains("구찌") || text.contains("프라다") ||
                text.contains("발렌시아가") || text.contains("생로랑") || text.contains("디올") ||
                text.contains("명품")
                || (price > 2000000 && (text.contains("백") || text.contains("가방") || text.contains("시계")))) {
            return "수입명품";
        }

        // 6. 모바일/태블릿
        if (text.contains("아이폰") || text.contains("갤럭시") || text.contains("아이패드") ||
                text.contains("태블릿") || text.contains("워치") || text.contains("스마트폰") || text.contains("버즈")
                || text.contains("에어팟")) {
            return "모바일/태블릿";
        }

        // 8. 노트북/PC
        if (text.contains("맥북") || text.contains("노트북") || text.contains("그램") ||
                text.contains("pc") || text.contains("컴퓨터") || text.contains("모니터") || text.contains("키보드")
                || text.contains("마우스")) {
            return "노트북/PC";
        }

        // 9. 카메라/캠코더
        if (text.contains("니콘") || text.contains("캐논") || text.contains("소니") ||
                text.contains("카메라") || text.contains("캠코더") || text.contains("dslr") || text.contains("미러리스")
                || text.contains("렌즈")) {
            return "카메라/캠코더";
        }

        // 12. 게임
        if (text.contains("닌텐도") || text.contains("스위치") || text.contains("ps5") ||
                text.contains("플레이스테이션") || text.contains("게임") || text.contains("타이틀") || text.contains("엑스박스")
                || text.contains("스팀덱")) {
            return "게임";
        }

        // 7. 가전제품
        if (text.contains("냉장고") || text.contains("세탁기") || text.contains("tv") ||
                text.contains("에어컨") || text.contains("건조기") || text.contains("전자레인지") || text.contains("청소기")
                || text.contains("공기청정기")) {
            return "가전제품";
        }

        // 2. 패션의류
        if (text.contains("패딩") || text.contains("코트") || text.contains("티셔츠") ||
                text.contains("바지") || text.contains("원피스") || text.contains("셔츠") || text.contains("자켓")
                || text.contains("의류")) {
            return "패션의류";
        }

        // 3. 패션잡화
        if (text.contains("운동화") || text.contains("스니커즈") || text.contains("구두") ||
                text.contains("가방") || text.contains("지갑") || text.contains("벨트") || text.contains("안경")
                || text.contains("선글라스") || text.contains("잡화")) {
            return "패션잡화";
        }

        // 4. 뷰티
        if (text.contains("화장품") || text.contains("향수") || text.contains("스킨") || text.contains("로션")
                || text.contains("뷰티")) {
            return "뷰티";
        }

        // 5. 출산/유아동
        if (text.contains("장난감") || text.contains("유모차") || text.contains("카시트") || text.contains("아기")
                || text.contains("기저귀") || text.contains("유아")) {
            return "출산/유아동";
        }

        // 10. 가구/인테리어
        if (text.contains("책상") || text.contains("침대") || text.contains("소파") || text.contains("테이블")
                || text.contains("의자") || text.contains("조명") || text.contains("인테리어")) {
            return "가구/인테리어";
        }

        // 11. 리빙/생활
        if (text.contains("주방") || text.contains("식기") || text.contains("컵") || text.contains("냄비")
                || text.contains("리빙") || text.contains("수건")) {
            return "리빙/생활";
        }

        // 13. 반려동물/취미
        if (text.contains("사료") || text.contains("간식") || text.contains("강아지") || text.contains("고양이")
                || text.contains("반려동물") || text.contains("피규어") || text.contains("레고")) {
            return "반려동물/취미";
        }

        // 14. 도서/음반/문구
        if (text.contains("도서") || text.contains("책") || text.contains("cd") || text.contains("앨범")
                || text.contains("문구") || text.contains("다이어리")) {
            return "도서/음반/문구";
        }

        // 16. 스포츠
        if (text.contains("골프") || text.contains("축구") || text.contains("야구") || text.contains("농구")
                || text.contains("스포츠") || text.contains("테니스")) {
            return "스포츠";
        }

        // 17. 레저/여행
        if (text.contains("캠핑") || text.contains("텐트") || text.contains("여행") || text.contains("캐리어")
                || text.contains("등산")) {
            return "레저/여행";
        }

        // 18. 오토바이
        if (text.contains("오토바이") || text.contains("바이크") || text.contains("헬멧")) {
            return "오토바이";
        }

        // 20. 무료나눔
        if (text.contains("나눔") || text.contains("무료")) {
            return "무료나눔";
        }

        // 21. 중고차
        if (text.contains("차") || text.contains("자동차") || text.contains("중고차")) {
            return "중고차";
        }

        // 15. 티켓/쿠폰
        if (text.contains("티켓") || text.contains("쿠폰") || text.contains("기프티콘") || text.contains("상품권")) {
            return "티켓/쿠폰";
        }

        // 19. 공구/산업용품
        if (text.contains("공구") || text.contains("드릴")) {
            return "공구/산업용품";
        }

        // 기본값: 해시 기반으로 골고루 분산하여 할당 (구분 불가한 경우)
        String[] categories = {
                "수입명품", "패션의류", "패션잡화", "뷰티", "출산/유아동", "모바일/태블릿", "가전제품",
                "노트북/PC", "카메라/캠코더", "가구/인테리어", "리빙/생활", "게임", "반려동물/취미",
                "도서/음반/문구", "티켓/쿠폰", "스포츠", "레저/여행", "오토바이", "공구/산업용품", "무료나눔", "중고차"
        };
        int index = Math.abs((name + title).hashCode()) % categories.length;
        return categories[index];
    }
}
