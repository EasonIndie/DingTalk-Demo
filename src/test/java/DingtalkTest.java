import com.alibaba.dashscope.exception.ApiException;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.response.OapiV2UserGetResponse;

public class DingtalkTest {

        public static void main(String[] args) {
            try {
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get");
                OapiV2UserGetRequest req = new OapiV2UserGetRequest();
                req.setUserid("2001051333950200");
                req.setLanguage("zh_CN");
                OapiV2UserGetResponse rsp = client.execute(req, "38eada08971633e685f5ed12148ab472");
                System.out.println(rsp.getBody());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
