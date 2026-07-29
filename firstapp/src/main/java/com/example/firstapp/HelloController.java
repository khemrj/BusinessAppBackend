package com.example.firstapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    
    @RequestMapping("/hello")
    public String hello(){
        return "Hello there Mr Khem from spring boot hi there";
}
}
