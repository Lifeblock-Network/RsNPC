package com.smallaswater.npc.utils;

import cn.nukkit.entity.data.human.Skin;
import cn.nukkit.utils.SerializedImage;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 皮肤构建器。
 * <p>
 * b-migration 中 {@code cn.nukkit.entity.data.human.Skin} 变成了对
 * {@code org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin} 的不可变包装，
 * 不再提供旧版可变的 setter API。此类复刻旧版 {@code cn.nukkit.entity.data.Skin} 中
 * 本插件用到的部分构建逻辑，最终通过 {@link #build()} 产出新的 Skin。
 *
 * @author LT_Name (migration helper)
 */
public class SkinBuilder {

    private static final int PIXEL_SIZE = 4;
    public static final int SINGLE_SKIN_SIZE = 32 * 32 * PIXEL_SIZE;

    public static final String GEOMETRY_CUSTOM = convertLegacyGeometryName("geometry.humanoid.custom");
    public static final String GEOMETRY_CUSTOM_SLIM = convertLegacyGeometryName("geometry.humanoid.customSlim");

    private String skinId;
    private String skinResourcePatch = GEOMETRY_CUSTOM;
    private SerializedImage skinData;
    private String geometryName;
    private String geometryData = "";
    private String geometryDataEngineVersion = "0.0.0";
    private boolean trusted = true;

    private static String convertLegacyGeometryName(String geometryName) {
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
        if (skinId == null || skinId.trim().isEmpty()) {
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

    public void setGeometryDataEngineVersion(String geometryDataEngineVersion) {
        this.geometryDataEngineVersion = geometryDataEngineVersion;
    }

    public void setSkinData(byte[] skinData) {
        this.skinData = SerializedImage.fromLegacy(skinData);
    }

    public void setSkinData(BufferedImage image) {
        this.skinData = parseBufferedImage(image);
    }

    public SerializedImage getSkinData() {
        return this.skinData;
    }

    public String getSkinId() {
        if (this.skinId == null) {
            generateSkinId("Custom");
        }
        return this.skinId;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public boolean isValid() {
        return getSkinId() != null && !getSkinId().trim().isEmpty() && getSkinId().length() < 100 &&
                this.skinData != null && this.skinData.width >= 32 && this.skinData.height >= 32 &&
                this.skinData.data.length >= SINGLE_SKIN_SIZE;
    }

    public Skin build() {
        ImageData image = this.skinData == null ? ImageData.EMPTY :
                ImageData.of(this.skinData.width, this.skinData.height, this.skinData.data);
        SerializedSkin serializedSkin = SerializedSkin.builder()
                .skinId(getSkinId())
                .skinResourcePatch(getSkinResourcePatch())
                .skinData(image)
                .capeData(ImageData.EMPTY)
                .geometryName(this.geometryName)
                .geometryData(this.geometryData)
                .geometryDataEngineVersion(this.geometryDataEngineVersion)
                .premium(false)
                .persona(false)
                .build();
        return new Skin(serializedSkin, this.trusted);
    }

}
