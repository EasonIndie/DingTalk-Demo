// dashscope SDK version >= 2.12.0
import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson.JSONObject;

public class BaiLianAPITest {
    public static void appCall(String bizParams)
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                // Use API Key instead of environment variables
                .apiKey("sk-04375e566a794b4f80ca856e88fbe916")
                .appId("b8c93d71a67e49b8af3860eaa33e95c4")
                .bizParams(JsonUtils.parse(bizParams)).prompt("输出最终结果")
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);
        String text = result.getOutput().getText();
        System.out.println(text);
        JSONObject jsonObject = MyJsonUtils.parsePossiblyWrappedJson(text);
        System.out.println(jsonObject.toString());
    }

    public static void main(String[] args) {
        try {
            String bizParams = "{\"data\":[\n" +
                    "{ \"orderIndex\": 0, \"speaker\": \"agent\", \"textContent\": \"喂。\" },\n" +
                    "{ \"orderIndex\": 1, \"speaker\": \"customer\", \"textContent\": \"喂。\" },\n" +
                    "{ \"orderIndex\": 2, \"speaker\": \"agent\", \"textContent\": \"喂。\" },\n" +
                    "{ \"orderIndex\": 3, \"speaker\": \"agent\", \"textContent\": \"我问你几个信息啊等一下\" },\n" +
                    "{ \"orderIndex\": 4, \"speaker\": \"agent\", \"textContent\": \"就是症状是否缓解。\" },\n" +
                    "{ \"orderIndex\": 5, \"speaker\": \"customer\", \"textContent\": \"缓解。\" },\n" +
                    "{ \"orderIndex\": 6, \"speaker\": \"agent\", \"textContent\": \"没关系。\" },\n" +
                    "{ \"orderIndex\": 7, \"speaker\": \"agent\", \"textContent\": \"皮肤皮肤状态是否良好？\" },\n" +
                    "{ \"orderIndex\": 8, \"speaker\": \"customer\", \"textContent\": \"良好。\" },\n" +
                    "{ \"orderIndex\": 9, \"speaker\": \"agent\", \"textContent\": \"不良好。\" },\n" +
                    "{ \"orderIndex\": 10, \"speaker\": \"agent\", \"textContent\": \"是否按时揭贴？\" },\n" +
                    "{ \"orderIndex\": 11, \"speaker\": \"agent\", \"textContent\": \"今天。\" },\n" +
                    "{ \"orderIndex\": 12, \"speaker\": \"customer\", \"textContent\": \"没有。\" }\n" +
                    "],\"tempNo\":\"abc\"}";
            appCall(bizParams);
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.err.println("message: " + e.getMessage());
            System.out.println("Please refer to documentation: https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
        }
        System.exit(0);
    }
}