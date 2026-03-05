import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.*;

public class test {
    public static void main(String[] args) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File dir = new File(".");
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                String oldName = file.getName();
                // 원본 파일 보호를 위해 "modified_" 접두사를 붙여 생성합니다.
                if (oldName.contains("(통합계정인증") && oldName.endsWith(".xlsx") && !oldName.startsWith("modified_")) {
                    String newName = "modified_" + oldName.replaceAll("_\\d{8}", "_" + today);
                    File newFile = new File(file.getParent(), newName);

                    try {
                        processExcelContent(file, newFile);
                        System.out.println("✅ 처리 완료: " + newName);
                    } catch (IOException e) {
                        System.err.println("❌ 에러 발생: " + oldName);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void processExcelContent(File src, File dest) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(src));
             ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(dest))) {

            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                // 새로운 엔트리를 만들 때 압축 해제된 데이터만 깔끔하게 넘김
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zout.putNextEntry(newEntry);

                if (entry.getName().equals("xl/worksheets/sheet1.xml") ||
                        entry.getName().equals("xl/sharedStrings.xml")) {

                    // 텍스트 손상을 막기 위해 ByteArrayOutputStream 사용
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zin.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }

                    String content = new String(bos.toByteArray(), StandardCharsets.UTF_8);

                    // 1. 점검자 변경
                    content = content.replace("홍길동", "이순신");

                    // 2. A2열 내용 0으로 변경 (패턴을 매우 단순화해서 태그 파손 방지)
                    // 점검일시 문구와 그 뒤의 시간 문자열을 최대한 넓게 잡아 0으로 치환
                    content = content.replaceAll("점검일시\\s*:\\s*[^<]+", "0");

                    zout.write(content.getBytes(StandardCharsets.UTF_8));
                } else {
                    // 이미지, 스타일, 폰트 정보 등은 원본 그대로 복사 (깨짐 방지 핵심)
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zin.read(buffer)) != -1) {
                        zout.write(buffer, 0, len);
                    }
                }
                zout.closeEntry();
                zin.closeEntry();
            }
        }
    }
}