package net.minecraft.client.resources.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.util.*;

public class IMetadataSerializer {
    private final IRegistry<String, IMetadataSerializer.Registration<? extends IMetadataSection>> metadataSectionSerializerRegistry = new RegistrySimple();
    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private Gson gson;

    public IMetadataSerializer() {
        this.gsonBuilder.registerTypeHierarchyAdapter(IChatComponent.class, new IChatComponent.Serializer());
        this.gsonBuilder.registerTypeHierarchyAdapter(ChatStyle.class, new ChatStyle.Serializer());
        this.gsonBuilder.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
    }

    public <T extends IMetadataSection> void registerMetadataSectionType(IMetadataSectionSerializer<T> metadataSectionSerializer, Class<T> clazz) {
        this.metadataSectionSerializerRegistry.putObject(metadataSectionSerializer.getSectionName(), new Registration<>(metadataSectionSerializer, clazz));
        this.gsonBuilder.registerTypeAdapter(clazz, metadataSectionSerializer);
        this.gson = null;
    }

    public <T extends IMetadataSection> T parseMetadataSection(String sectionName, JsonObject json) {
        if (sectionName == null) {
            throw new IllegalArgumentException("Metadata section name cannot be null");
        } else if (!json.has(sectionName)) {
            return null;
        } else if (!json.get(sectionName).isJsonObject()) {
            throw new IllegalArgumentException("Invalid metadata for '" + sectionName + "' - expected object, found " + json.get(sectionName));
        } else {
            IMetadataSerializer.Registration<?> registration = this.metadataSectionSerializerRegistry.getObject(sectionName);

            if (registration == null) {
                throw new IllegalArgumentException("Don't know how to handle metadata section '" + sectionName + "'");
            } else {
                return (T) this.getGson().fromJson(json.getAsJsonObject(sectionName), registration.clazz);
            }
        }
    }

    private Gson getGson() {
        if (this.gson == null) {
            this.gson = this.gsonBuilder.create();
        }

        return this.gson;
    }

    static class Registration<T extends IMetadataSection> {
        final IMetadataSectionSerializer<T> section;
        final Class<T> clazz;

        private Registration(IMetadataSectionSerializer<T> metadataSectionSerializer, Class<T> clazzToRegister) {
            this.section = metadataSectionSerializer;
            this.clazz = clazzToRegister;
        }
    }
}
