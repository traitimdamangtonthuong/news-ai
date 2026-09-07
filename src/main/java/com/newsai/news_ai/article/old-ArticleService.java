// package com.newsai.news_ai.article;

// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import org.springframework.stereotype.Service;


// @Service
// public class ArticleService {
//     public final List<Article> articles = new ArrayList<>();
    
//     public ArticleService() {
//         articles.add(new Article(
//             1L,
//             "HQP vừa mua pc mới",
//             "vãi lồn luôn pc mới ợ",
//             "thế là có máy đánh sơ vêi địt mẹ",
//             "Báo Sa Báu",
//             "https://viet69.com/HQP-pc",
//             "https://viet69.com/images/HQP-pc.jpg",
//             LocalDateTime.of(2026, 9, 1, 8, 0),
//             LocalDateTime.now()
//         ));

//         articles.add(new Article(
//             2L,
//             "HQP tiếp tục đánh sơ vêi",
//             "vãi lồn luôn game nhìn ghẻ vcut vẫn chơi",
//             "Vẫn tiếp tục sự nghiệp survev.io vốn đã rất thành công, tuyển thủ HQP tiếp tục hút trộm thuốc.",
//             "Báo Sa Báu",
//             "https://viet69.com/HQP-pc",
//             "https://viet69.com/images/HQP-pc.jpg",
//             LocalDateTime.of(2026, 9, 1, 9, 36),
//             LocalDateTime.now()
//         ));

//         articles.add(new Article(
//             3L,
//             "HQP nấu cơm rất ngon mặc dù đánh sơ vêi",
//             "vãi lồn luôn game thì nghiện cơm thì ngon",
//             "Vẫn tiếp tục sự nghiệp survev.io vốn đã rất thành công, tuyển thủ HQP mặc dù rất hay hút trộm thuốc vẫn nấu cơm rất ngon.",
//             "Báo Sa Báu",
//             "https://viet69.com/HQP-pc",
//             "https://viet69.com/images/HQP-pc.jpg",
//             LocalDateTime.of(2026, 9, 1, 10, 0),
//             LocalDateTime.now()
//         ));
//     }

//     private long getMaxId() {

//         long maxId = 0;

//         for (Article article : articles) {

//             if (article.getId() > maxId) {
//                 maxId = article.getId();
//             }
//         }

//         return maxId;
//     }   

//     public List<Article> getAllArticles() {
//         return articles;
//     }

//     public Article getArticleById(Long id) {
//         for (Article article : articles) {
//             if (article.getId().equals(id)) {
//                 return article;
//             }
//         }
//         return null;
//     }

//     public Article addArticle(Article article) {
        
//         article.setId(article.getId());

//         article.setCrawledAt(LocalDateTime.now());

//         articles.add(article);

//         return article;
//     }

//     public Article updateArticle(Long id, Article updatedArticle) {
//         for(Article article : articles) {
//             if(article.getId().equals(id)) {
//                 article.setTitle(updatedArticle.getTitle());
//                 article.setDescription(updatedArticle.getDescription());
//                 article.setContent(updatedArticle.getContent());
//                 article.setSource(updatedArticle.getSource());
//                 article.setUrl(updatedArticle.getUrl());
//                 article.setImageUrl(updatedArticle.getImageUrl());
//                 article.setPublishedAt(updatedArticle.getPublishedAt());

//                 return article;
//             }
//         }
//         return null;
//     }

//     public boolean deleteArticle(Long id) {
//         return articles.removeIf(
//             article -> article.getId().equals(id)
//         );
//     }
// }
