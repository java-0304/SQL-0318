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
 * CSV ������ �о DB �����ϴ� ���α׷�
 * sol : ALTER TABLE tbl_seoul_bus_onoff convert to charset utf8;
 * ERROR 1366 (HY000): Incorrect string value: '\xEC\x9D\xB4\xEB\xAF\xB8...' for column 'name' at row 1
 * ALTER TABLE (���̺��) convert to charset utf8;���� ���̺� ������ �ٲ��ִ� �ѱ۷� ������ �Է��� ����������.
 */
//@Slf4j
public class CsvFileSaveToDB {
    private final static String filePath = "D:/SQL/2021_seoul_bus_onoff.csv"; //������ full path
//    private final Logger logger = LoggerFactory.getLogger(CsvFileSaveToDB.class);

    public static void main(String[] args) {
        //�ѱ� ���� ������ ���ؼ� characterEncoding=UTF-8 ó��
        final String jdbcURL = "jdbc:mysql://localhost:3306/yscom?characterEncoding=UTF-8";
        final String username = "root";
        final String password = "1234";

        final int batchSize = 2_000; //bulk insert�� Ŀ�� ����

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

// ������ ��ǥ ����
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

        int columnSize = 55; //CSV ������ �ʵ� �÷� ����

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
                    connection.commit(); //DB���� ���Ϻл��� ���ϴ� ��뷮 ó���� �߰��߰� Ŀ��
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sleep(1); //���� �л�
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

        int sampleDataRow = 0; //���� ������ row��ȣ
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile))) {

            List<CSVRecord> records;
            try (CSVParser parser = CSVFormat.EXCEL.withFirstRecordAsHeader().withQuote('"').parse(bufferedReader)) {
                records = parser.getRecords();
            } //����Ÿ�� & �ֵ���ǥ escapeó��
//            log.debug("\nCSV ���\n\t{}\n������ ����\n\t{}\n", parser.getHeaderMap(), records.get(sampleDataRow));
//            log.info("\n\t��� �ʵ� ���� :{}\n\t������ ���� :{}\n\t{}��° row�� ������ �ʵ� ����:{}\n\n", parser.getHeaderMap().size(), records.size(), sampleDataRow, records.get(sampleDataRow).size());

            return records;
        }
    }

}
