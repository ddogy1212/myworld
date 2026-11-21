# MYWORLD 1.1.0 · 데이터 출처와 현실성 범위

## 국가 수
- 유엔 회원국: **193** (UN 공식 분류)
- 유엔 비회원 옵서버: **2** (교황청 / State of Palestine)
- 기타 별도 표시: **2** (대만, 코소보; 국제적 지위·인정 범위는 다름)
- 합계: **197개**, 종속령/해외령을 독립국가로 자동 분류하지 않음

국가 코드·이름·지역: datasets/country-codes (기초 ISO/UN 구조 정보), 게임 내부에 국가별 행으로 포함.

## 실제 통계
- 인구: World Bank indicator `SP.POP.TOTL`, GitHub datasets/population 추출 데이터 최신 관측 **2025년** (국가별 연도 별도 저장)
- 명목 GDP(미국 달러): World Bank indicator `NY.GDP.MKTP.CD`, GitHub datasets/gdp 추출 데이터 최신 관측 **2023년** (국가별 연도 별도 저장)
- 좌표: samayo/country-json country-by-geo-coordinates에서 국가 지리적 범위의 중심점을 사용. 일부 나라에서 좌표 누락은 지도 마커를 숨기고 국가 검색에서는 유지
- 자료 누락: 북한(명목 GDP), 대만·교황청(본 데이터에서 인구 및 GDP) 등의 값은 **0을 '실제 0'이라고 주장하지 않으며**, 게임 UI에는 '자료 없음'으로 표시
- 경제 게임 지수는 통계의 소득수준에서 계산한 대략적인 게임 수치이며 **실제 통계 지표가 아님**
- 정치 안정, 동맹, 분쟁, 시민집단, 과학기술, 환경 등은 아직 **게임 모델의 가정**이므로 2026년 11월 실제 상황과 정확히 일치하지 않음

## 2026년 국제 정세
유엔, IMF October 2026 WEO, UCDP v26.1 같은 최신 공개 자료를 개별적으로 검증할 수 있지만, 이 앱은 2026년 현재의 **모든 양자 동맹, 전쟁·평화 협상, 제재 및 선거를 검증된 방식으로 반영하지 못했습니다**. 자동 진행 과정은 대체역사 시뮬레이션으로 표시합니다. 2026년의 뉴스나 예측치를 임의로 현재 사실로 생성하지 않습니다.

UN membership: https://www.un.org/en/about-us/member-states
UN observers: https://www.un.org/en/about-us/non-member-states
World Bank population: https://data.worldbank.org/indicator/SP.POP.TOTL
World Bank GDP: https://data.worldbank.org/indicator/NY.GDP.MKTP.CD
IMF October 2026 WEO: https://www.imf.org/external/datamapper/datasets/WEO
UCDP downloads (2025 coverage): https://ucdp.uu.se/downloads/

## 자유 명령 입력 기능
앱 상단 텍스트 창에서 국가명 + 하고 싶은 일을 입력합니다. 지원되는 문장 예시:
- `부탄에 12개월 동안 재생에너지 투자`
- `가나에서 의료 예산 30% 확대`
- `한국과 일본 무역협정 체결`
- `미국과 캐나다 외교 대화`
- `나우루에서 대학 교육 개혁`
- `세계 전체 탄소 협력`

**인터넷 AI 모델이 아닌 오프라인 키워드/국가명 명령 해석기**이므로 문장에 따라 지원하지 않는 동작도 있습니다. 명령은 대상, 내용, 강도, 기간을 화면에서 확인한 뒤에만 실행합니다. `30%` 등의 수치는 **게임용 영향력 단계(1~3)**로 변환되며 실제 30% 예산 증액을 물리적으로 재현하지 않습니다.

기간은 **게임 속 1~24개월**, 영향을 받는 국가는 자동으로 예산을 지출하며 계획을 이어갑니다. 실행 가능한 명령과 구체적 결과는 해당 시점의 국가 경제·정치 상태에 따라 달라집니다.

## 장기 동작
현실 24시간 = 게임 1개월, Android JobScheduler는 일정 실행을 보장하지 않음. 앱 또는 위젯을 열면 실제 경과 시간을 바탕으로 최대 96개월의 미처 진행되지 않은 달을 순차 따라잡습니다. 장시간 오프라인 상태에선 이어서 따라잡을 수 있습니다.

## QA
Android GitHub Actions Gradle `:app:testDebugUnitTest :app:assembleDebug` 통과.
검사 항목: 197개 목록·193/2/2 분류·국가 간 외교 행렬 대칭성·120개월 진행·결정론·자유 명령 파서·장기 정책 동작·플레이어 개입 제한.
실제 갤럭시 장시간 동작, API 최신성, 현실 정치 정확성은 테스트 결과가 아님.
