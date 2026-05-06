package xy177.tinkersplannerantique.client.planner;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.translation.I18n;
import xy177.tinkersplannerantique.PlannerConfig;

final class PlannerCodeImportScreen extends GuiScreen {

    private final PlannerScreen parent;
    private GuiTextField textField;
    private String message = "";

    PlannerCodeImportScreen(PlannerScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        textField = new GuiTextField(0, fontRenderer, width / 2 - 140, height / 2 - 10, 280, 20);
        textField.setMaxStringLength(2048);
        textField.setFocused(true);
        textField.setText(GuiScreen.getClipboardString());
        addButton(new GuiButton(1, width / 2 - 140, height / 2 + 20, 136, 20, I18n.translateToLocal("gui.tpa.import")));
        addButton(new GuiButton(2, width / 2 + 4, height / 2 + 20, 136, 20, I18n.translateToLocal("gui.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            tryImport();
        } else if (button.id == 2) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == 28 || keyCode == 156) {
            tryImport();
            return;
        }
        textField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        textField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.translateToLocal("gui.tpa.import_code"), width / 2, height / 2 - 34, 0xFFFFFF);
        textField.drawTextBox();
        if (!message.isEmpty()) {
            drawCenteredString(fontRenderer, message, width / 2, height / 2 + 48, 0xFF9090);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void tryImport() {
        try {
            PlannerShortCodeList list = PlannerConfig.enableShortBlueprintCode ? PlannerShortCodeList.loadOrCreate(PlannerClientEvents.getDataFolder(), PlannerClientEvents.getAllTargets()) : null;
            PlannerBlueprint blueprint = PlannerBlueprintCodecs.importCode(textField.getText(), list, PlannerClientEvents.getAllTargets());
            mc.displayGuiScreen(PlannerScreen.forImportedBlueprint(parent.getParentScreen(), blueprint));
        } catch (Exception e) {
            String raw = e.getMessage();
            if (raw != null && raw.startsWith("gui.tpa.")) {
                message = I18n.translateToLocal(raw);
            } else {
                message = raw == null ? I18n.translateToLocal("gui.tpa.import_failed") : raw;
            }
        }
    }
}
