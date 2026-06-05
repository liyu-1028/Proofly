package com.lyllink.proofly.controller;

import com.lyllink.proofly.service.BillingService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/public/webhook/xpay")
public class PublicWebhookController {

    private final BillingService billingService;

    public PublicWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * 模拟 XPay 支付页面 (HTML)
     */
    @GetMapping(value = "/mock-pay", produces = MediaType.TEXT_HTML_VALUE)
    public String mockPayPage(@RequestParam("orderNo") String orderNo) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <title>模拟 XPay 支付中心</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #f5f5f5; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .pay-box { background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); padding: 40px; text-align: center; max-width: 400px; width: 100%; }
                        h2 { color: #333; margin-bottom: 20px; }
                        .order-info { background: #fafafa; padding: 15px; border-radius: 4px; text-align: left; margin-bottom: 30px; font-size: 14px; color: #555; }
                        .order-info p { margin: 8px 0; }
                        .btn-pay { background-color: #2a9d8f; color: white; border: none; padding: 12px 30px; font-size: 16px; border-radius: 4px; cursor: pointer; transition: background-color 0.2s; width: 100%; margin-bottom: 12px; }
                        .btn-pay:hover { background-color: #207a6f; }
                        .btn-cancel { background-color: #e76f51; color: white; border: none; padding: 12px 30px; font-size: 16px; border-radius: 4px; cursor: pointer; transition: background-color 0.2s; width: 100%; }
                        .btn-cancel:hover { background-color: #c95c41; }
                        .success-tip { color: #2a9d8f; font-weight: bold; font-size: 18px; margin-top: 20px; display: none; }
                    </style>
                </head>
                <body>
                    <div class="pay-box">
                        <h2>Proofly 支付收银台</h2>
                        <div class="order-info">
                            <p><strong>系统单号：</strong> <span id="order-no">""" + orderNo + """
                            </span></p>
                            <p><strong>支付渠道：</strong> 微信支付 / 支付宝</p>
                            <p><strong>备注：</strong> Pro 高级版套餐升级</p>
                        </div>
                        <button class="btn-pay" onclick="paySuccess()">确认支付 (模拟)</button>
                        <button class="btn-cancel" onclick="window.close()">取消支付</button>
                        <div id="tip" class="success-tip">模拟支付完成！正在通知系统，请关闭本窗口...</div>
                    </div>
                    
                    <script>
                        function paySuccess() {
                            const orderNo = document.getElementById('order-no').innerText.trim();
                            
                            fetch('/api/public/webhook/xpay', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({
                                    orderNo: orderNo,
                                    paymentMethod: 'wechat',
                                    outTradeNo: 'XPAY_MOCK_' + Date.now(),
                                    sign: 'mock_sign_hash'
                                })
                            })
                            .then(res => {
                                if (res.ok) {
                                    document.getElementById('tip').style.display = 'block';
                                    document.querySelector('.btn-pay').disabled = true;
                                    document.querySelector('.btn-cancel').disabled = true;
                                    setTimeout(() => {
                                        window.close();
                                    }, 2000);
                                } else {
                                    alert('模拟回调失败，请检查后端服务');
                                }
                            })
                            .catch(err => {
                                alert('网络错误: ' + err.message);
                            });
                        }
                    </script>
                </body>
                </html>
                """;
    }

    /**
     * Webhook 回调接收接口
     */
    @PostMapping
    public String handleWebhook(@RequestBody Map<String, String> payload) {
        String orderNo = payload.get("orderNo");
        String paymentMethod = payload.get("paymentMethod");
        String outTradeNo = payload.get("outTradeNo");
        String sign = payload.get("sign");

        log.info("收到 XPay 支付回调: orderNo={}, outTradeNo={}, sign={}", orderNo, outTradeNo, sign);

        if (orderNo == null) {
            return "fail: orderNo is null";
        }

        try {
            billingService.handleWebhook(orderNo, paymentMethod, outTradeNo);
            return "success";
        } catch (Exception e) {
            log.error("回调处理失败", e);
            return "fail: " + e.getMessage();
        }
    }
}
