package pe.net.ws.kpiweb.features.auth;

import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * In-memory SharedPreferences implementation for host JVM unit tests.
 */
public class MockSharedPreferences implements SharedPreferences {
    private final Map<String, Object> storage = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(storage);
    }

    @Override
    public String getString(String key, String defValue) {
        Object val = storage.get(key);
        return val instanceof String ? (String) val : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object val = storage.get(key);
        return val instanceof Integer ? (Integer) val : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object val = storage.get(key);
        return val instanceof Long ? (Long) val : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object val = storage.get(key);
        return val instanceof Float ? (Float) val : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object val = storage.get(key);
        return val instanceof Boolean ? (Boolean) val : defValue;
    }

    @Override
    public boolean contains(String key) {
        return storage.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new MockEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    public interface CommitFilter {
        boolean shouldFail(Map<String, Object> changes, boolean clearRequested, boolean removeRequested);
    }

    private boolean failCommits = false;
    private CommitFilter commitFilter = null;

    public void setFailCommits(boolean failCommits) {
        this.failCommits = failCommits;
    }

    public void setCommitFilter(CommitFilter commitFilter) {
        this.commitFilter = commitFilter;
    }

    public void setFailOnlyOnConfirmedTrue(boolean fail) {
        if (fail) {
            this.commitFilter = (changes, clearRequested, removeRequested) ->
                    changes.containsKey("confirmed") && Boolean.TRUE.equals(changes.get("confirmed"));
        } else {
            this.commitFilter = null;
        }
    }

    public void setFailOnClear(boolean fail) {
        if (fail) {
            this.commitFilter = (changes, clearRequested, removeRequested) ->
                    clearRequested || removeRequested;
        } else {
            this.commitFilter = null;
        }
    }

    private class MockEditor implements Editor {
        private final Map<String, Object> temp = new HashMap<>();
        private boolean clearRequested = false;

        @Override
        public Editor putString(String key, String value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            temp.put(key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            temp.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            temp.put(key, this); // sentinel for removal
            return this;
        }

        @Override
        public Editor clear() {
            clearRequested = true;
            return this;
        }

        @Override
        public boolean commit() {
            boolean removeRequested = temp.containsValue(this);
            if (failCommits || (commitFilter != null && commitFilter.shouldFail(temp, clearRequested, removeRequested))) {
                return false;
            }
            if (clearRequested) {
                storage.clear();
            }
            for (Map.Entry<String, Object> entry : temp.entrySet()) {
                if (entry.getValue() == this) {
                    storage.remove(entry.getKey());
                } else {
                    storage.put(entry.getKey(), entry.getValue());
                }
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
