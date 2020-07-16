/*
 *     Copyright (c) 2018 Isa Hekmatizadeh.
 *     This file is part of mafiagame.
 *
 *     Mafiagame is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Mafiagame is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Mafiagame.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.mafiagame.engine;

import co.mafiagame.engine.exception.PlayerNotFoundException;
import co.mafiagame.engine.exception.RegisterAfterStartException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Esa Hekmatizadeh
 */
class GameState {
	private boolean gameStarted = false;
	private int citizenNo;
	private int mafiaNo;
	private boolean hasDetective;
	private boolean hasDoctor;
	private Map<String, Player> alivePlayersMap = new HashMap<>();

	GameState(int citizenNo, int mafiaNo,
						boolean hasDetective, boolean hasDoctor) {
		this.citizenNo = citizenNo;
		this.mafiaNo = mafiaNo;
		this.hasDetective = hasDetective;
		this.hasDoctor = hasDoctor;
	}

	void registerPlayer(Player player) {
		if (gameStarted)
			throw new RegisterAfterStartException();
		alivePlayersMap.put(player.getUserId(), player);
		if (totalPlayer() == alivePlayersMap.size())
			gameStarted = true;
	}

	void killPlayer(String playerUserId) {
		if (Player.NOBODY_USERID.equalsIgnoreCase(playerUserId))
			return;
		Player removed = alivePlayersMap.remove(playerUserId);
		switch (removed.getRole()) {
			case CITIZEN:
				citizenNo--;
				break;
			case MAFIA:
				mafiaNo--;
				break;
			case DETECTIVE:
				hasDetective = false;
				break;
			case DOCTOR:
				hasDoctor = false;
				break;
			default: //TODO: should be think about
				throw new IllegalStateException("player's role with id '" + removed.getUserId() + "' is invalid");
		}
	}

	long totalPlayer() {
		return citizenNo + mafiaNo + (hasDetective ? 1 : 0) + (hasDoctor ? 1 : 0);
	}

	List<Player> alivePlayers() {
		return new ArrayList<>(alivePlayersMap.values());
	}

	List<Player> mafiaPlayers() {
		return new ArrayList<>(alivePlayersMap.values().stream().filter(p -> p.getRole() == Role.MAFIA).collect(Collectors.toList()));
	}

	Optional<Player> doctor() {
		return alivePlayersMap.values().stream().filter(p -> p.getRole() == Role.DOCTOR).findAny();
	}

	Optional<Player> detective() {
		return alivePlayersMap.values().stream().filter(p -> p.getRole() == Role.DETECTIVE).findAny();
	}

	Player player(String userId) {
		return alivePlayersMap.get(userId);
	}

	void checkPlayerExist(String userId) throws PlayerNotFoundException {
		if (!alivePlayersMap.containsKey(userId) && !Player.NOBODY.getUserId().equalsIgnoreCase(userId))
			throw new PlayerNotFoundException(userId);
	}

	long getCitizenNo() {
		return citizenNo;
	}

	long getMafiaNo() {
		return mafiaNo;
	}

	boolean hasDetective() {
		return hasDetective;
	}

	boolean hasDoctor() {
		return hasDoctor;
	}

	public boolean gameStarted() {
		return gameStarted;
	}

	@Override
	public String toString() {
		return "(totalPlayer:" + totalPlayer() +
				", citizenNo=" + citizenNo +
				", mafiaNo=" + mafiaNo +
				", hasDetective=" + hasDetective +
				", hasDoctor=" + hasDoctor + ')';
	}
}
