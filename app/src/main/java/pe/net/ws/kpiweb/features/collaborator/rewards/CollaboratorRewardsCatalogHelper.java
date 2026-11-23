package pe.net.ws.kpiweb.features.collaborator.rewards;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lógica pura de filtrado, ordenamiento, resolución visual de respaldo y formateo
 * del catálogo real de Premios y Beneficios (SIGD).
 *
 * No inventa premios, puntos ni categorías; opera exclusivamente sobre los atributos
 * devueltos por el servidor.
 */
public final class CollaboratorRewardsCatalogHelper {

    public enum FilterMode {
        ALL,
        EXPIRING_SOON,
        AFFORDABLE
    }

    public enum SortMode {
        DEFAULT,
        EXPIRING_FIRST,
        POINTS_ASC
    }

    public enum IllustrationKind {
        GENERIC_GIFT,
        CINEMA,
        COFFEE,
        VOUCHER,
        TECH
    }

    public enum CatalogEmptyState {
        NONE,
        NO_REGISTERED_REWARDS,
        NO_SEARCH_RESULTS,
        NO_FILTER_RESULTS
    }

    public static final class RewardItem {
        public final int id;
        public final String title;
        public final String description;
        public final String imageUrl;
        public final String category;
        public final int pointsRequired;
        public final int secondsRemaining;
        public final String endDate;
        public final String effectiveStatus;

        public RewardItem(int id,
                          String title,
                          String description,
                          String imageUrl,
                          String category,
                          int pointsRequired,
                          int secondsRemaining,
                          String endDate,
                          String effectiveStatus) {
            this.id = id;
            this.title = title == null ? "" : title.trim();
            this.description = description == null ? "" : description.trim();
            this.imageUrl = imageUrl == null ? "" : imageUrl.trim();
            this.category = category == null ? "" : category.trim();
            this.pointsRequired = Math.max(0, pointsRequired);
            this.secondsRemaining = secondsRemaining;
            this.endDate = endDate == null ? "" : endDate.trim();
            this.effectiveStatus = effectiveStatus == null ? "" : effectiveStatus.trim();
        }

        public boolean hasPointsCost() {
            return pointsRequired > 0;
        }

        public boolean hasRealImage() {
            return !imageUrl.isEmpty();
        }

        public boolean isExpired(long elapsedSinceLoadMs) {
            return secondsRemaining >= 0 && elapsedSinceLoadMs >= secondsRemaining * 1000L;
        }

        public int effectiveSecondsRemaining(long elapsedSinceLoadMs) {
            if (secondsRemaining < 0) return -1;
            long elapsedSec = Math.max(0L, elapsedSinceLoadMs / 1000L);
            return (int) Math.max(0L, secondsRemaining - elapsedSec);
        }
    }

    public static final class FilterState {
        public final String query;
        public final FilterMode mode;
        public final String category;
        public final SortMode sortMode;

        public FilterState(String query, FilterMode mode, String category, SortMode sortMode) {
            this.query = query == null ? "" : query.trim();
            this.mode = mode == null ? FilterMode.ALL : mode;
            this.category = category == null ? "" : category.trim();
            this.sortMode = sortMode == null ? SortMode.DEFAULT : sortMode;
        }

        public boolean hasQuery() {
            return !query.isEmpty();
        }

        public boolean hasActiveFilters() {
            return mode != FilterMode.ALL
                    || !category.isEmpty()
                    || sortMode != SortMode.DEFAULT;
        }

        public int activeFilterCount() {
            int count = 0;
            if (mode != FilterMode.ALL) count++;
            if (!category.isEmpty()) count++;
            if (sortMode != SortMode.DEFAULT) count++;
            return count;
        }
    }

    public static final class AvailableFilterCapabilities {
        public final boolean hasExpiringSoonOption;
        public final boolean hasAffordableOption;
        public final boolean hasExpiringSortOption;
        public final boolean hasPointsSortOption;
        public final List<String> realCategories;

        public AvailableFilterCapabilities(boolean hasExpiringSoonOption,
                                           boolean hasAffordableOption,
                                           boolean hasExpiringSortOption,
                                           boolean hasPointsSortOption,
                                           List<String> realCategories) {
            this.hasExpiringSoonOption = hasExpiringSoonOption;
            this.hasAffordableOption = hasAffordableOption;
            this.hasExpiringSortOption = hasExpiringSortOption;
            this.hasPointsSortOption = hasPointsSortOption;
            this.realCategories = Collections.unmodifiableList(new ArrayList<>(realCategories));
        }

