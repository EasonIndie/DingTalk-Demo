import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MyJsonUtils {

    /**
     * Parse text with possible wrapping format:
     * text: {"data":{"result1":"{...}"}}
     * or directly {"data":{"result1":"{...}"}}
     * Returns inner JSONObject (if exists), otherwise returns outer JSONObject.
     */
    public static JSONObject parsePossiblyWrappedJson(String rawText) {
        if (rawText == null) return null;

        // 1) Remove leading/trailing whitespace
        String s = rawText.trim();

        // 2) If starts with "text:", remove it
        if (s.startsWith("text:")) {
            s = s.substring(5).trim();
        }

        // 3) If wrapped with extra quotes, remove them
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }

        // 4) Parse outer JSON
        JSONObject outer;
        try {
            outer = JSON.parseObject(s);
        } catch (Exception e) {
            // Parse failed, throw exception
            throw new IllegalArgumentException("Outer JSON parse failed: " + e.getMessage(), e);
        }

        // 5) Get data.result1, if string then parse again
        JSONObject data = outer.getJSONObject("data");
        if (data == null) {
            // No data node, return outer directly
            return outer;
        }

        // Check result1 - could be object or string JSON
        Object result1Obj = data.get("result1");
        if (result1Obj == null) {
            // No result1, return data or outer as needed
            return data;
        }

        if (result1Obj instanceof JSONObject) {
            return (JSONObject) result1Obj;
        }

        String result1Str = result1Obj.toString();
        // Parse the string form JSON
        try {
            JSONObject result1 = JSON.parseObject(result1Str);
            return result1;
        } catch (Exception e) {
            // If second parse fails, wrap error info in exception
            throw new IllegalArgumentException("result1 is not valid JSON string, second parse failed: " + e.getMessage(), e);
        }
    }

    // Test main
    public static void main(String[] args) {
        // Build the test string step by step
        String jsonStr = "{\"hfnr\":\"test\",\"hzhf\":\"test summary\",\"hfcg\":\"C Not Satisfied\",\"xchfrq\":\"\"}";
        String innerJson = "{\"result1\":\"" + jsonStr + "\"}";
        String dataJson = "{\"data\":" + innerJson + "}";
        String text = "text: " + dataJson;

        try {
            JSONObject inner = parsePossiblyWrappedJson(text);
            System.out.println("Successfully parsed JSON:");
            System.out.println("hfnr = " + inner.getString("hfnr"));
            System.out.println("hzhf = " + inner.getString("hzhf"));
            System.out.println("hfcg = " + inner.getString("hfcg"));
            System.out.println("xchfrq = " + inner.getString("xchfrq"));
        } catch (Exception e) {
            System.err.println("Parse failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}