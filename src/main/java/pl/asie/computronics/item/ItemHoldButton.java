package pl.asie.computronics.item;

import pl.asie.computronics.panel.PanelComponentType;

/** Panel button that stays pressed for as long as the player holds right-click on it. */
public class ItemHoldButton extends ItemPanelComponent {
    public ItemHoldButton(Properties properties) {
        super(properties, PanelComponentType.HOLD_BUTTON);
    }
}
