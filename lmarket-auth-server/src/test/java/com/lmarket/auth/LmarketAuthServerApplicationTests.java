package com.lmarket.auth;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayUserPicture;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class LmarketAuthServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void testAlipay(){
        String APP_ID = "2017022205812268";
        String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCYL+2kWPOPXZ4wYkUfqg+DTgqeT84+cZiXtCLxN8Ix12l4CY0UHaBC2eLhaPP5PXIjPTdoLCmhsqhGyvMqssTNVMUgQ5FVl9xJXLuOBhvotk8SRGkjzR0oCpfByFdJccDrpMdhu6XP9F3tq7i4CKpiZAtG8WdUjEeoE4nbUU7oGNMk/yZsB7XxORYSZn7jAzEGso1P8XLJlSRFIt6M//hkOBOJcgbBv4dk8WJUYCBtDyuZqpVk1bZjVoT9RKWE+j0sTQDl3wkzrpDM0+IsYx80K2s0G3eJlKVNDFJPEP5s2Tjq2pTVmoc46gWgSBBC2pzNquAWnZ/eUI6H2rpht5ItAgMBAAECggEATdMkqYi7dLMhNAjs5FZo/HKqR270P0aEDAAYZlZnPmm0C6iGtqS30bfup+qt6OUdOxwQ+3qqB4B/QUSDuXAV6BLTg75J0gWIpxN+7g/9sbGZGfRV9/LqWBGirXwv9uTvDRIEy9vT3VZPujy4h5QW3Z3fjnXgq4HZGwBy3O7ldRaC4dk4aIB240YmEbTiyK8HW0o2GMBFdOa2Qv48pMirycIuz+4kGJ5bc088lBtKuqk9Wvdp5nfPYE1iKWxfzCxUkj3BZ9PSCwoskOr7opSnTCfqDH/38tpKTquDgX/wLYknxtTnEodd59h13VsbIR3sJ5mpj54olvmVctYRm3mhWQKBgQDKhh361FF6OtecR00BLuD03GlfTyg3jAYm8G38aA81VKmRCOGdXLAEipaKMsxk1UVnWF7pdH0052RcfamceAjMbv1ss9XXi10dhNPCPGhXBhjv9F1Inw1xTZlpRecW/qA5+LidbPbJ43ap0QBudT6Szea42OwhTiiPpJ3RZcYLCwKBgQDAXzvmTCD+tmsuifsZ1wNUNYSVBm+RZRg7JQm0hM9kZmR6U/pb3l+UEqlPDctKISk0AZV0MXmBd9C2fmzX0sBVczbFe53nOG0it1wDS8cxGPr7ycWyO457QjYqtGj4g7Uc80nfB9ruysbV4DrnBb7FCp1LZHA+b6E3jC1HM73apwKBgEYUfs9LJR6cuftUGOtt414X30ahx5LhG0fiLs0G3SxhLYQIhLnFVwtbQZh015GU0z1dsQoryx/BalMvEMAaMUE8ruksmbTKfuA2FGjEKn7il0KBVr5Ie4hXh9yL2dl1JtCopomjKQrXcTp2HiYa4BpAsbQJLDyiJRr/pbTN+OfdAoGBAKC+cyTqtB9ieok2ztU2rxdsaM7Fw/AwE2FIf6WR3GI+lPMKvQaK+D6YoVTnZ9kXgIBuWl3vAWLHpWT4myZ0ejZswXWe58DPi5IYuRAf/9/sybwfS8y0q0gfeXeRrK0MEQiR2kY9IBms4xzP8ygbyt6HEgxvbsMdMA9kWGlIuPm9AoGBAJ/dgc1vq9to1dc384ptPh1AAXXjyuA2dB5xnEcYQUvrgVUcApWnffDyi1z7eWxGd8PUDgBGWO6o6fbqj/kot9BAggMKJv/Qg7moVXbXXayipvAEGlnkLXffWoDyxWwAkoSI7df+Nj3ZVo9JJE4eAHHFziuAursJuKrqHyI/9Bw+";

        String CHARSET = "GBK";
        String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5bDCxb0W5PJMCP7diUUOGmbtVzYnXU++FGP3v2kanjg2dqG2gqH9wJZpcARQgN0yOc7RH8A1ze2rD850+iA0yTRcMcPwLeiVhPmIayfSHatGTdK9pTpRh4HVRYnm0JWC8VTOHrqChnbEog+MIhYxaTMwqjc6YOKM+HpG8o2lR3EVwTKQi/TY0hnPYpN4owziJYgppAKB9plR38zVgfjJJX8+qR5+cXkHZn3yPKDQLmcUZ1JcqKbxy9BJyZHnvcEWM5jrDypBD70aYuTN6KDDhCcjQmlJbLBMDQG35SRa5TR7Quvzt/r7EQdKrU62Wl5lP1vSy3BvuciTp84i3TfSswIDAQAB";

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                APP_ID, APP_PRIVATE_KEY, "json", CHARSET, ALIPAY_PUBLIC_KEY, "RSA2");
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode("fe1895a90e8c4377a6f9f13be868YX41");
        request.setGrantType("authorization_code");
        try {
            AlipayUserInfoShareRequest infoShareRequest = new AlipayUserInfoShareRequest();
            AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(request);

            String accessToken = oauthTokenResponse.getAccessToken();
            AlipayUserInfoShareResponse infoShareResponse = alipayClient.execute(infoShareRequest, accessToken);
            System.out.println(infoShareResponse.getBody());
            System.out.println(infoShareResponse.getAddress());
            System.out.println(infoShareResponse.getArea());
            System.out.println(infoShareResponse.getCity());
            System.out.println(infoShareResponse.getAvatar());
            System.out.println(infoShareResponse.getNickName());
            System.out.println(infoShareResponse.getGender());
            System.out.println(infoShareResponse.getLicenseNo());
            System.out.println(infoShareResponse.getCertNo());
            System.out.println(infoShareResponse.getEmail());
            System.out.println(infoShareResponse.getPersonBirthday());
            System.out.println(infoShareResponse.getTaobaoId());
        } catch (AlipayApiException e) {
            //处理异常
            e.printStackTrace();
        }

    }

}
