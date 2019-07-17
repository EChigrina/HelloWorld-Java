package com.intuit.developer.helloworld.controller;

import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.RestTemplate;

@Controller
public class WebhooksController {
    private static final Logger logger = Logger.getLogger(WebhooksController.class);
    private static final String SIGNATURE = "intuit-signature";
    private static final String SUCCESS = "Success";
    private static final String ERROR = "Error";

    @Autowired
    OAuth2PlatformClientFactory factory;

    @GetMapping("/webhooks")
    public String webhooks(Model model) {
        model.addAttribute("response", "webhooks");
        return "connected";
    }
    @PostMapping("/webhooks")
    //@ResponseBody
    public void webhooks(@RequestHeader(SIGNATURE) String signature, @RequestBody String payload, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("payload", payload);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity( "https://dxfeed-quickbooksintegration.cs17.force.com/services/apexrest/SalesforceQuickbooksIntegration", request , String.class );
        //logger.info(response);
    }
}
