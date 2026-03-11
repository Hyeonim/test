import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test4
{
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z]{2,7}$";
    // 2. ⭐️ 성능 최적화: 정규식을 쓸 때마다 만들지 않고, 딱 한 번만 미리 컴파일(준비)해 둡니다.
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * 이메일 유효성을 검사하는 메서드
     * @param email 검사할 이메일 문자열
     * @return 형식이 맞으면 true, 틀리면 false
     */
    public static boolean isValidEmail(String email) {
        // null이거나 빈 칸이면 바로 튕겨냅니다. (하이버네이트와 다른 점!)
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // 미리 준비해둔 정규식 패턴과 입력받은 이메일이 일치하는지(matches) 확인합니다.
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    public static void main(String[] args) {

//        EmailValidator validator = new EmailValidator();

        // ✅ 1. 성공 케이스 (Key: 이메일, Value: 어떤 특징이 있는지 설명)
        // 입력한 순서대로 출력하기 위해 LinkedHashMap을 사용합니다.
        Map<String, String> validCases = new LinkedHashMap<>();
        validCases.put("test@example.com", "가장 기본적인 표준 형태");
        validCases.put("test123@example.com", "가장 기본적인 표준 형태");
        validCases.put("first.last@example.com", "아이디에 마침표(.) 포함");
        validCases.put("user-name@example.com", "아이디에 하이픈(-) 포함");
        validCases.put("user+tag@example.com", "구글 등에서 쓰는 태그(+) 포함");
        validCases.put("123456789@example.com", "숫자로만 이루어진 아이디");
        validCases.put("test@localhost", "[특징] 로컬 도메인 허용 (Hibernate 특성)");
        validCases.put("test@192.168.0.1", "[특징] IP 주소 형태 허용 (Hibernate 특성)");
        validCases.put(null, "[특징] null은 검사하지 않고 무조건 통과 (Hibernate 특성)");
        validCases.put("", "[특징] 빈 문자열도 통과 (Hibernate 특성)");

        validCases.put("testexample.com", "@ 기호 누락");
        validCases.put("@example.com", "아이디 부분 누락");
        validCases.put("test@", "도메인 부분 누락");
        validCases.put("test@example@com", "@ 기호가 2개 이상 중복");
        validCases.put("test space@example.com", "아이디에 공백(띄어쓰기) 포함");
        validCases.put("test@exa mple.com", "도메인에 공백(띄어쓰기) 포함");
        validCases.put("test..name@example.com", "마침표(.) 연속 두 번 사용");
        validCases.put(".test@example.com", "마침표(.)로 시작함");
        validCases.put("test.@example.com", "마침표(.)로 끝남");
        validCases.put("test@!#$example.com", "도메인에 허용되지 않는 특수문자 포함");

        for (Map.Entry<String, String> entry : validCases.entrySet()) {
            String email = entry.getKey();
            String description = entry.getValue();

//            boolean isValid = validator.isValid(email,null);

            // 결과, 이메일, 설명을 한 줄에 깔끔하게 맞춤 출력
//            System.out.printf("결과: [%-5b] | %-25s | 설명: %s%n", isValid, email, description);
        }


    }
}