        public boolean hasAnyRealFilter() {
            return hasExpiringSoonOption
                    || hasAffordableOption
                    || hasExpiringSortOption
                    || hasPointsSortOption
                    || !realCategories.isEmpty();
        }
    }

    private static final int EXPIRING_SOON_THRESHOLD_SECONDS = 72 * 3600;

    private CollaboratorRewardsCatalogHelper() {}

    public static List<RewardItem> parseRewards(JsonArray rawArray) {
        List<RewardItem> result = new ArrayList<>();
        if (rawArray == null) return result;
        for (JsonElement element : rawArray) {
            if (element == null || !element.isJsonObject()) continue;
            result.add(parseReward(element.getAsJsonObject()));
        }
        return result;
    }

    public static RewardItem parseReward(JsonObject obj) {
        if (obj == null) {
            return new RewardItem(0, "", "", "", "", 0, -1, "", "");
        }
        int id = getInt(obj, "id_premio", 0);
        String title = getString(obj, "titulo");
        String description = getString(obj, "descripcion");
        String imageUrl = getString(obj, "imagen_url");
        if (imageUrl.isEmpty()) imageUrl = getString(obj, "url_imagen");
        String category = getString(obj, "categoria_nombre");
        if (category.isEmpty()) category = getString(obj, "categoria");
        int pointsRequired = getInt(obj, "puntos_requeridos", 0);
        int secondsRemaining = getInt(obj, "segundos_restantes", -1);
        String endDate = getString(obj, "fin_en");
        String effectiveStatus = getString(obj, "estado_efectivo");
        return new RewardItem(
                id,
                title,
                description,
                imageUrl,
                category,
                pointsRequired,
                secondsRemaining,
                endDate,
                effectiveStatus
        );
    }

    public static List<RewardItem> filterActive(List<RewardItem> items, long elapsedSinceLoadMs) {
        List<RewardItem> active = new ArrayList<>();
        if (items == null) return active;
        for (RewardItem item : items) {
            if (item == null) continue;
            if (!item.isExpired(elapsedSinceLoadMs)) {
                active.add(item);
            }
        }
        return active;
    }

    public static AvailableFilterCapabilities inspectCapabilities(List<RewardItem> activeItems,
                                                                  long elapsedSinceLoadMs) {
        boolean hasExpiringSoon = false;
        boolean hasPointsCost = false;
        boolean hasExpiration = false;
        Set<String> categories = new LinkedHashSet<>();

        if (activeItems != null) {
            for (RewardItem item : activeItems) {
                if (item == null) continue;
                int left = item.effectiveSecondsRemaining(elapsedSinceLoadMs);
                if (left >= 0) {
                    hasExpiration = true;
                    if (left <= EXPIRING_SOON_THRESHOLD_SECONDS) {
                        hasExpiringSoon = true;
                    }
                }
                if (item.hasPointsCost()) {
                    hasPointsCost = true;
                }
                if (!item.category.isEmpty()) {
                    categories.add(item.category);
                }
            }
        }

        return new AvailableFilterCapabilities(
                hasExpiringSoon,
                hasPointsCost,
                hasExpiration && activeItems != null && activeItems.size() > 1,
                hasPointsCost && activeItems != null && activeItems.size() > 1,
                new ArrayList<>(categories)
        );
    }

