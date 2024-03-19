package seoulbus;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * CSV 파일을 읽어서 DB 제장하는 프로그램
 * sol : ALTER TABLE tbl_seoul_bus_onoff convert to charset utf8;
 * ERROR 1366 (HY000): Incorrect string value: '\xEC\x9D\xB4\xEB\xAF\xB8...' for column 'name' at row 1
 * ALTER TABLE (테이블명) convert to charset utf8;으로 테이블 설정을 바꿔주니 한글로 데이터 입력이 가능해졌다.
 */
//@Slf4j
public class CsvFileSaveToDB {
    private final static String filePath = "D:/SQL/2021_seoul_bus_onoff.csv"; //절대경로 full path
//    private final Logger logger = LoggerFactory.getLogger(CsvFileSaveToDB.class);

    public static void main(String[] args) {
        //한글 깨지 방지를 위해서 characterEncoding=UTF-8 처리
        final String jdbcURL = "jdbc:mysql://localhost:3306/yscom?characterEncoding=UTF-8";
        final String username = "root";
        final String password = "1234";

        final int batchSize = 2_000; //bulk insert시 커밋 갯수

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(jdbcURL, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("1");

        StringBuilder sb = new StringBuilder("null");

        for (int i = 1; i <= 55; i++) {
            sb.append(",? ");
        }

// 마지막 쉼표 제거
        sb.setLength(sb.length() - 1);
//        sb.append(")");

        String sql = "INSERT INTO tbl_seoul_bus_onoff values (" + sb.toString() + ");";
        System.out.println(sql);

//        String sql = "INSERT INTO tbl_seoul_bus_onoff select * from (null,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,)";

        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.getMessage();
        }

        int columnSize = 55; //CSV 데이터 필드 컬럼 갯수

        List<CSVRecord> records = null;
        try {
            records = getCsvRecords();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int row = 0; row < records.size(); row++) {

            CSVRecord data = records.get(row);
            for (int fieldIndex = 0; fieldIndex < columnSize; fieldIndex++) {
                try {
                    statement.setString(fieldIndex + 1, data.get(fieldIndex));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                statement.addBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (row % batchSize == 0) {
                try {
                    statement.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                System.out.println(String.format("statement.executeBatch ing row ==> %s", row));
                try {
                    connection.commit(); //DB서버 부하분산을 원하는 대용량 처리시 중간중간 커밋
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sleep(1); //부하 분산
            }
        }


    }

    private static void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<CSVRecord> getCsvRecords() throws IOException {

        File targetFile = new File(filePath);

        int sampleDataRow = 0; //샘플 데이터 row번호
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile))) {

            List<CSVRecord> records;
            try (CSVParser parser = CSVFormat.EXCEL.withFirstRecordAsHeader().withQuote('"').parse(bufferedReader)) {
                records = parser.getRecords();
            } //엑셀타입 & 쌍따옴표 escape처리
//            log.debug("\nCSV 헤더\n\t{}\n데이터 샘플\n\t{}\n", parser.getHeaderMap(), records.get(sampleDataRow));
//            log.info("\n\t헤더 필드 갯수 :{}\n\t데이터 갯수 :{}\n\t{}번째 row의 데이터 필드 갯수:{}\n\n", parser.getHeaderMap().size(), records.size(), sampleDataRow, records.get(sampleDataRow).size());

            return records;
        }
    }

}
