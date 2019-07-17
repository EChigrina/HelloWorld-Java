package com.intuit.developer.helloworld.controller;

import com.intuit.developer.helloworld.client.OAuth2PlatformClientFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

public class WebhooksController {
    private static final Logger logger = Logger.getLogger(WebhooksController.class);
    private static final String SIGNATURE = "intuit-signature";
    private static final String SUCCESS = "Success";
    private static final String ERROR = "Error";
    @Autowired
    OAuth2PlatformClientFactory factory;
    @RequestMapping(value = "/webhooks", method = RequestMethod.POST)
    @ResponseBody
    public void webhooks(@RequestHeader(SIGNATURE) String signature, @RequestBody String payload) {

        // if signature is empty return 401
        if (!StringUtils.hasText(signature)) {
            logger.info("empty signature");
        }

        // if payload is empty, don't do anything
        if (!StringUtils.hasText(payload)) {
            logger.info("empty payload");
        }

        logger.info("request recieved ");

        //if request valid - push to queue
       /* if (securityService.isRequestValid(signature, payload)) {
            queueService.add(payload);
        } else {
            return new ResponseEntity<>(new ResponseWrapper(ERROR), HttpStatus.FORBIDDEN);
        }

        LOG.info("response sent ");
        return new ResponseEntity<>(new ResponseWrapper(SUCCESS), HttpStatus.OK);*/
    }
}