    public static List<RewardItem> applySearchAndFilters(List<RewardItem> activeItems,
                                                         FilterState state,
                                                         int userPoints,
                                                         long elapsedSinceLoadMs) {
        List<RewardItem> filtered = new ArrayList<>();
        if (activeItems == null || activeItems.isEmpty()) return filtered;

        FilterState safeState = state == null
                ? new FilterState("", FilterMode.ALL, "", SortMode.DEFAULT)
                : state;
        String normQuery = normalize(safeState.query);
        String normCategory = normalize(safeState.category);

        for (RewardItem item : activeItems) {
            if (item == null) continue;

            if (!normQuery.isEmpty()) {
                boolean matchesTitle = normalize(item.title).contains(normQuery);
                boolean matchesDesc = normalize(item.description).contains(normQuery);
                boolean matchesCat = normalize(item.category).contains(normQuery);
                if (!matchesTitle && !matchesDesc && !matchesCat) continue;
            }

            if (!normCategory.isEmpty() && !normalize(item.category).equals(normCategory)) {
                continue;
            }

            if (safeState.mode == FilterMode.EXPIRING_SOON) {
                int left = item.effectiveSecondsRemaining(elapsedSinceLoadMs);
                if (left < 0 || left > EXPIRING_SOON_THRESHOLD_SECONDS) continue;
            } else if (safeState.mode == FilterMode.AFFORDABLE) {
                if (!item.hasPointsCost() || userPoints < item.pointsRequired) continue;
            }

            filtered.add(item);
        }

        if (safeState.sortMode == SortMode.EXPIRING_FIRST) {
            filtered.sort(Comparator.comparingInt(item -> {
                int left = item.effectiveSecondsRemaining(elapsedSinceLoadMs);
                return left < 0 ? Integer.MAX_VALUE : left;
            }));
        } else if (safeState.sortMode == SortMode.POINTS_ASC) {
            filtered.sort(Comparator.comparingInt(item ->
                    item.hasPointsCost() ? item.pointsRequired : Integer.MAX_VALUE));
        }

        return filtered;
    }

    public static CatalogEmptyState resolveEmptyState(int totalActiveCount,
                                                      int displayedCount,
                                                      FilterState state) {
        if (displayedCount > 0) return CatalogEmptyState.NONE;
        if (totalActiveCount <= 0) return CatalogEmptyState.NO_REGISTERED_REWARDS;
        if (state != null && state.hasActiveFilters() && !state.hasQuery()) {
            return CatalogEmptyState.NO_FILTER_RESULTS;
        }
        if (state != null && state.hasQuery() && !state.hasActiveFilters()) {
            return CatalogEmptyState.NO_SEARCH_RESULTS;
        }
        if (state != null && state.hasActiveFilters()) {
            return CatalogEmptyState.NO_FILTER_RESULTS;
        }
        return CatalogEmptyState.NO_SEARCH_RESULTS;
    }

    /**
     * Resuelve una ilustración de respaldo SOLO cuando no hay imagen del servidor.
     * Si el título o categoría real no coincide claramente con una temática conocida,
     * devuelve GENERIC_GIFT para no representar de forma engañosa el premio.
     */
    public static IllustrationKind resolveFallbackIllustration(String title, String category) {
        String text = normalize((title == null ? "" : title) + " " + (category == null ? "" : category));
        if (text.isEmpty()) return IllustrationKind.GENERIC_GIFT;

        if (containsAny(text, "cine", "pelicula", "popcorn", "palomita", "entrada", "entradas", "boleto", "boletos")) {
            return IllustrationKind.CINEMA;
        }
        if (containsAny(text, "cafe", "cafeteria", "starbucks", "capuchino", "latte", "desayuno", "bebida")) {
            return IllustrationKind.COFFEE;
        }
        if (containsAny(text, "vale", "cupon", "descuento", "bono", "gift card", "giftcard", "tarjeta de regalo")) {
            return IllustrationKind.VOUCHER;
        }
        if (containsAny(text, "audifono", "auricular", "headphone", "tecnologia", "gadget", "mouse", "teclado", "parlante")) {
            return IllustrationKind.TECH;
        }
        return IllustrationKind.GENERIC_GIFT;
    }

    public static String formatPointsBadge(int points) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        return nf.format(Math.max(0, points)) + " pts";
    }

    public static String formatValidityLabel(int secondsRemaining, String endDate) {
        if (secondsRemaining >= 0) {
            if (secondsRemaining < 3600) {
                return "Vence en < 1 h";
            }
            int hours = secondsRemaining / 3600;
            if (hours < 24) {
                return "Vigente · " + hours + " h";
            }
            int days = hours / 24;
            return days == 1 ? "Vigente · 1 día" : "Vigente · " + days + " días";
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            String compactDate = endDate.length() >= 10 ? endDate.substring(0, 10) : endDate;
            return "Hasta " + compactDate;
        }
        return "Publicado";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static String getString(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
