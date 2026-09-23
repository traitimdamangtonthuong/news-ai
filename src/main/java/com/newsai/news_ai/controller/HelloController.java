// package com.newsai.news_ai.controller;

// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestParam;
// import java.util.Map;

// @RestController // Class này nhận HTTP request và trả response.
// public class HelloController {

//     @GetMapping("/hello") // Khi có HTTP GET request tới /hello, gọi method này.
//     public String hello(){
//         return "Xin Chao Quy Phuoc!";
//     }
    
//     @GetMapping("about")
//     public String about() {
//         return "Toi la HQP!";
//     }
    
//     @GetMapping("/api/status")
//     public Map<String, Object> status(){
//         return Map.of(
//             "application", "News AI",
//             "status", "running",
//             "version", 1
//         );
//     }
    
// }
