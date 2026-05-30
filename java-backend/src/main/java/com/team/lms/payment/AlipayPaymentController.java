package com.team.lms.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/alipay")
public class AlipayPaymentController {

    private final AlipaySandboxPaymentService alipaySandboxPaymentService;

    @PostMapping("/notify")
    public String handleNotify(@RequestParam Map<String, String> params) {
        return alipaySandboxPaymentService.handleNotify(params) ? "success" : "failure";
    }
}
