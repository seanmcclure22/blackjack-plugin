package com.blackjack;

import lombok.SneakyThrows;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

import java.text.SimpleDateFormat;

import javax.inject.Inject;
import java.awt.*;

public class BlackjackOverlay extends Overlay {
    private final Client client;
    private final BlackjackConfig config;
    private final BlackjackPlugin plugin;

    @Inject
    BlackjackOverlay(Client client, BlackjackConfig config, BlackjackPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        for (NPC npc : plugin.getHighlightedNpcs())
        {
            Color color = plugin.getHighlightColor();
            renderNpcOverlay(graphics, npc, color);
        }
        return null;
    }

    private void renderNpcOverlay(Graphics2D graphics, NPC actor, Color color)
    {
        NPCComposition npcComposition = actor.getTransformedComposition();
        if (npcComposition == null || !npcComposition.isInteractible())
        {
            return;
        }

        switch (config.renderStyle())
        {
            case TILE:
                int size = npcComposition.getSize();
                LocalPoint lp = actor.getLocalLocation();
                Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, lp, size);

                renderPoly(graphics, color, tilePoly);
                break;

            case HULL:
                Shape objectClickbox = actor.getConvexHull();
                //client.addChatMessage(ChatMessageType.CONSOLE, "DEBUG", color.toString(), "");
                renderPoly(graphics, color, objectClickbox);
                break;
        }

        if (config.statusText() && actor.getName() != null)
        {
            String npcName = Text.removeTags(actor.getName());
            Point textLocation = actor.getCanvasTextLocation(graphics, npcName, actor.getLogicalHeight() + 40);

            if (textLocation != null)
            {
                //client.addChatMessage(ChatMessageType.CONSOLE, "DEBUG", plugin.statusText(), "");
                OverlayUtil.renderTextLocation(graphics, textLocation, plugin.statusText(), color);
            }
        }
    }

    private void renderPoly(Graphics2D graphics, Color color, Shape polygon)
    {
        if (polygon != null)
        {
            graphics.setColor(color);
            graphics.setStroke(new BasicStroke(2));
            graphics.draw(polygon);
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
            graphics.fill(polygon);
        }
    }
}
