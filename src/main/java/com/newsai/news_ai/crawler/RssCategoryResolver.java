package com.newsai.news_ai.crawler;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Tự động bóc tách slug từ đường dẫn RSS URL và ánh xạ sang tên Danh mục tiếng Việt.
 * Ví dụ: "https://vnexpress.net/rss/the-gioi.rss" -> "the-gioi" -> "Thế giới"
 */
@Component
public class RssCategoryResolver {

    // Danh sách các feed tổng hợp không thuộc chuyên mục cụ thể
    private static final Set<String> GENERAL_FEEDS = Set.of(
            "trang-chu", "tin-nong", "home", "index"
    );

    // Bảng từ điển ánh xạ slug phổ biến của các báo Việt Nam (VnExpress, Tuổi Trẻ, Thanh Niên...)
    private static final Map<String, String> SLUG_TO_CATEGORY_NAME = Map.ofEntries(
            Map.entry("the-gioi", "Thế giới"),
            Map.entry("thegioi", "Thế giới"),
            Map.entry("thoi-su", "Thời sự"),
            Map.entry("thoisu", "Thời sự"),
            Map.entry("kinh-doanh", "Kinh doanh"),
            Map.entry("kinhdoanh", "Kinh doanh"),
            Map.entry("bat-dong-san", "Bất động sản"),
            Map.entry("batdongsan", "Bất động sản"),
            Map.entry("khoa-hoc", "Khoa học công nghệ"),
            Map.entry("khoahoc", "Khoa học công nghệ"),
            Map.entry("khoa-hoc-cong-nghe", "Khoa học công nghệ"),
            Map.entry("suc-khoe", "Sức khỏe"),
            Map.entry("suckhoe", "Sức khỏe"),
            Map.entry("the-thao", "Thể thao"),
            Map.entry("thethao", "Thể thao"),
            Map.entry("giai-tri", "Giải trí"),
            Map.entry("giaitri", "Giải trí"),
            Map.entry("phap-luat", "Pháp luật"),
            Map.entry("phapluat", "Pháp luật"),
            Map.entry("giao-duc", "Giáo dục"),
            Map.entry("giaoduc", "Giáo dục"),
            Map.entry("gia-dinh", "Đời sống"),
            Map.entry("doi-song", "Đời sống"),
            Map.entry("doisong", "Đời sống"),
            Map.entry("du-lich", "Du lịch"),
            Map.entry("dulich", "Du lịch"),
            Map.entry("so-hoa", "Số hóa"),
            Map.entry("sohoa", "Số hóa"),
            Map.entry("cong-nghe", "Khoa học công nghệ"),
            Map.entry("congnghe", "Khoa học công nghệ"),
            Map.entry("xe", "Xe"),
            Map.entry("oto-xe-may", "Xe"),
            Map.entry("otoxemay", "Xe"),
            Map.entry("quan-su", "Quân sự"),
            Map.entry("quansu", "Quân sự"),
            Map.entry("nhip-song-tre", "Nhịp sống trẻ"),
            Map.entry("chao-ngay-moi", "Chào ngày mới"),
            Map.entry("goc-nhin", "Góc nhìn"),
            Map.entry("spotlight", "Spotlight"),
            Map.entry("thu-gian", "Thư giãn"),
            Map.entry("y-kien", "Ý kiến"),
            Map.entry("tam-su", "Tâm sự"),
            Map.entry("tin-moi-nhat", "Tin mới nhất")
    );

    /**
     * Bóc tách và trả về tên Category tiếng Việt từ đường link RSS.
     * Trả về null nếu là feed tổng hợp hoặc không nhận diện được.
     */
    public String resolveCategoryName(String rssUrl) {
        String slug = extractSlug(rssUrl);
        if (slug == null) {
            return null;
        }

        // Gọt bỏ mã ID số ở đuôi nếu có (ví dụ: "thegioi-1231" -> "thegioi")
        slug = slug.replaceAll("-\\d+$", "");

        if (GENERAL_FEEDS.contains(slug)) {
            return null;
        }

        return SLUG_TO_CATEGORY_NAME.get(slug);
    }

    /**
     * Cắt lấy slug từ URL:
     * "https://vnexpress.net/rss/the-gioi.rss?utm=..." -> "the-gioi"
     */
    public String extractSlug(String rssUrl) {
        if (rssUrl == null || rssUrl.isBlank()) {
            return null;
        }

        try {
            // Loại bỏ query string (ví dụ ?utm_source=...)
            String cleanUrl = rssUrl.split("\\?")[0].trim();

            // Lấy tên file cuối cùng
            URI uri = URI.create(cleanUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            String fileName = path.substring(path.lastIndexOf('/') + 1);

            // Bỏ đuôi file (.rss hoặc .xml)
            if (fileName.endsWith(".rss")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            } else if (fileName.endsWith(".xml")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }

            fileName = fileName.toLowerCase().trim();
            return fileName.isEmpty() ? null : fileName;
        } catch (Exception e) {
            return null;
        }
    }
}
