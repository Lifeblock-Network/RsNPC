package com.smallaswater.npc.utils;

import org.powernukkitx.entity.data.human.Skin;
import org.powernukkitx.utils.SerializedImage;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Skin builder.
 * <p>
 * In PNX 3.0.0, {@code org.powernukkitx.entity.data.human.Skin} became an immutable wrapper
 * around {@code org.cloudburstmc.protocol.bedrock.data.skin.Skin} and no longer
 * exposes the old mutable setter API. This class reproduces the parts of the legacy
 * {@code org.powernukkitx.entity.data.Skin} build logic that this plugin relies on, and finally
 * produces the new Skin via {@link #build()}.
 */
public class SkinBuilder {

    private static final int PIXEL_SIZE = 4;
    public static final int SINGLE_SKIN_SIZE = 32 * 32 * PIXEL_SIZE;

    public static final String GEOMETRY_CUSTOM = convertLegacyGeometryName("geometry.humanoid.custom");
    public static final String GEOMETRY_CUSTOM_SLIM = convertLegacyGeometryName("geometry.humanoid.custom.slim");

    /**
     * The built-in humanoid geometry shipped with the server (defines {@code geometry.humanoid.custom}
     * in a format the current client accepts). The legacy {@code org.powernukkitx.entity.data.Skin} used this
     * as the default geometry data; a skin whose resource patch references {@code geometry.humanoid.custom}
     * but supplies no geometry data of its own renders as the vanilla default on modern clients.
     */
    static final String GEOMETRY_HUMANOID;

    static {
        String geoData;
        try (InputStream stream = SkinBuilder.class.getClassLoader().getResourceAsStream("gamedata/skin_geometry.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            geoData = reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (Exception e) {
            geoData = "";
        }
        GEOMETRY_HUMANOID = geoData;
    }

    public static String getGeometryHumanoid() {
        if (GEOMETRY_HUMANOID != null && !GEOMETRY_HUMANOID.isEmpty()) {
            return GEOMETRY_HUMANOID;
        }
        try (InputStream stream = SkinBuilder.class.getClassLoader().getResourceAsStream("gamedata/skin_geometry.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n", "", "\n"));
        } catch (Exception e) {
            return "";
        }
    }

    private String skinId;
    private String skinResourcePatch = GEOMETRY_CUSTOM;
    @Getter
    private SerializedImage skinData;
    private String geometryName = "geometry.humanoid.custom";
    private String geometryData = "";
    @Setter
    private String geometryDataEngineVersion = "0.0.0";
    @Setter
    private boolean trusted = true;
    @Setter
    private boolean primaryUser = false;
    @Setter
    private boolean overridingPlayerAppearance = false;

    public static String convertLegacyGeometryName(String geometryName) {
        return "{\"geometry\" : {\"default\" : \"" + geometryName + "\"}}";
    }

    private static SerializedImage parseBufferedImage(BufferedImage image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);
                outputStream.write(color.getRed());
                outputStream.write(color.getGreen());
                outputStream.write(color.getBlue());
                outputStream.write(color.getAlpha());
            }
        }
        image.flush();
        return new SerializedImage(image.getWidth(), image.getHeight(), outputStream.toByteArray());
    }

    public void setSkinId(String skinId) {
        if (skinId == null || skinId.trim().isEmpty() || skinId.length() >= 100) {
            generateSkinId("Custom");
            return;
        }
        this.skinId = skinId;
    }

    public void generateSkinId(String name) {
        byte[] image = getSkinData().data;
        byte[] patch = getSkinResourcePatch().getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[image.length + patch.length];
        System.arraycopy(image, 0, data, 0, image.length);
        System.arraycopy(patch, 0, data, image.length, patch.length);
        this.skinId = UUID.nameUUIDFromBytes(data) + "." + name;
    }

    public void setGeometryName(String geometryName) {
        if (geometryName == null || geometryName.trim().isEmpty()) {
            this.geometryName = "geometry.humanoid";
            this.skinResourcePatch = GEOMETRY_CUSTOM;
            return;
        }
        this.geometryName = geometryName;
        this.skinResourcePatch = convertLegacyGeometryName(geometryName);
    }

    public String getSkinResourcePatch() {
        return this.skinResourcePatch == null ? "" : this.skinResourcePatch;
    }

    public void setSkinResourcePatch(String skinResourcePatch) {
        if (skinResourcePatch == null || skinResourcePatch.trim().isEmpty()) {
            this.skinResourcePatch = GEOMETRY_CUSTOM;
            return;
        }
        this.skinResourcePatch = skinResourcePatch;
    }

    public void setGeometryData(String geometryData) {
        if (geometryData != null) {
            this.geometryData = geometryData;
        }
    }

    public void setSkinData(byte[] skinData) {
        if (skinData == null || skinData.length < SINGLE_SKIN_SIZE) {
            return;
        }

        int width;
        int height;
        if (skinData.length == SINGLE_SKIN_SIZE) {
            width = 64;
            height = 32;
        } else if (skinData.length == SINGLE_SKIN_SIZE * 2 || skinData.length == SINGLE_SKIN_SIZE * 4) {
            width = 64;
            height = 64;
        } else if (skinData.length == SINGLE_SKIN_SIZE * 16) {
            width = 128;
            height = 128;
        } else {
            return;
        }

        this.skinData = new SerializedImage(width, height, skinData);
    }

    public void setSkinData(BufferedImage image) {
        this.skinData = parseBufferedImage(image);
    }

    public String getSkinId() {
        if (this.skinId == null || this.skinId.trim().isEmpty() || this.skinId.length() >= 100) {
            generateSkinId("Custom");
        }
        return this.skinId;
    }

    public boolean isValid() {
        return getSkinId() != null && !getSkinId().trim().isEmpty() && getSkinId().length() < 100 &&
                this.skinData != null && this.skinData.width >= 32 && this.skinData.height >= 32 &&
                this.skinData.data.length >= SINGLE_SKIN_SIZE;
    }

    public Skin build() {
        ImageData image = this.skinData == null ? ImageData.EMPTY :
                ImageData.of(this.skinData.width, this.skinData.height, this.skinData.data);

        String effectiveGeometryName = (this.geometryName == null || this.geometryName.isEmpty())
                ? "geometry.humanoid" : this.geometryName;

        String effectiveGeometryData;
        if (this.geometryData != null && !this.geometryData.isEmpty()) {
            effectiveGeometryData = this.geometryData;
        } else if ("geometry.humanoid.custom".equals(effectiveGeometryName)) {
            effectiveGeometryData = getGeometryHumanoid();
        } else {
            effectiveGeometryData = "";
        }

        String effectiveEngineVersion = (effectiveGeometryData == null || effectiveGeometryData.isEmpty())
                ? "" : ((this.geometryDataEngineVersion == null || "0.0.0".equals(this.geometryDataEngineVersion))
                ? "1.12.0" : this.geometryDataEngineVersion);

        org.cloudburstmc.protocol.bedrock.data.skin.Skin cloudburstSkin =
                org.cloudburstmc.protocol.bedrock.data.skin.Skin.builder()
                .skinId(getSkinId())
                .skinResourcePatch(getSkinResourcePatch())
                .skinData(image)
                .capeData(ImageData.EMPTY)
                .geometryName(effectiveGeometryName)
                .geometryData(effectiveGeometryData)
                .geometryDataEngineVersion(effectiveEngineVersion)
                .premium(false)
                .persona(false)
                .primaryUser(this.primaryUser)
                .overridingPlayerAppearance(this.overridingPlayerAppearance)
                .build();
        return new Skin(cloudburstSkin, this.trusted);
    }

}
