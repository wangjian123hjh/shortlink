package com.nageoffer.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ShortLonkNotfundController {
    @RequestMapping("/page/notfund")
    public String notfund(){
        return "notfund";
    }
}
