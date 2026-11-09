import { useMemo } from "react";
import type { Category } from "../api/articles";

interface NavbarProps {
  categories: Category[];
  selectedCategory: string;
  onSelectCategory: (categoryId: string) => void;
}

export default function Navbar({
  categories,
  selectedCategory,
  onSelectCategory,
}: NavbarProps) {
  // 1. Lọc danh mục gốc (không có parent)
  const rootCategories = useMemo(() => {
    return categories.filter((c) => !c.parent);
  }, [categories]);

  // 2. Gom nhóm danh mục con theo parentId
  const subCategoriesMap = useMemo(() => {
    const map = new Map<number, Category[]>();
    for (const c of categories) {
      if (c.parent?.id) {
        const list = map.get(c.parent.id) || [];
        list.push(c);
        map.set(c.parent.id, list);
      }
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

        {/* Danh sách danh mục cha */}
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
