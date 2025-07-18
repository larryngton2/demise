package net.minecraft.server.management;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UserList<K, V extends UserListEntry<K>> {
    protected static final Logger logger = LogManager.getLogger();
    protected final Gson gson;
    private final File saveFile;
    private final Map<String, V> values = Maps.newHashMap();
    private boolean lanServer = true;
    private static final ParameterizedType saveFileFormat = new ParameterizedType() {
        public Type @NotNull [] getActualTypeArguments() {
            return new Type[]{UserListEntry.class};
        }

        public @NotNull Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    public UserList(File saveFile) {
        this.saveFile = saveFile;
        GsonBuilder gsonbuilder = (new GsonBuilder()).setPrettyPrinting();
        gsonbuilder.registerTypeHierarchyAdapter(UserListEntry.class, new UserList.Serializer());
        this.gson = gsonbuilder.create();
    }

    public boolean isLanServer() {
        return this.lanServer;
    }

    public void setLanServer(boolean state) {
        this.lanServer = state;
    }

    public void addEntry(V entry) {
        this.values.put(this.getObjectKey(entry.getValue()), entry);

        try {
            this.writeChanges();
        } catch (IOException ioexception) {
            logger.warn("Could not save the list after adding a user.", ioexception);
        }
    }

    public V getEntry(K obj) {
        this.removeExpired();
        return this.values.get(this.getObjectKey(obj));
    }

    public void removeEntry(K entry) {
        this.values.remove(this.getObjectKey(entry));

        try {
            this.writeChanges();
        } catch (IOException ioexception) {
            logger.warn("Could not save the list after removing a user.", ioexception);
        }
    }

    public String[] getKeys() {
        return this.values.keySet().toArray(new String[0]);
    }

    protected String getObjectKey(K obj) {
        return obj.toString();
    }

    protected boolean hasEntry(K entry) {
        return this.values.containsKey(this.getObjectKey(entry));
    }

    private void removeExpired() {
        List<K> list = Lists.newArrayList();

        for (V v : this.values.values()) {
            if (v.hasBanExpired()) {
                list.add(v.getValue());
            }
        }

        for (K k : list) {
            this.values.remove(k);
        }
    }

    protected UserListEntry createEntry(JsonObject entryData) {
        return new UserListEntry<>(null, entryData);
    }

    protected Map<String, V> getValues() {
        return this.values;
    }

    public void writeChanges() throws IOException {
        Collection<V> collection = this.values.values();
        String s = this.gson.toJson(collection);
        BufferedWriter bufferedwriter = null;

        try {
            bufferedwriter = Files.newWriter(this.saveFile, Charsets.UTF_8);
            bufferedwriter.write(s);
        } finally {
            IOUtils.closeQuietly(bufferedwriter);
        }
    }

    class Serializer implements JsonDeserializer<UserListEntry<K>>, JsonSerializer<UserListEntry<K>> {
        private Serializer() {
        }

        public JsonElement serialize(UserListEntry<K> p_serialize_1_, Type p_serialize_2_, JsonSerializationContext p_serialize_3_) {
            JsonObject jsonobject = new JsonObject();
            p_serialize_1_.onSerialization(jsonobject);
            return jsonobject;
        }

        public UserListEntry<K> deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
            if (p_deserialize_1_.isJsonObject()) {
                JsonObject jsonobject = p_deserialize_1_.getAsJsonObject();
                return (UserListEntry<K>) UserList.this.createEntry(jsonobject);
            } else {
                return null;
            }
        }
    }
}
