package pe.net.ws.kpiweb.features.collaborator.rewards;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.AvailableFilterCapabilities;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.CatalogEmptyState;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.FilterMode;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.FilterState;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.IllustrationKind;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.RewardItem;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.SortMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollaboratorRewardsCatalogHelperTest {

    @Test
    public void parseRewards_readsServerFieldsWithoutFabricatingPointsOrCategory() {
        JsonObject raw = new JsonObject();
        raw.addProperty("id_premio", 7);
        raw.addProperty("titulo", "Reconocimiento trimestral");
        raw.addProperty("descripcion", "Destacado por puntualidad");
        raw.addProperty("imagen_url", "/websiteplus-api/api/v1/premios/7/imagen");
        raw.addProperty("segundos_restantes", 7200);
        raw.addProperty("fin_en", "2026-12-31 23:59:59");
        raw.addProperty("estado_efectivo", "ACTIVO");

        JsonArray arr = new JsonArray();
        arr.add(raw);

        List<RewardItem> parsed = CollaboratorRewardsCatalogHelper.parseRewards(arr);
        assertEquals(1, parsed.size());
        RewardItem item = parsed.get(0);
        assertEquals(7, item.id);
        assertEquals("Reconocimiento trimestral", item.title);
        assertEquals("", item.category);
        assertFalse(item.hasPointsCost());
        assertTrue(item.hasRealImage());
        assertEquals("Vigente · 2 h", CollaboratorRewardsCatalogHelper.formatValidityLabel(item.secondsRemaining, item.endDate));
    }

    @Test
    public void filterActive_excludesExpiredItems() {
        RewardItem expiring = new RewardItem(1, "Uno", "Desc", "", "", 0, 5, "", "ACTIVO");
        RewardItem active = new RewardItem(2, "Dos", "Desc", "", "", 0, 3600, "", "ACTIVO");
        RewardItem noLimit = new RewardItem(3, "Tres", "Desc", "", "", 0, -1, "", "ACTIVO");

        List<RewardItem> result = CollaboratorRewardsCatalogHelper.filterActive(
                Arrays.asList(expiring, active, noLimit), 10_000L);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).id);
        assertEquals(3, result.get(1).id);
    }

    @Test
    public void searchAndFilter_combinesQueryCategoryPointsAndSort() {
        RewardItem coffee = new RewardItem(1, "Café Premium", "Desayuno especial", "", "Gastronomía", 150, 10_000, "", "ACTIVO");
        RewardItem tech = new RewardItem(2, "Audífonos Pro", "Cancelación de ruido", "", "Tecnología", 600, 200_000, "", "ACTIVO");
        RewardItem cinema = new RewardItem(3, "Entradas al Cine", "Dos boletos", "", "Entretenimiento", 300, 3_600, "", "ACTIVO");
        List<RewardItem> items = Arrays.asList(coffee, tech, cinema);

        AvailableFilterCapabilities caps = CollaboratorRewardsCatalogHelper.inspectCapabilities(items, 0L);
        assertTrue(caps.hasAffordableOption);
        assertTrue(caps.hasExpiringSoonOption);
        assertTrue(caps.hasPointsSortOption);
        assertEquals(3, caps.realCategories.size());

        // Search accent-insensitive + affordable filter (user has 350 pts)
        FilterState affordableState = new FilterState("cafe", FilterMode.AFFORDABLE, "", SortMode.POINTS_ASC);
        List<RewardItem> coffeeOnly = CollaboratorRewardsCatalogHelper.applySearchAndFilters(items, affordableState, 350, 0L);
        assertEquals(1, coffeeOnly.size());
        assertEquals(1, coffeeOnly.get(0).id);

        // Sort expiring first across affordable items
        FilterState expiringSort = new FilterState("", FilterMode.AFFORDABLE, "", SortMode.EXPIRING_FIRST);
        List<RewardItem> sorted = CollaboratorRewardsCatalogHelper.applySearchAndFilters(items, expiringSort, 350, 0L);
        assertEquals(2, sorted.size());
        assertEquals(3, sorted.get(0).id);
        assertEquals(1, sorted.get(1).id);
    }

    @Test
    public void resolveEmptyState_distinguishesCatalogEmptySearchAndFilters() {
        FilterState idle = new FilterState("", FilterMode.ALL, "", SortMode.DEFAULT);
        FilterState searchOnly = new FilterState("viaje", FilterMode.ALL, "", SortMode.DEFAULT);
        FilterState filterOnly = new FilterState("", FilterMode.EXPIRING_SOON, "", SortMode.DEFAULT);

        assertEquals(CatalogEmptyState.NO_REGISTERED_REWARDS,
                CollaboratorRewardsCatalogHelper.resolveEmptyState(0, 0, idle));
        assertEquals(CatalogEmptyState.NO_SEARCH_RESULTS,
                CollaboratorRewardsCatalogHelper.resolveEmptyState(3, 0, searchOnly));
        assertEquals(CatalogEmptyState.NO_FILTER_RESULTS,
                CollaboratorRewardsCatalogHelper.resolveEmptyState(3, 0, filterOnly));
        assertEquals(CatalogEmptyState.NONE,
                CollaboratorRewardsCatalogHelper.resolveEmptyState(3, 1, searchOnly));
    }

    @Test
    public void resolveFallbackIllustration_matchesKnownThemesAndDefaultsToGenericGift() {
        assertEquals(IllustrationKind.CINEMA,
                CollaboratorRewardsCatalogHelper.resolveFallbackIllustration("Entradas de Cine Doble", ""));
        assertEquals(IllustrationKind.COFFEE,
                CollaboratorRewardsCatalogHelper.resolveFallbackIllustration("Café de especialidad", ""));
        assertEquals(IllustrationKind.VOUCHER,
                CollaboratorRewardsCatalogHelper.resolveFallbackIllustration("Vale de descuento 20%", ""));
        assertEquals(IllustrationKind.TECH,
                CollaboratorRewardsCatalogHelper.resolveFallbackIllustration("Audífonos inalámbricos", ""));
        assertEquals(IllustrationKind.GENERIC_GIFT,
                CollaboratorRewardsCatalogHelper.resolveFallbackIllustration("Reconocimiento al Mérito", "Institucional"));
    }
}
