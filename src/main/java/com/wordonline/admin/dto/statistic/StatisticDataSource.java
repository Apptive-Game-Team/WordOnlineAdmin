package com.wordonline.admin.dto.statistic;

/**
 * 통계를 어느 데이터베이스에서 읽을지.
 * <p>
 * admin은 이미 두 데이터베이스를 안다. {@code DATABASE_URL}이 주 데이터베이스이고,
 * {@code DEV_DATABASE_URL}이 설정되어 있으면 보조 데이터베이스가 함께 뜬다
 * ({@code SecondaryDatabaseConfig}). 보조 쪽은 JPA 없이 {@code JdbcTemplate}만 있으므로 이 화면의
 * 조회도 {@code JdbcTemplate}으로 돌아간다.
 */
public enum StatisticDataSource {

    PRIMARY("운영"),
    SECONDARY("개발");

    private final String label;

    StatisticDataSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 알 수 없는 값은 주 데이터베이스로 떨어뜨린다. 조용히 다른 DB를 보여주는 것보다 낫다. */
    public static StatisticDataSource parse(String value) {
        if (value == null) {
            return PRIMARY;
        }
        for (StatisticDataSource candidate : values()) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        return PRIMARY;
    }
}
