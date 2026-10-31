const TAG_PATTERN = /<[^>]*>/g;
const IMG_SRC_PATTERN = /<img[^>]+src=["']([^"']+)["']/i;

/**
 * Decodes HTML entities (`&amp;`, `&#039;`, ...) using a throwaway
 * <textarea>, which lets the browser apply the correct decoding rules.
 */
function decodeHtmlEntities(html: string): string {
  const area = document.createElement("textarea");
  area.innerHTML = html;
  return area.value;
}

/**
 * Strips HTML tags out of a feed description (RSS descriptions are often raw
 * HTML such as `<a href=...><img .../></a></br>Lead text...`) and collapses
 * the remaining whitespace into a single clean paragraph. Returns null when
 * nothing usable remains.
 */
export function stripHtmlToText(html: string | null): string | null {
  if (!html) return null;

  const withoutTags = html.replace(TAG_PATTERN, " ");
  const collapsed = decodeHtmlEntities(withoutTags).replace(/\s+/g, " ").trim();
  return collapsed.length > 0 ? collapsed : null;
}

/**
 * Extracts the URL of the first `<img src="...">` found in an HTML
 * description, so the article card can show the real thumbnail even when the
 * feed did not provide an `<enclosure>` image.
 */
export function extractFirstImageUrl(html: string | null): string | null {
  if (!html) return null;

  const match = IMG_SRC_PATTERN.exec(html);
  return match ? match[1] : null;
}