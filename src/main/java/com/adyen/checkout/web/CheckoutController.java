package com.adyen.checkout.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CheckoutController {
    private final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    @Value("${ADYEN_CLIENT_KEY}")
    private String clientKey;

    @GetMapping("/")
    public  String preview(){
        return "preview";
    }

    @GetMapping("/checkout")
    public String checkout(Model model){
        model.addAttribute( "clientKey", clientKey);
        return "checkout";
    }

    @GetMapping("/result/{type}")
    public String result(@PathVariable String type, Model model){
        model.addAttribute( "type", type);
        return "result";
    }

}
