package com.intuit.developer.helloworld.controller;

import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WebhooksController {
    private static final Logger logger = Logger.getLogger(WebhooksController.class);
    private static final String SIGNATURE = "intuit-signature";
    private static final String SUCCESS = "Success";
    private static final String ERROR = "Error";

    @Autowired
    OAuth2PlatformClientFactory factory;

   /* @GetMapping("/webhooks")
    public String webhooks(Model model) {
        return "/connected";
    }

    @PostMapping("/webhooks")
    @ResponseBody
    public void webhooks(@RequestHeader(SIGNATURE) String signature, @RequestBody String payload) {

    }*/
}
