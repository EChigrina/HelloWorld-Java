package com.intuit.developer.helloworld.controller;

import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
       /* HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("payload", payload);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity( "https://dxfeed-quickbooksintegration.cs17.force.com/services/apexrest/SalesforceQuickbooksIntegration", request , String.class );
        //logger.info(response);*/
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("https://dxfeed-quickbooksintegration.cs17.force.com/services/apexrest/SalesforceQuickbooksIntegration");

// Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("payload", payload));
        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            httppost.setHeader("Content-Type", "application/json; charset=UTF-8");

//Execute and get the response.
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
        }
        catch(IOException e) {

        }
    }
}
