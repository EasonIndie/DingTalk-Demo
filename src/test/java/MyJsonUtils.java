import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MyJsonUtils {

    /**
     * Enhanced parser for various JSON formats from AI APIs:
     * 1. Direct JSON: {"hfnr":"content","hzhf":"summary","hfcg":"result","xchfrq":""}
     * 2. With text prefix: text: {"hfnr":"content",...}
     * 3. Wrapped format: text: {"data":{"result1":"{...}"}}
     * 4. Markdown format: ```json {"hfnr":"content",...} ```
     * 5. Multiple attempts to find valid JSON within text
     */
    public static JSONObject parsePossiblyWrappedJson(String rawText) {
        if (rawText == null) return null;

        // 1) Remove leading/trailing whitespace
        String s = rawText.trim();

        // 2) Try multiple parsing strategies in order

        // Strategy 1: Direct JSON parsing
        JSONObject result = tryParseJSON(s);
        if (result != null) {
            return result;
        }

        // Strategy 2: Remove common prefixes
        if (s.startsWith("text:")) {
            s = s.substring(5).trim();
            result = tryParseJSON(s);
            if (result != null) return result;
        }

        // Strategy 3: Remove markdown formatting
        if (s.startsWith("```json")) {
            s = s.substring(7);
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.trim();
            result = tryParseJSON(s);
            if (result != null) return result;
        }
        else if (s.startsWith("```")) {
            s = s.substring(3);
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.trim();
            result = tryParseJSON(s);
            if (result != null) return result;
        }

        // Strategy 4: Remove outer quotes
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
            result = tryParseJSON(s);
            if (result != null) return result;
        }

        // Strategy 5: Extract JSON from text using regex-like approach
        result = extractAndParseJSON(s);
        if (result != null) return result;

        // Strategy 6: Handle nested data.result1 format
        result = tryParseNestedFormat(s);
        if (result != null) return result;

        // If all strategies fail, try to extract individual fields from text
        return extractFieldsFromText(rawText);
    }

    /**
     * Try to parse string as JSON directly
     */
    private static JSONObject tryParseJSON(String text) {
        try {
            JSONObject json = JSON.parseObject(text);
            // Check if it contains the expected fields
            if (json.containsKey("hfnr") || json.containsKey("hzhf") || json.containsKey("hfcg")) {
                return json;
            }
            // If doesn't contain expected fields but is valid JSON, check for nested data
            if (json.containsKey("data")) {
                return parseDataSection(json);
            }
            return json; // Return valid JSON even if it doesn't contain expected fields
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse data section which might contain result1 or other structures
     */
    private static JSONObject parseDataSection(JSONObject json) {
        JSONObject data = json.getJSONObject("data");
        if (data == null) return json;

        // Check for result1
        Object result1Obj = data.get("result1");
        if (result1Obj != null) {
            if (result1Obj instanceof JSONObject) {
                return (JSONObject) result1Obj;
            }
            try {
                JSONObject result1 = JSON.parseObject(result1Obj.toString());
                return result1;
            } catch (Exception e) {
                // Continue trying other approaches
            }
        }

        // Check if data itself is the target
        if (data.containsKey("hfnr") || data.containsKey("hzhf") || data.containsKey("hfcg")) {
            return data;
        }

        return json;
    }

    /**
     * Extract JSON from text using pattern matching
     */
    private static JSONObject extractAndParseJSON(String text) {
        // Look for JSON-like patterns between { and }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start != -1 && end != -1 && end > start) {
            String jsonStr = text.substring(start, end + 1);
            return tryParseJSON(jsonStr);
        }

        return null;
    }

    /**
     * Try to parse nested format like {"data":{"result1":"{...}"}}
     */
    private static JSONObject tryParseNestedFormat(String text) {
        try {
            JSONObject outer = JSON.parseObject(text);
            return parseDataSection(outer);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract individual fields from text when JSON parsing fails
     */
    private static JSONObject extractFieldsFromText(String text) {
        JSONObject result = new JSONObject();

        // Try to extract common field patterns
        extractField(result, text, "hfnr");
        extractField(result, text, "hzhf");
        extractField(result, text, "hfcg");
        extractField(result, text, "xchfrq");

        return result;
    }

    /**
     * Extract a specific field from text using regex-like patterns
     */
    private static void extractField(JSONObject result, String text, String fieldName) {
        // Try various patterns for field extraction
        String[] patterns = {
            "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"",
            "'" + fieldName + "'\\s*:\\s*'([^']+)'",
            fieldName + "\\s*[:=]\\s*\"([^\"]+)\"",
            fieldName + "\\s*[:=]\\s*'([^']+)'"
        };

        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                if (m.find()) {
                    result.put(fieldName, m.group(1));
                    break;
                }
            } catch (Exception e) {
                // Continue to next pattern
            }
        }
    }

    // Test main
    public static void main(String[] args) {
        // Test multiple formats
        String[] testCases = {
            // Direct JSON format
            "{\"hfnr\":\"医生询问患者症状\",\"hzhf\":\"症状缓解\",\"hfcg\":\"C满意\",\"xchfrq\":\"\"}",

            // With text prefix
            "text: {\"hfnr\":\"医生询问患者症状\",\"hzhf\":\"症状缓解\",\"hfcg\":\"C满意\",\"xchfrq\":\"\"}",

            // Nested data.result1 format
            "{\"data\":{\"result1\":\"{\\\"hfnr\\\":\\\"医生询问患者症状\\\",\\\"hzhf\\\":\\\"症状缓解\\\",\\\"hfcg\\\":\\\"C满意\\\",\\\"xchfrq\\\":\\\"\\\"}\"}}",

            // With text prefix and nested
            "text: {\"data\":{\"result1\":\"{\\\"hfnr\\\":\\\"医生询问患者症状\\\",\\\"hzhf\\\":\\\"症状缓解\\\",\\\"hfcg\\\":\\\"C满意\\\",\\\"xchfrq\\\":\\\"\\\"}\"}}"
        };

        for (int i = 0; i < testCases.length; i++) {
            System.out.println("=== Test Case " + (i + 1) + " ===");
            String testCase = testCases[i];

            try {
                JSONObject result = parsePossiblyWrappedJson(testCase);
                System.out.println("Successfully parsed JSON:");
                System.out.println("hfnr = " + result.getString("hfnr"));
                System.out.println("hzhf = " + result.getString("hzhf"));
                System.out.println("hfcg = " + result.getString("hfcg"));
                System.out.println("xchfrq = " + result.getString("xchfrq"));
                System.out.println("Full result: " + result.toJSONString());
            } catch (Exception e) {
                System.err.println("Parse failed for test case " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();
        }
    }
}