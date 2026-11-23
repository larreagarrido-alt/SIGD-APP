package pe.net.ws.kpiweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessRecommendationRepository;

public class WellnessRecommendationRepositoryTest {
    @Test public void optInIsRequiredAndEachAccountKeepsItsOwnPreferences() {
        SharedPreferences prefs = memoryPreferences();
        WellnessRecommendationRepository first = new WellnessRecommendationRepository(prefs, 11);
        WellnessRecommendationRepository second = new WellnessRecommendationRepository(prefs, 12);
        assertFalse(first.rate("202611", "workload", true));
        first.setEnabled(true);
        assertTrue(first.rate("202611", "workload", true));
        assertFalse(first.rate("202611", "workload", true));
        assertEquals(Integer.valueOf(1), first.preferences().get("workload"));
        assertTrue(second.preferences().isEmpty());
        second.setEnabled(true);
        assertEquals(Integer.valueOf(0), second.preferences().get("workload"));
        first.setEnabled(false);
        assertFalse(first.rate("202611", "workload", false));
        assertTrue(first.preferences().isEmpty());
        first.clear();
        first.setEnabled(true);
        assertEquals(Integer.valueOf(0), first.preferences().get("workload"));
        assertEquals(0, first.vote("202611", "workload"));
    }

    private SharedPreferences memoryPreferences() {
        Map<String, Object> values = new HashMap<>();
        return (SharedPreferences) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{SharedPreferences.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAll": return new HashMap<>(values);
                        case "getBoolean": return values.getOrDefault(args[0], args[1]);
                        case "getInt": return values.getOrDefault(args[0], args[1]);
                        case "edit": return Proxy.newProxyInstance(getClass().getClassLoader(),
                                new Class[]{SharedPreferences.Editor.class}, (editor, editMethod, editArgs) -> {
                                    switch (editMethod.getName()) {
                                        case "putBoolean":
                                        case "putInt": values.put((String) editArgs[0], editArgs[1]); return editor;
                                        case "remove": values.remove(editArgs[0]); return editor;
                                        case "apply": return null;
                                        case "commit": return true;
                                        default: throw new UnsupportedOperationException(editMethod.getName());
                                    }
                                });
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}
