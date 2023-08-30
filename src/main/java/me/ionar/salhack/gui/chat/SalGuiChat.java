package me.ionar.salhack.gui.chat;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;

public class SalGuiChat extends GuiChat {
    public SalGuiChat(GuiChat oldChat) {
        defaultInputFieldText = oldChat.inputField.getText();
        setWorldAndResolution(oldChat.mc, oldChat.width, oldChat.height);
    }

    public SalGuiChat(String defaultText) {
        defaultInputFieldText = defaultText;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.sentHistoryCursor = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new SalGuiTextField(0, this.fontRenderer, 4, this.height - 12, this.width - 4, 12);
        this.inputField.setMaxStringLength(256);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setFocused(true);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setCanLoseFocus(false);
        this.tabCompleter = new GuiChat.ChatTabCompleter(this.inputField);
    }
}
