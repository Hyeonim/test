import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public class test2 {
    public static void main(String[] args) throws Exception {
        File inputFile = new File("(통합계정인증 위탁운영) 계정인증 부문 점검_20260122.xlsx");
        File outputFile = new File("modified.xlsx");

        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(inputFile));
             ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile))) {

            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));

                // 수정 대상을 sheet1.xml로 변경 (여기에 데이터가 들어있음)
                if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zin, StandardCharsets.UTF_8));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 생성 코드에서 넣었던 데이터를 치환
                        line = line.replace("점검자 : 배세민", "점검자 : 진상현")
                                .replace("2026-02-04", "2026-12-25"); // 날짜도 변경 확인
                        content.append(line);
                    }
                    zout.write(content.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = zin.read(buf)) > 0) {
                        zout.write(buf, 0, len);
                    }
                }
                zout.closeEntry();
                zin.closeEntry();
            }
        }
        System.out.println("수정 완료! modified.xlsx를 확인하세요.");
    }
}