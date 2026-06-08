package com.smallaswater.npc.form.response;

import org.cloudburstmc.protocol.bedrock.packet.NpcRequestPacket;
import com.smallaswater.npc.form.element.ResponseElementDialogButton;
import com.smallaswater.npc.form.windows.AdvancedFormWindowDialog;
import lombok.Getter;

@Getter
public class FormResponseDialog {

    private final long entityRuntimeId;
    private final String data;
    private ResponseElementDialogButton clickedButton;//can be null
    private final String sceneName;
    private final NpcRequestPacket.RequestType requestType;
    private final int actionType;

    public FormResponseDialog(NpcRequestPacket packet, AdvancedFormWindowDialog dialog) {
        this.entityRuntimeId = packet.getNpcRuntimeID();
        this.data = packet.getActions();
        try {
            this.clickedButton = dialog.getButtons().get(packet.getActionIndex());
        } catch (IndexOutOfBoundsException e) {
            this.clickedButton = null;
        }
        this.sceneName = packet.getSceneName();
        this.requestType = packet.getRequestType();
        this.actionType = packet.getActionIndex();
    }
}
