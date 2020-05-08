package com.blackjack;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.WildcardMatcher;
import javax.inject.Inject;
import java.awt.Color;
import java.util.*;

@PluginDescriptor(
		name = "Blackjack",
		description = "Help show whether a blackjack target is awaken, knocked out or aggressive.",
		tags = {"blackjack", "thieve", "thieving"}
)
public class BlackjackPlugin extends Plugin {
	private static final String SUCCESS_BLACKJACK = "You smack the bandit over the head and render them unconscious.";
	private static final String FAILED_BLACKJACK = "Your blow only glances off the bandit's head.";
	private static final String SUCCESS_PICKPOCKET = "You pick the Menaphite's pocket.";
	private static final String FAILED_PICKPOCKET = "You fail to pick the Menaphite's pocket.";

	@Inject
	private BlackjackConfig blackjackConfig;

	@Inject
	private BlackjackOverlay blackjackOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	/**
	 * NPCs to highlight
	 */
	@Getter(AccessLevel.PACKAGE)
	private final Set<NPC> highlightedNpcs = new HashSet<>();

	/**
	 * Stores state of if blackjack NPC: Awaken, Knocked Out or Aggressive.
	 */
	@Getter(AccessLevel.PACKAGE)
	private BlackjackNPCState npcState = BlackjackNPCState.AWAKEN;

	private String highlight = "";
	private long nextKnockOutTick = 0;

	@Provides
	BlackjackConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BlackjackConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(blackjackOverlay);
		highlight = npcToHighlight();
		clientThread.invoke(() ->
		{
			rebuildAllNpcs();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(blackjackOverlay);
		highlightedNpcs.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN ||
				event.getGameState() == GameState.HOPPING)
		{
			highlightedNpcs.clear();
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event) {

		if (!event.getGroup().equals("blackjack")) {
			return;
		}

		highlight = npcToHighlight();
		rebuildAllNpcs();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		final NPC npc = npcSpawned.getNpc();
		final String npcName = npc.getName();

		if (npcName == null)
		{
			return;
		}

		if (WildcardMatcher.matches(highlight, npcName))
		{
			highlightedNpcs.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		final NPC npc = npcDespawned.getNpc();

		highlightedNpcs.remove(npc);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getTickCount() >= nextKnockOutTick && npcState != BlackjackNPCState.AGGRESSIVE)
		{
			npcState = BlackjackNPCState.AWAKEN;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String msg = event.getMessage();

		if (event.getType() == ChatMessageType.SPAM)
		{
			switch (msg) {
				case SUCCESS_BLACKJACK:
					npcState = BlackjackNPCState.KNOCKED_OUT;
					nextKnockOutTick = client.getTickCount() + 4;
					break;
				case FAILED_BLACKJACK:
					npcState = BlackjackNPCState.AGGRESSIVE;
					break;
				case SUCCESS_PICKPOCKET:
				case FAILED_PICKPOCKET:
					npcState = BlackjackNPCState.AWAKEN;
				default:
					break;
			}
		}
	}

	public Color getHighlightColor() throws IllegalStateException {
		switch (npcState)
		{
			case KNOCKED_OUT:
				return blackjackConfig.knockedOutStateColor();
			case AWAKEN:
				return blackjackConfig.awakeStateColor();
			case AGGRESSIVE:
				return blackjackConfig.aggressiveStateColor();
			default:
				throw new IllegalStateException("Unexpected value: " + blackjackConfig.npcToBlackjack());
		}
	}

	private String npcToHighlight()
	{
		switch (blackjackConfig.npcToBlackjack()) {
			case BANDIT:
				return "Bandit";
			case MENAPHITE_THUG:
				return "Menaphite Thug";
			default:
				throw new IllegalStateException("Unexpected value: " + blackjackConfig.npcToBlackjack());
		}
	}

	private void rebuildAllNpcs()
	{
		highlightedNpcs.clear();

		if (client.getGameState() != GameState.LOGGED_IN &&
				client.getGameState() != GameState.LOADING)
		{
			// NPCs are still in the client after logging out,
			// but we don't want to highlight those.
			return;
		}

		for (NPC npc : client.getNpcs())
		{
			final String npcName = npc.getName();

			if (npcName != null && WildcardMatcher.matches(highlight, npcName))
			{
				highlightedNpcs.add(npc);
			}
		}
	}

	public String statusText()
	{
		return npcState.getDisplayName();
	}
}
