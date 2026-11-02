package dev.dnasolver.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dnasolver.DnaConfig;
import dev.dnasolver.DnaSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    private final DnaSession dnaSolver$session = new DnaSession();

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    /** Same injection point SkyHanni uses for its "background drawn" event. */
    @Inject(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
        )
    )
    private void dnaSolver$drawHighlights(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        dnaSolver$session.refresh(self);
        dnaSolver$session.renderHighlights(context, self);
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void dnaSolver$hideTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (DnaConfig.get().hideTooltips && dnaSolver$session.isActive(self) && DnaSession.isBoardSlot(this.hoveredSlot)) {
            ci.cancel();
        }
    }

    @WrapMethod(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V")
    private void dnaSolver$slotClicked(Slot slot, int slotId, int button, ContainerInput input, Operation<Void> original) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (dnaSolver$session.isActive(self)) {
            DnaConfig cfg = DnaConfig.get();
            if (slotId == DnaSession.CLOSE_BUTTON_SLOT && cfg.blockAccidentalClosing) {
                return;
            }
            if (cfg.useMiddleClick && slot != null && input != ContainerInput.CLONE) {
                original.call(slot, slotId, 2, ContainerInput.CLONE);
                return;
            }
        }
        original.call(slot, slotId, button, input);
    }
}
