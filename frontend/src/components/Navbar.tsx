import { useMemo } from "react";
import type { Category } from "../api/articles";

interface NavbarProps {
  categories: Category[];
  selectedCategory: string;
  onSelectCategory: (categoryId: string) => void;
}

// Danh sách thứ tự ưu tiên các chuyên mục trên thanh Navigation Bar
// Bạn có thể dễ dàng thay đổi thứ tự hoặc thêm/bớt chuyên mục tại đây
const PREFERRED_ROOT_ORDER = [
  "Tin mới nhất",
  "Thời sự",
  "Thế giới",
  "Kinh doanh",
  "Bất động sản",
  "Khoa học công nghệ",
  "Thể thao",
  "Giải trí",
  "Sức khỏe",
  "Đời sống",
  "Giáo dục",
  "Xe",
  "Du lịch",
  "Pháp luật",
  "Góc nhìn",
  "Tâm sự",
  "Thư giãn",
  "Spotlight",
];

function getCategoryPriority(cat: Category): number {
  const name = cat.name.toLowerCase().trim();
  const slug = (cat.slug || "").toLowerCase().trim();

  for (let i = 0; i < PREFERRED_ROOT_ORDER.length; i++) {
    const target = PREFERRED_ROOT_ORDER[i].toLowerCase().trim();
    if (name === target || slug === target) {
      return i;
    }
  }
  return 999;
}

export default function Navbar({
  categories,
  selectedCategory,
  onSelectCategory,
}: NavbarProps) {
  // 1. Lọc và sắp xếp danh mục gốc theo thứ tự ưu tiên
  const rootCategories = useMemo(() => {
    return categories
      .filter((c) => !c.parent)
      .sort((a, b) => {
        const pA = getCategoryPriority(a);
        const pB = getCategoryPriority(b);
        if (pA !== pB) {
          return pA - pB;
        }
        return a.name.localeCompare(b.name, "vi");
      });
  }, [categories]);

  // 2. Gom nhóm danh mục con theo parentId và sắp xếp theo A-Z
  const subCategoriesMap = useMemo(() => {
    const map = new Map<number, Category[]>();
    for (const c of categories) {
      if (c.parent?.id) {
        const list = map.get(c.parent.id) || [];
        list.push(c);
        map.set(c.parent.id, list);
      }
    }
    // Sắp xếp các danh mục con trong popup theo thứ tự chữ cái A-Z
    for (const list of map.values()) {
      list.sort((a, b) => a.name.localeCompare(b.name, "vi"));
    }
    return map;
  }, [categories]);

  return (
    <nav className="news-navbar">
      <div className="nav-container">
        {/* Nút xem Tất cả / Trang chủ */}
        <button
          type="button"
          className={`nav-link ${selectedCategory === "" ? "active" : ""}`}
          onClick={() => onSelectCategory("")}
        >
          Trang chủ
        </button>

        {/* Danh sách danh mục cha theo thứ tự đã sắp xếp */}
        {rootCategories.map((root) => {
          const subs = subCategoriesMap.get(root.id) || [];
          const hasSubs = subs.length > 0;

          const isParentActive = String(root.id) === selectedCategory;
          const isChildActive = subs.some((s) => String(s.id) === selectedCategory);
          const isActive = isParentActive || isChildActive;

          return (
            <div key={root.id} className="nav-item">
              <button
                type="button"
                className={`nav-link ${isActive ? "active" : ""}`}
                onClick={() => onSelectCategory(String(root.id))}
              >
                <span>{root.name}</span>
                {hasSubs && <span className="arrow-down">▾</span>}
              </button>

              {/* Dropdown Menu hiện ra khi hover */}
              {hasSubs && (
                <div className="dropdown-menu">
                  <button
                    type="button"
                    className={`dropdown-item header-item ${
                      isParentActive ? "active" : ""
                    }`}
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectCategory(String(root.id));
                    }}
                  >
                    Tất cả {root.name}
                  </button>
                  <div className="dropdown-divider" />
                  {subs.map((sub) => (
                    <button
                      key={sub.id}
                      type="button"
                      className={`dropdown-item ${
                        String(sub.id) === selectedCategory ? "active" : ""
                      }`}
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectCategory(String(sub.id));
                      }}
                    >
                      {sub.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </nav>
  );
}
